package com.ucm.analysis.service;

import com.ucm.analysis.entity.ViolationCase;
import com.ucm.analysis.event.OrderArchivedEvent;
import com.ucm.analysis.repository.ViolationCaseRepository;
import com.ucm.common.BizException;
import com.ucm.entity.ChangeSpot;
import com.ucm.entity.SysUser;
import com.ucm.entity.WorkOrder;
import com.ucm.repository.WorkOrderRepository;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 历史案例库：训练数据源的读写入口。
 * 工单归档后通过事件把处置结果同步为训练标签（数据源独立于工单表）；
 * 也支持手工录入历史案例、批量补偿同步。
 */
@Service
public class CaseLibraryService {

    private static final Logger log = LoggerFactory.getLogger(CaseLibraryService.class);

    private final ViolationCaseRepository caseRepo;
    private final WorkOrderRepository orderRepo;
    private final OverdueAlertService alertService;

    public CaseLibraryService(ViolationCaseRepository caseRepo, WorkOrderRepository orderRepo,
                              OverdueAlertService alertService) {
        this.caseRepo = caseRepo;
        this.orderRepo = orderRepo;
        this.alertService = alertService;
    }

    public List<ViolationCase> list(ViolationCase.ViolationType type) {
        return type == null ? caseRepo.findAll() : caseRepo.findByViolationType(type);
    }

    /** 手工录入历史案例 */
    @Transactional
    public ViolationCase create(ViolationCase c) {
        if (c.getLongitude() != null && c.getLatitude() != null) {
            c.setGridCode(GridUtil.gridCode(c.getLongitude(), c.getLatitude()));
        }
        c.setSourceOrderId(null); // 手工录入无来源工单
        return caseRepo.save(c);
    }

    /** 工单归档后同步训练标签（事务提交后异步处理，失败不影响主流程） */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderArchived(OrderArchivedEvent event) {
        try {
            syncFromOrder(event.getOrderId(), event.getRectifyMethod());
            alertService.closeByOrderId(event.getOrderId());
        } catch (Exception e) {
            log.warn("同步归档工单 {} 到案例库失败: {}", event.getOrderId(), e.getMessage());
        }
    }

    /**
     * 把单个已归档工单同步为训练案例（幂等：按来源工单去重）。
     *
     * @return true 表示新建了案例
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean syncFromOrder(Long orderId, String rectifyMethod) {
        if (caseRepo.existsBySourceOrderId(orderId)) return false;
        WorkOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new BizException("工单不存在: " + orderId));
        if (order.getStatus() != WorkOrder.Status.ARCHIVED) {
            throw new BizException("仅已归档工单可同步为案例: " + order.getCode());
        }
        ViolationCase.RectifyMethod method = parseMethod(rectifyMethod);
        if (method == null) {
            // 无标签时用规则推荐值兜底，保证案例可用于训练
            method = ViolationCase.RectifyMethod.ASSISTED_DEMOLITION;
        }
        caseRepo.save(buildFromOrder(order, method));
        return true;
    }

    /** 批量补偿同步：把全部已归档但未入案例库的工单导入（整改方式未知的一律按助拆兜底） */
    @Transactional
    public Map<String, Object> syncAll() {
        int created = 0, skipped = 0;
        for (WorkOrder order : orderRepo.findByStatus(WorkOrder.Status.ARCHIVED)) {
            if (caseRepo.existsBySourceOrderId(order.getId())) {
                skipped++;
                continue;
            }
            caseRepo.save(buildFromOrder(order, ViolationCase.RectifyMethod.ASSISTED_DEMOLITION));
            created++;
        }
        return Map.of("created", created, "skipped", skipped);
    }

    private ViolationCase buildFromOrder(WorkOrder order, ViolationCase.RectifyMethod method) {
        ChangeSpot spot = order.getSpot();
        ViolationCase c = new ViolationCase();
        c.setSourceOrderId(order.getId());
        c.setViolationType(DurationPredictService.mapChangeType(spot.getChangeType()));
        c.setAreaM2(spot.getAreaM2() == null ? 0 : spot.getAreaM2());
        if (spot.getGeom() != null) {
            Point centroid = spot.getGeom().getCentroid();
            c.setLongitude(centroid.getX());
            c.setLatitude(centroid.getY());
            c.setGridCode(GridUtil.gridCode(centroid.getX(), centroid.getY()));
        }
        c.setRectifyMethod(method);
        LocalDateTime finishedAt = order.getUpdatedAt();
        c.setFinishedAt(finishedAt);
        c.setDurationDays(Duration.between(order.getCreatedAt(), finishedAt).toMinutes() / 1440.0);
        SysUser assignee = order.getAssignee();
        if (assignee != null) {
            c.setOperatorId(assignee.getId());
            c.setOperatorName(assignee.getRealName());
        }
        return c;
    }

    private static ViolationCase.RectifyMethod parseMethod(String name) {
        if (name == null || name.isBlank()) return null;
        try {
            return ViolationCase.RectifyMethod.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
