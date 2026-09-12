package com.ucm.analysis.service;

import com.ucm.analysis.entity.AnalysisAlert;
import com.ucm.analysis.repository.AnalysisAlertRepository;
import com.ucm.entity.WorkOrder;
import com.ucm.repository.WorkOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 超期预警：定时扫描未办结工单，对照工期预测结果，
 * 超期 / 临期自动生成预警；工单办结后自动关闭对应预警。
 */
@Service
public class OverdueAlertService {

    private static final Logger log = LoggerFactory.getLogger(OverdueAlertService.class);

    private final WorkOrderRepository orderRepo;
    private final AnalysisAlertRepository alertRepo;
    private final DurationPredictService predictService;

    public OverdueAlertService(WorkOrderRepository orderRepo, AnalysisAlertRepository alertRepo,
                               DurationPredictService predictService) {
        this.orderRepo = orderRepo;
        this.alertRepo = alertRepo;
        this.predictService = predictService;
    }

    /** 每小时扫描一次（可用 analysis.alert.scan-interval-ms 覆盖） */
    @Scheduled(fixedDelayString = "${analysis.alert.scan-interval-ms:3600000}", initialDelay = 60000)
    @Transactional
    public void scan() {
        int created = 0;
        for (WorkOrder order : orderRepo.findByStatusIn(HeatmapService.OPEN_STATUSES)) {
            Map<String, Object> p = predictService.predictForOrder(order);
            if (Boolean.TRUE.equals(p.get("overdue"))) {
                created += createIfAbsent(order, AnalysisAlert.AlertType.OVERDUE, p);
            } else if (Boolean.TRUE.equals(p.get("dueSoon"))) {
                created += createIfAbsent(order, AnalysisAlert.AlertType.DUE_SOON, p);
            }
        }
        int closed = closeFinished();
        if (created > 0 || closed > 0) {
            log.info("超期预警扫描完成: 新增 {} 条, 关闭 {} 条", created, closed);
        }
    }

    /** 工单办结（归档/排除）后立即关闭其未处理预警 */
    @Transactional
    public void closeByOrderId(Long orderId) {
        for (AnalysisAlert a : alertRepo.findByStatus(AnalysisAlert.Status.OPEN)) {
            if (a.getOrderId().equals(orderId)) {
                a.setStatus(AnalysisAlert.Status.CLOSED);
                a.setClosedAt(LocalDateTime.now());
                alertRepo.save(a);
            }
        }
    }

    private int createIfAbsent(WorkOrder order, AnalysisAlert.AlertType type, Map<String, Object> p) {
        boolean exists = alertRepo.existsByOrderIdAndAlertTypeAndStatus(
                order.getId(), type, AnalysisAlert.Status.OPEN);
        if (exists) return 0;
        AnalysisAlert alert = new AnalysisAlert();
        alert.setOrderId(order.getId());
        alert.setOrderCode(order.getCode());
        alert.setAlertType(type);
        alert.setPredictedFinishAt((LocalDateTime) p.get("predictedFinishAt"));
        alert.setMessage(type == AnalysisAlert.AlertType.OVERDUE
                ? String.format("已超预测工期 %.1f 天未办结（预测 %.1f 天）",
                    -(double) p.get("remainingDays"), (double) p.get("predictedDays"))
                : String.format("临近预测完成时间，剩余 %.1f 天", (double) p.get("remainingDays")));
        alertRepo.save(alert);
        return 1;
    }

    /** 已办结工单的未处理预警自动关闭，返回关闭数量 */
    private int closeFinished() {
        int closed = 0;
        for (AnalysisAlert a : alertRepo.findByStatus(AnalysisAlert.Status.OPEN)) {
            WorkOrder order = orderRepo.findById(a.getOrderId()).orElse(null);
            if (order == null || !HeatmapService.OPEN_STATUSES.contains(order.getStatus())) {
                a.setStatus(AnalysisAlert.Status.CLOSED);
                a.setClosedAt(LocalDateTime.now());
                alertRepo.save(a);
                closed++;
            }
        }
        return closed;
    }
}
