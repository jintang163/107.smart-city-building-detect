package com.ucm.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 核查工单。状态机：
 * PENDING(待核查) --派单--> INSPECTING(核查中)
 * INSPECTING --认定--> CONFIRMED(已认定) --整改--> RECTIFYING(整改中) --归档--> ARCHIVED
 * INSPECTING --排除--> EXCLUDED(已排除)
 */
@Entity
@Table(name = "work_order")
public class WorkOrder {

    public enum Status { PENDING, INSPECTING, CONFIRMED, EXCLUDED, RECTIFYING, ARCHIVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 工单编号，如 WO20260912-0001 */
    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @OneToOne
    @JoinColumn(name = "spot_id", nullable = false)
    private ChangeSpot spot;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 512)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.PENDING;

    /** 处置队员 */
    @ManyToOne
    @JoinColumn(name = "assignee_id")
    private SysUser assignee;

    /** 优先级 1高 2中 3低 */
    private Integer priority = 2;

    private LocalDate deadline;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public ChangeSpot getSpot() { return spot; }
    public void setSpot(ChangeSpot spot) { this.spot = spot; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public SysUser getAssignee() { return assignee; }
    public void setAssignee(SysUser assignee) { this.assignee = assignee; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
