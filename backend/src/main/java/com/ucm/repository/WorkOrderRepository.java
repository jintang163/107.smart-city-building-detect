package com.ucm.repository;

import com.ucm.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    List<WorkOrder> findByStatus(WorkOrder.Status status);
    List<WorkOrder> findByAssigneeId(Long assigneeId);
    boolean existsBySpotId(Long spotId);
}
