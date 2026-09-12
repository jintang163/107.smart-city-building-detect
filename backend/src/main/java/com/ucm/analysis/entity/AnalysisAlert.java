package com.ucm.analysis.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 智能分析预警（工期预测超期 / 临期未办结）。
 * 由定时任务扫描未办结工单生成，工单办结后自动关闭。
 */
@Entity
@Table(name = "analysis_alert")
public class AnalysisAlert {

    public enum AlertType {
        OVERDUE,   // 已超过预测完成时间仍未办结
        DUE_SOON   // 临近预测完成时间（剩余工期不足 20%）
    }

    public enum Status { OPEN, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_code", nullable = false, length = 32)
    private String orderCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 16)
    private AlertType alertType;

    @Column(length = 256)
    private String message;

    /** 预测完成时间 */
    @Column(name = "predicted_finish_at")
    private LocalDateTime predictedFinishAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.OPEN;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public AlertType getAlertType() { return alertType; }
    public void setAlertType(AlertType alertType) { this.alertType = alertType; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getPredictedFinishAt() { return predictedFinishAt; }
    public void setPredictedFinishAt(LocalDateTime predictedFinishAt) { this.predictedFinishAt = predictedFinishAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
}
