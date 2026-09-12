package com.ucm.service;

import com.ucm.common.BizException;
import com.ucm.common.UserContext;
import com.ucm.entity.SysUser;
import com.ucm.entity.WorkOrder;
import com.ucm.entity.WorkOrderLog;
import com.ucm.repository.SysUserRepository;
import com.ucm.repository.WorkOrderLogRepository;
import com.ucm.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 工单流转：派单 -> 现场核查 -> 认定/排除 -> 整改 -> 归档 */
@Service
public class WorkOrderService {

    /** 动作 -> (允许的前置状态, 目标状态) */
    private record Transition(Set<WorkOrder.Status> from, WorkOrder.Status to) {}

    private static final Map<String, Transition> TRANSITIONS = Map.of(
            "ASSIGN",   new Transition(Set.of(WorkOrder.Status.PENDING), WorkOrder.Status.INSPECTING),
            "CONFIRM",  new Transition(Set.of(WorkOrder.Status.INSPECTING), WorkOrder.Status.CONFIRMED),
            "EXCLUDE",  new Transition(Set.of(WorkOrder.Status.INSPECTING), WorkOrder.Status.EXCLUDED),
            "RECTIFY",  new Transition(Set.of(WorkOrder.Status.CONFIRMED), WorkOrder.Status.RECTIFYING),
            "ARCHIVE",  new Transition(Set.of(WorkOrder.Status.RECTIFYING, WorkOrder.Status.CONFIRMED), WorkOrder.Status.ARCHIVED)
    );

    private final WorkOrderRepository orderRepo;
    private final WorkOrderLogRepository logRepo;
    private final SysUserRepository userRepo;

    public WorkOrderService(WorkOrderRepository orderRepo, WorkOrderLogRepository logRepo, SysUserRepository userRepo) {
        this.orderRepo = orderRepo;
        this.logRepo = logRepo;
        this.userRepo = userRepo;
    }

    public List<WorkOrder> list(WorkOrder.Status status, Long assigneeId) {
        if (assigneeId != null) return orderRepo.findByAssigneeId(assigneeId);
        if (status != null) return orderRepo.findByStatus(status);
        return orderRepo.findAll();
    }

    public WorkOrder detail(Long id) {
        return orderRepo.findById(id).orElseThrow(() -> new BizException("工单不存在: " + id));
    }

    public List<WorkOrderLog> logs(Long orderId) {
        return logRepo.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    /** 执行工单动作（状态机校验 + 留痕） */
    @Transactional
    public WorkOrder action(Long orderId, String action, Long assigneeId, String comment, String photos) {
        WorkOrder order = detail(orderId);
        Transition t = TRANSITIONS.get(action);
        if (t == null) {
            throw new BizException("不支持的工单动作: " + action);
        }
        if (!t.from().contains(order.getStatus())) {
            throw new BizException(String.format("当前状态 %s 不允许执行 %s", order.getStatus(), action));
        }
        if ("ASSIGN".equals(action)) {
            if (assigneeId == null) throw new BizException("派单必须指定处置队员");
            SysUser assignee = userRepo.findById(assigneeId)
                    .orElseThrow(() -> new BizException("队员不存在: " + assigneeId));
            order.setAssignee(assignee);
        }
        order.setStatus(t.to());
        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);

        WorkOrderLog log = new WorkOrderLog();
        log.setOrder(order);
        log.setAction(action);
        log.setOperatorName(UserContext.username());
        log.setComment(comment);
        log.setPhotos(photos);
        logRepo.save(log);
        return order;
    }
}
