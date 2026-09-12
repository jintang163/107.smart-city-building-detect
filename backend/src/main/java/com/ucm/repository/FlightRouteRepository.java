package com.ucm.repository;

import com.ucm.entity.FlightRoute;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightRouteRepository extends JpaRepository<FlightRoute, Long> {
}
