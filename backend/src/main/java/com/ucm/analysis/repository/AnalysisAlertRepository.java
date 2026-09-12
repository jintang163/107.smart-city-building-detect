package com.ucm.analysis.repository;

import com.ucm.analysis.entity.AnalysisAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisAlertRepository extends JpaRepository<AnalysisAlert, Long> {
    boolean existsByOrderIdAndAlertTypeAndStatus(Long orderId, AnalysisAlert.AlertType alertType, AnalysisAlert.Status status);
    List<AnalysisAlert> findByStatusOrderByCreatedAtDesc(AnalysisAlert.Status status);
    List<AnalysisAlert> findByStatus(AnalysisAlert.Status status);
}
