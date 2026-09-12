package com.ucm.analysis.service;

import com.ucm.analysis.drools.RectifyFact;
import com.ucm.analysis.entity.ViolationCase;
import com.ucm.analysis.repository.ViolationCaseRepository;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 整改方案推荐：Drools 规则引擎决策整改方式（自拆/助拆/强拆），
 * 历史案例统计模型校正工时预估。
 */
@Service
public class RecommendService {

    /** 网格内未办结工单达到该数即判定执法热点 */
    public static final int HOTSPOT_THRESHOLD = 3;
    /** 网格内历史案例达到该数即判定重复违建高发区 */
    public static final int REPEAT_AREA_THRESHOLD = 3;
    /** 历史工时样本达到该数才参与加权校正 */
    private static final int MIN_HOUR_SAMPLES = 3;

    private final KieContainer kieContainer;
    private final ViolationCaseRepository caseRepo;
    private final HeatmapService heatmapService;

    public RecommendService(KieContainer kieContainer, ViolationCaseRepository caseRepo, HeatmapService heatmapService) {
        this.kieContainer = kieContainer;
        this.caseRepo = caseRepo;
        this.heatmapService = heatmapService;
    }

    /**
     * 推荐整改方案。
     *
     * @param violationType 违建类型（ViolationType 枚举名）
     * @param areaM2        面积（平方米）
     * @param lng           经度（用于热点/重复高发判定，可空）
     * @param lat           纬度（可空）
     */
    public Map<String, Object> recommend(String violationType, double areaM2, Double lng, Double lat) {
        boolean hotspot = false;
        boolean repeatArea = false;
        String gridCode = null;
        if (lng != null && lat != null) {
            gridCode = GridUtil.gridCode(lng, lat);
            hotspot = heatmapService.openOrderCount(gridCode) >= HOTSPOT_THRESHOLD;
            repeatArea = caseRepo.findByGridCode(gridCode).size() >= REPEAT_AREA_THRESHOLD;
        }

        // 规则引擎决策
        RectifyFact fact = new RectifyFact(violationType, areaM2, hotspot, repeatArea);
        KieSession session = kieContainer.newKieSession();
        try {
            session.insert(fact);
            session.fireAllRules();
        } finally {
            session.dispose();
        }

        // 规则工时 = 基础工时 + 每平米工时 × 面积
        double ruleHours = fact.getBaseHours() + fact.getPerM2Hours() * areaM2;

        // 统计校正：同类型历史案例平均实际工时，样本充足时与规则工时各半加权
        List<ViolationCase> sameType = caseRepo.findByViolationType(ViolationCase.ViolationType.valueOf(violationType));
        List<ViolationCase> withHours = sameType.stream().filter(c -> c.getWorkHours() != null).toList();
        Double historyAvgHours = null;
        double estimatedHours = ruleHours;
        if (withHours.size() >= MIN_HOUR_SAMPLES) {
            historyAvgHours = withHours.stream().mapToDouble(ViolationCase::getWorkHours).average().orElse(0);
            // 面积差异较大时对历史均值做面积比例缩放
            double avgArea = withHours.stream().mapToDouble(ViolationCase::getAreaM2).average().orElse(areaM2);
            double scaledHistory = historyAvgHours * (avgArea > 0 ? areaM2 / avgArea : 1.0);
            estimatedHours = 0.5 * ruleHours + 0.5 * scaledHistory;
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("violationType", violationType);
        m.put("areaM2", areaM2);
        m.put("rectifyMethod", fact.getRectifyMethod());
        m.put("riskLevel", fact.getRiskLevel());
        m.put("reason", fact.getReason());
        m.put("estimatedHours", Math.round(estimatedHours * 10) / 10.0);
        m.put("ruleHours", Math.round(ruleHours * 10) / 10.0);
        m.put("historyAvgHours", historyAvgHours == null ? null : Math.round(historyAvgHours * 10) / 10.0);
        m.put("hotspot", hotspot);
        m.put("repeatArea", repeatArea);
        m.put("gridCode", gridCode);
        m.put("similarCases", sameType.size());
        return m;
    }
}
