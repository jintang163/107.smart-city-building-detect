package com.ucm.repository;

import com.ucm.entity.WorkOrderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkOrderLogRepository extends JpaRepository<WorkOrderLog, Long> {
    List<WorkOrderLog> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}
