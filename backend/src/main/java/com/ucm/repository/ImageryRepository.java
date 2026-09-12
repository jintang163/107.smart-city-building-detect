package com.ucm.repository;

import com.ucm.entity.Imagery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ImageryRepository extends JpaRepository<Imagery, Long> {
    List<Imagery> findByTaskId(Long taskId);
}
