package com.ucm.analysis.service;

import com.ucm.analysis.entity.ViolationCase;
import com.ucm.analysis.repository.ViolationCaseRepository;
import com.ucm.common.BizException;
import com.ucm.entity.ChangeSpot;
import com.ucm.entity.SysUser;
import com.ucm.entity.WorkOrder;
import com.ucm.repository.SysUserRepository;
import com.ucm.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 队员能力匹配：基于历史案例统计队员画像（类型经验 / 效率 / 准时率），
 * 结合工单复杂度与当前负载打分，智能推荐处置队员。
 */
@Service
public class DispatchService {

    /** 复杂工单对队员历史处理量的最低要求（低于则判定新手，降权） */
    private static final int EXPERIENCED_MIN_CASES = 3;

    private final SysUserRepository userRepo;
    private final WorkOrderRepository orderRepo;
    private final ViolationCaseRepository caseRepo;
    private final HeatmapService heatmapService;

    public DispatchService(SysUserRepository userRepo, WorkOrderRepository orderRepo,
                           ViolationCaseRepository caseRepo, HeatmapService heatmapService) {
        this.userRepo = userRepo;
        this.orderRepo = orderRepo;
        this.caseRepo = caseRepo;
        this.heatmapService = heatmapService;
    }

    /** 全部队员的能力画像 */
    public List<Map<String, Object>> memberProfiles() {
        List<ViolationCase> all = caseRepo.findAll();
        double globalAvgDays = globalAvgDays(all);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysUser u : userRepo.findByRole(SysUser.Role.OPERATOR)) {
            result.add(profile(u, all, globalAvgDays));
        }
        return result;
    }

    /** 针对某工单的智能分派建议（按匹配得分降序，首位为推荐队员） */
    public List<Map<String, Object>> suggest(Long orderId) {
        WorkOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new BizException("工单不存在: " + orderId));
        ChangeSpot spot = order.getSpot();
        ViolationCase.ViolationType type = DurationPredictService.mapChangeType(spot.getChangeType());
        double area = spot.getAreaM2() == null ? 0 : spot.getAreaM2();

        // 工单复杂度评分：面积(0~50) + 类型(5~15) + 热点(0~10)
        double areaScore = Math.min(50, area / 500.0 * 50);
        int typeScore = switch (type) {
            case ROOFTOP_ADDITION, OCCUPY_LAND -> 15;
            case ILLEGAL_EXPANSION -> 10;
            case TEMP_STRUCTURE -> 5;
        };
        boolean hotspot = false;
        if (spot.getGeom() != null) {
            var c = spot.getGeom().getCentroid();
            hotspot = heatmapService.openOrderCount(GridUtil.gridCode(c.getX(), c.getY()))
                    >= RecommendService.HOTSPOT_THRESHOLD;
        }
        double complexity = areaScore + typeScore + (hotspot ? 10 : 0);
        String complexityLabel = complexity >= 50 ? "复杂" : complexity >= 30 ? "中等" : "简单";

        List<ViolationCase> all = caseRepo.findAll();
        double globalAvgDays = globalAvgDays(all);

        List<Map<String, Object>> ranked = new ArrayList<>();
        for (SysUser u : userRepo.findByRole(SysUser.Role.OPERATOR)) {
            Map<String, Object> profile = profile(u, all, globalAvgDays);
            int handled = (int) profile.get("handledCount");
            @SuppressWarnings("unchecked")
            Map<String, Integer> byType = (Map<String, Integer>) profile.get("byType");
            int typeHandled = byType.getOrDefault(type.name(), 0);
            double efficiency = (double) profile.get("efficiency");
            double onTimeRate = (double) profile.get("onTimeRate");
            long load = (long) profile.get("currentLoad");

            // 匹配得分：类型经验 40% + 效率 30% + 准时率 20% - 负载惩罚 10%
            double typeExp = handled == 0 ? 0 : (double) typeHandled / handled;
            double score = 40 * typeExp
                    + 30 * Math.min(efficiency, 1.5) / 1.5
                    + 20 * onTimeRate
                    - 10 * Math.min(load, 5) / 5.0;

            StringBuilder reason = new StringBuilder();
            if (typeHandled > 0) reason.append("处理过 ").append(typeHandled).append(" 起同类违建；");
            if (handled == 0) reason.append("暂无历史处置记录；");

            // 复杂工单新手降权，简单工单向新手倾斜（以老带新）
            if (complexity >= 50 && handled < EXPERIENCED_MIN_CASES) {
                score *= 0.5;
                reason.append("复杂工单，新手降权；");
            } else if (complexity < 30 && handled < EXPERIENCED_MIN_CASES) {
                score += 5;
                reason.append("简单工单，适合新手锻炼；");
            }
            if (load >= 5) reason.append("当前负载较高；");

            profile.put("typeHandledCount", typeHandled);
            profile.put("score", Math.round(score * 10) / 10.0);
            profile.put("suggestReason", reason.toString());
            profile.put("complexity", Math.round(complexity * 10) / 10.0);
            profile.put("complexityLabel", complexityLabel);
            ranked.add(profile);
        }
        ranked.sort(Comparator.comparingDouble(m -> -(double) m.get("score")));
        if (!ranked.isEmpty()) ranked.get(0).put("recommended", true);
        return ranked;
    }

    /** 单个队员画像 */
    private Map<String, Object> profile(SysUser u, List<ViolationCase> all, double globalAvgDays) {
        List<ViolationCase> mine = all.stream()
                .filter(c -> u.getId().equals(c.getOperatorId())).toList();
        int handled = mine.size();
        double avgDays = mine.stream().filter(c -> c.getDurationDays() != null)
                .mapToDouble(ViolationCase::getDurationDays).average().orElse(globalAvgDays);
        // 效率系数 = 全局均值 / 个人均值（>1 表示快于平均）
        double efficiency = avgDays > 0 ? globalAvgDays / avgDays : 1.0;
        // 准时率：实际处理天数不超过同类型案例均值的占比
        long onTime = mine.stream().filter(c -> {
            if (c.getDurationDays() == null) return true;
            double typeAvg = all.stream()
                    .filter(o -> o.getViolationType() == c.getViolationType() && o.getDurationDays() != null)
                    .mapToDouble(ViolationCase::getDurationDays).average().orElse(globalAvgDays);
            return c.getDurationDays() <= typeAvg;
        }).count();
        double onTimeRate = handled == 0 ? 1.0 : (double) onTime / handled;
        long load = orderRepo.countByAssigneeIdAndStatusIn(u.getId(), HeatmapService.OPEN_STATUSES);

        // 各违建类型处理量
        Map<String, Integer> byType = new LinkedHashMap<>();
        for (ViolationCase.ViolationType t : ViolationCase.ViolationType.values()) {
            byType.put(t.name(), (int) mine.stream().filter(c -> c.getViolationType() == t).count());
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", u.getId());
        m.put("username", u.getUsername());
        m.put("realName", u.getRealName());
        m.put("handledCount", handled);
        m.put("avgDurationDays", Math.round(avgDays * 10) / 10.0);
        m.put("efficiency", Math.round(efficiency * 100) / 100.0);
        m.put("onTimeRate", Math.round(onTimeRate * 100) / 100.0);
        m.put("currentLoad", load);
        m.put("byType", byType);
        return m;
    }

    private static double globalAvgDays(List<ViolationCase> all) {
        return all.stream().filter(c -> c.getDurationDays() != null)
                .mapToDouble(ViolationCase::getDurationDays).average().orElse(7.0);
    }
}
