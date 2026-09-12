package com.ucm.analysis.service;

import com.ucm.analysis.entity.ViolationCase;
import com.ucm.analysis.repository.ViolationCaseRepository;
import com.ucm.entity.ChangeSpot;
import com.ucm.entity.WorkOrder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工期预测统计模型：按「违建类型 × 面积档」分组统计历史平均处理天数，
 * 样本不足时按 类型 -> 全局 -> 默认值 分层回退（非深度学习，纯统计）。
 */
@Service
public class DurationPredictService {

    /** 分组样本数下限，不足则向上一级回退 */
    private static final int MIN_SAMPLES = 3;
    /** 无历史数据时的默认预测工期（天） */
    private static final double DEFAULT_DAYS = 7.0;

    private final ViolationCaseRepository caseRepo;

    public DurationPredictService(ViolationCaseRepository caseRepo) {
        this.caseRepo = caseRepo;
    }

    /** 面积档：S <50㎡ / M 50~200㎡ / L 200~500㎡ / XL ≥500㎡ */
    public static String areaBucket(double areaM2) {
        if (areaM2 < 50) return "S";
        if (areaM2 < 200) return "M";
        if (areaM2 < 500) return "L";
        return "XL";
    }

    /** 图斑变化类型 -> 违建类型映射（工单未记录违建类型时的近似映射） */
    public static ViolationCase.ViolationType mapChangeType(String changeType) {
        if (changeType == null) return ViolationCase.ViolationType.OCCUPY_LAND;
        return switch (changeType) {
            case "EXPANSION" -> ViolationCase.ViolationType.ILLEGAL_EXPANSION;
            case "DEMOLITION" -> ViolationCase.ViolationType.TEMP_STRUCTURE;
            default -> ViolationCase.ViolationType.OCCUPY_LAND; // NEW_CONSTRUCTION 按占地违建处理
        };
    }

    /**
     * 预测处理天数。返回 [预测天数, 依据说明, 样本数]。
     */
    public Prediction predictDays(ViolationCase.ViolationType type, double areaM2) {
        List<ViolationCase> all = caseRepo.findAll().stream()
                .filter(c -> c.getDurationDays() != null).toList();
        String bucket = areaBucket(areaM2);

        // 第一级：同类型 × 同面积档
        List<ViolationCase> group = all.stream()
                .filter(c -> c.getViolationType() == type && areaBucket(c.getAreaM2()).equals(bucket))
                .toList();
        if (group.size() >= MIN_SAMPLES) {
            return new Prediction(avg(group), "同类型同面积档历史均值（样本 " + group.size() + " 条）", group.size());
        }
        // 第二级：同类型
        List<ViolationCase> byType = all.stream().filter(c -> c.getViolationType() == type).toList();
        if (byType.size() >= MIN_SAMPLES) {
            return new Prediction(avg(byType), "同类型历史均值（样本 " + byType.size() + " 条）", byType.size());
        }
        // 第三级：全局
        if (!all.isEmpty()) {
            return new Prediction(avg(all), "全局历史均值（样本 " + all.size() + " 条）", all.size());
        }
        return new Prediction(DEFAULT_DAYS, "无历史案例，使用默认工期", 0);
    }

    private static double avg(List<ViolationCase> cases) {
        return cases.stream().mapToDouble(ViolationCase::getDurationDays).average().orElse(DEFAULT_DAYS);
    }

    /** 针对在办工单的工期预测与超期判定 */
    public Map<String, Object> predictForOrder(WorkOrder order) {
        ChangeSpot spot = order.getSpot();
        ViolationCase.ViolationType type = mapChangeType(spot.getChangeType());
        double area = spot.getAreaM2() == null ? 0 : spot.getAreaM2();
        Prediction p = predictDays(type, area);

        LocalDateTime createdAt = order.getCreatedAt();
        LocalDateTime predictedFinishAt = createdAt.plusDays(Math.round(p.days()));
        LocalDateTime now = LocalDateTime.now();
        double elapsedDays = Duration.between(createdAt, now).toMinutes() / 1440.0;
        double remainingDays = p.days() - elapsedDays;
        boolean overdue = remainingDays < 0;
        // 剩余工期不足 20% 判定为临期
        boolean dueSoon = !overdue && remainingDays < p.days() * 0.2;

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderId", order.getId());
        m.put("orderCode", order.getCode());
        m.put("violationType", type.name());
        m.put("areaM2", area);
        m.put("predictedDays", Math.round(p.days() * 10) / 10.0);
        m.put("predictedFinishAt", predictedFinishAt);
        m.put("elapsedDays", Math.round(elapsedDays * 10) / 10.0);
        m.put("remainingDays", Math.round(remainingDays * 10) / 10.0);
        m.put("overdue", overdue);
        m.put("dueSoon", dueSoon);
        m.put("basis", p.basis());
        m.put("samples", p.samples());
        return m;
    }

    /** 预测结果值对象 */
    public record Prediction(double days, String basis, int samples) {}
}
