package com.ucm.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 航拍任务（一次航线执行） */
@Entity
@Table(name = "flight_task")
public class FlightTask {

    public enum Status { PLANNED, EXECUTING, DONE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "route_id")
    private FlightRoute route;

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.PLANNED;

    @Column(name = "planned_date")
    private LocalDate plannedDate;

    /** 执行人 */
    @Column(length = 64)
    private String operator;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FlightRoute getRoute() { return route; }
    public void setRoute(FlightRoute route) { this.route = route; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDate getPlannedDate() { return plannedDate; }
    public void setPlannedDate(LocalDate plannedDate) { this.plannedDate = plannedDate; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
