package com.ucm.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

/** 工单流转日志（派单/核查/认定/整改/归档全程留痕） */
@Entity
@Table(name = "work_order_log")
public class WorkOrderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private WorkOrder order;

    /** 动作：ASSIGN/INSPECT/CONFIRM/EXCLUDE/RECTIFY/ARCHIVE */
    @Column(nullable = false, length = 32)
    private String action;

    @Column(name = "operator_name", length = 64)
    private String operatorName;

    @Column(length = 512)
    private String comment;

    /** 现场照片对象名 JSON 数组 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String photos;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public WorkOrder getOrder() { return order; }
    public void setOrder(WorkOrder order) { this.order = order; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getPhotos() { return photos; }
    public void setPhotos(String photos) { this.photos = photos; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
