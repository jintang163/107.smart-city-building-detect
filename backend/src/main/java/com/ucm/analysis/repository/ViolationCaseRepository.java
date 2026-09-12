package com.ucm.analysis.repository;

import com.ucm.analysis.entity.ViolationCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ViolationCaseRepository extends JpaRepository<ViolationCase, Long> {
    boolean existsBySourceOrderId(Long sourceOrderId);
    List<ViolationCase> findByViolationType(ViolationCase.ViolationType violationType);
    List<ViolationCase> findByGridCode(String gridCode);
}
