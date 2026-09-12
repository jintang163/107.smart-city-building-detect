package com.ucm.analysis.controller;

import com.ucm.analysis.entity.AnalysisAlert;
import com.ucm.analysis.entity.ViolationCase;
import com.ucm.analysis.repository.AnalysisAlertRepository;
import com.ucm.analysis.service.CaseLibraryService;
import com.ucm.analysis.service.DispatchService;
import com.ucm.analysis.service.DurationPredictService;
import com.ucm.analysis.service.HeatmapService;
import com.ucm.analysis.service.OverdueAlertService;
import com.ucm.analysis.service.RecommendService;
import com.ucm.common.ApiResponse;
import com.ucm.common.BizException;
import com.ucm.entity.WorkOrder;
import com.ucm.repository.WorkOrderRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能分析模块：整改方案推荐 / 执法热力图 / 工期预测与超期预警 / 队员能力匹配 / 案例库。
 * 数据源独立于工单表（violation_case），仅从工单结果同步训练标签。
 */
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final RecommendService recommendService;
    private final HeatmapService heatmapService;
    private final DurationPredictService predictService;
    private final DispatchService dispatchService;
    private final CaseLibraryService caseService;
    private final OverdueAlertService alertService;
    private final AnalysisAlertRepository alertRepo;
    private final WorkOrderRepository orderRepo;

    public AnalysisController(RecommendService recommendService, HeatmapService heatmapService,
                              DurationPredictService predictService, DispatchService dispatchService,
                              CaseLibraryService caseService, OverdueAlertService alertService,
                              AnalysisAlertRepository alertRepo, WorkOrderRepository orderRepo) {
        this.recommendService = recommendService;
        this.heatmapService = heatmapService;
        this.predictService = predictService;
        this.dispatchService = dispatchService;
        this.caseService = caseService;
        this.alertService = alertService;
        this.alertRepo = alertRepo;
        this.orderRepo = orderRepo;
    }

    // ---------- 整改方案推荐 ----------

    /** 入参：violationType / areaM2 / lng / lat，输出推荐整改方式 + 预估工时 + 命中规则 */
    @PostMapping("/recommend")
    public ApiResponse<?> recommend(@RequestBody Map<String, Object> body) {
        String type = (String) body.get("violationType");
        if (type == null || type.isBlank()) throw new BizException("violationType 不能为空");
        try {
            ViolationCase.ViolationType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new BizException("未知违建类型: " + type);
        }
        double areaM2 = body.get("areaM2") == null ? 0 : ((Number) body.get("areaM2")).doubleValue();
        if (areaM2 <= 0) throw new BizException("areaM2 必须大于 0");
        Double lng = body.get("lng") == null ? null : ((Number) body.get("lng")).doubleValue();
        Double lat = body.get("lat") == null ? null : ((Number) body.get("lat")).doubleValue();

        Map<String, Object> result = recommendService.recommend(type, areaM2, lng, lat);
        // 附带工期预测，一次调用给全处置建议
        var prediction = predictService.predictDays(ViolationCase.ViolationType.valueOf(type), areaM2);
        result.put("predictedDays", Math.round(prediction.days() * 10) / 10.0);
        result.put("predictBasis", prediction.basis());
        return ApiResponse.ok(result);
    }

    // ---------- 执法资源热力图 ----------

    /** 未办结工单网格热力 GeoJSON + 汇总 + 巡查/航线优先级 */
    @GetMapping("/heatmap")
    public ApiResponse<?> heatmap() {
        return ApiResponse.ok(heatmapService.heatmap());
    }

    // ---------- 工期预测与超期预警 ----------

    /** 全部未办结工单的工期预测 */
    @GetMapping("/predict")
    public ApiResponse<?> predictOpen() {
        List<Map<String, Object>> list = orderRepo.findByStatusIn(HeatmapService.OPEN_STATUSES)
                .stream().map(predictService::predictForOrder).toList();
        return ApiResponse.ok(list);
    }

    /** 单个工单工期预测 */
    @GetMapping("/predict/{orderId}")
    public ApiResponse<?> predictOne(@PathVariable Long orderId) {
        WorkOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new BizException("工单不存在: " + orderId));
        return ApiResponse.ok(predictService.predictForOrder(order));
    }

    /** 预警列表（默认未处理） */
    @GetMapping("/alerts")
    public ApiResponse<?> alerts(@RequestParam(required = false) String status) {
        AnalysisAlert.Status st = "CLOSED".equalsIgnoreCase(status == null ? "" : status)
                ? AnalysisAlert.Status.CLOSED : AnalysisAlert.Status.OPEN;
        return ApiResponse.ok(alertRepo.findByStatusOrderByCreatedAtDesc(st).stream()
                .map(this::alertDto).toList());
    }

    /** 手动触发一次预警扫描 */
    @PostMapping("/alerts/scan")
    public ApiResponse<?> scanAlerts() {
        alertService.scan();
        return ApiResponse.ok(Map.of("scanned", true));
    }

    /** 手动关闭预警 */
    @PostMapping("/alerts/{id}/close")
    public ApiResponse<?> closeAlert(@PathVariable Long id) {
        AnalysisAlert alert = alertRepo.findById(id)
                .orElseThrow(() -> new BizException("预警不存在: " + id));
        alert.setStatus(AnalysisAlert.Status.CLOSED);
        alert.setClosedAt(LocalDateTime.now());
        return ApiResponse.ok(alertDto(alertRepo.save(alert)));
    }

    // ---------- 队员能力匹配 ----------

    /** 队员能力画像 */
    @GetMapping("/members")
    public ApiResponse<?> members() {
        return ApiResponse.ok(dispatchService.memberProfiles());
    }

    /** 针对工单的智能分派建议（按得分降序） */
    @GetMapping("/dispatch/suggest")
    public ApiResponse<?> suggest(@RequestParam Long orderId) {
        return ApiResponse.ok(dispatchService.suggest(orderId));
    }

    // ---------- 历史案例库 ----------

    @GetMapping("/cases")
    public ApiResponse<?> cases(@RequestParam(required = false) String type) {
        ViolationCase.ViolationType t = (type == null || type.isBlank())
                ? null : ViolationCase.ViolationType.valueOf(type);
        return ApiResponse.ok(caseService.list(t).stream().map(this::caseDto).toList());
    }

    /** 手工录入历史案例 */
    @PostMapping("/cases")
    public ApiResponse<?> createCase(@RequestBody Map<String, Object> body) {
        ViolationCase c = new ViolationCase();
        c.setViolationType(ViolationCase.ViolationType.valueOf((String) body.get("violationType")));
        c.setAreaM2(((Number) body.get("areaM2")).doubleValue());
        c.setRectifyMethod(ViolationCase.RectifyMethod.valueOf((String) body.get("rectifyMethod")));
        if (body.get("lng") != null) c.setLongitude(((Number) body.get("lng")).doubleValue());
        if (body.get("lat") != null) c.setLatitude(((Number) body.get("lat")).doubleValue());
        if (body.get("durationDays") != null) c.setDurationDays(((Number) body.get("durationDays")).doubleValue());
        if (body.get("workHours") != null) c.setWorkHours(((Number) body.get("workHours")).doubleValue());
        if (body.get("operatorName") != null) c.setOperatorName(body.get("operatorName").toString());
        c.setFinishedAt(LocalDateTime.now());
        return ApiResponse.ok(caseDto(caseService.create(c)));
    }

    /** 从已归档工单批量补偿同步训练标签 */
    @PostMapping("/cases/sync")
    public ApiResponse<?> syncCases() {
        return ApiResponse.ok(caseService.syncAll());
    }

    // ---------- DTO ----------

    private Map<String, Object> caseDto(ViolationCase c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("sourceOrderId", c.getSourceOrderId());
        m.put("violationType", c.getViolationType().name());
        m.put("areaM2", c.getAreaM2());
        m.put("lng", c.getLongitude());
        m.put("lat", c.getLatitude());
        m.put("gridCode", c.getGridCode());
        m.put("rectifyMethod", c.getRectifyMethod().name());
        m.put("durationDays", c.getDurationDays());
        m.put("workHours", c.getWorkHours());
        m.put("operatorName", c.getOperatorName());
        m.put("finishedAt", c.getFinishedAt());
        return m;
    }

    private Map<String, Object> alertDto(AnalysisAlert a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("orderId", a.getOrderId());
        m.put("orderCode", a.getOrderCode());
        m.put("alertType", a.getAlertType().name());
        m.put("message", a.getMessage());
        m.put("predictedFinishAt", a.getPredictedFinishAt());
        m.put("status", a.getStatus().name());
        m.put("createdAt", a.getCreatedAt());
        return m;
    }
}
