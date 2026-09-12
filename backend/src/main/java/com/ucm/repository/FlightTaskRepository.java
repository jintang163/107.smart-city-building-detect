package com.ucm.repository;

import com.ucm.entity.FlightTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightTaskRepository extends JpaRepository<FlightTask, Long> {
}
