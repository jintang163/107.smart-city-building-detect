package com.ucm.repository;

import com.ucm.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    List<WorkOrder> findByStatus(WorkOrder.Status status);
    List<WorkOrder> findByAssigneeId(Long assigneeId);
    List<WorkOrder> findByStatusIn(List<WorkOrder.Status> statuses);
    long countByAssigneeIdAndStatusIn(Long assigneeId, List<WorkOrder.Status> statuses);
    boolean existsBySpotId(Long spotId);
}
