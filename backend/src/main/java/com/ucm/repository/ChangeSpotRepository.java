package com.ucm.repository;

import com.ucm.entity.ChangeSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChangeSpotRepository extends JpaRepository<ChangeSpot, Long> {
    List<ChangeSpot> findByStatus(ChangeSpot.Status status);
    List<ChangeSpot> findByCompareTaskId(Long compareTaskId);
}
