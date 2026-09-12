package com.ucm.analysis.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 历史违建处置案例库（智能分析模块的训练数据源）。
 * 独立于工单表：工单归档后把处置结果同步到本表作为训练标签，
 * 也支持手工录入历史案例做冷启动。
 */
@Entity
@Table(name = "violation_case")
public class ViolationCase {

    /** 违建类型 */
    public enum ViolationType {
        ROOFTOP_ADDITION,   // 屋顶加盖
        ILLEGAL_EXPANSION,  // 违法扩建
        OCCUPY_LAND,        // 违法占地
        TEMP_STRUCTURE      // 临时搭建
    }

    /** 整改方式 */
    public enum RectifyMethod {
        SELF_DEMOLITION,     // 自拆（限期自行拆除）
        ASSISTED_DEMOLITION, // 助拆（执法队协助拆除）
        FORCED_DEMOLITION    // 强拆（强制拆除）
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 来源工单 ID（手工录入的案例为空）；唯一，保证同步幂等 */
    @Column(name = "source_order_id", unique = true)
    private Long sourceOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_type", nullable = false, length = 32)
    private ViolationType violationType;

    /** 违建面积（平方米） */
    @Column(name = "area_m2", nullable = false)
    private Double areaM2;

    /** 图斑质心经度（EPSG:4326） */
    private Double longitude;

    /** 图斑质心纬度（EPSG:4326） */
    private Double latitude;

    /** 0.01° 网格编码，如 "11430_3055"，用于区域聚合统计 */
    @Column(name = "grid_code", length = 32)
    private String gridCode;

    /** 实际采取的整改方式（训练标签） */
    @Enumerated(EnumType.STRING)
    @Column(name = "rectify_method", nullable = false, length = 32)
    private RectifyMethod rectifyMethod;

    /** 实际处理天数（工单创建 -> 归档） */
    @Column(name = "duration_days")
    private Double durationDays;

    /** 实际投入工时（小时，可空） */
    @Column(name = "work_hours")
    private Double workHours;

    /** 处置队员 */
    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "operator_name", length = 64)
    private String operatorName;

    /** 办结时间 */
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSourceOrderId() { return sourceOrderId; }
    public void setSourceOrderId(Long sourceOrderId) { this.sourceOrderId = sourceOrderId; }
    public ViolationType getViolationType() { return violationType; }
    public void setViolationType(ViolationType violationType) { this.violationType = violationType; }
    public Double getAreaM2() { return areaM2; }
    public void setAreaM2(Double areaM2) { this.areaM2 = areaM2; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public String getGridCode() { return gridCode; }
    public void setGridCode(String gridCode) { this.gridCode = gridCode; }
    public RectifyMethod getRectifyMethod() { return rectifyMethod; }
    public void setRectifyMethod(RectifyMethod rectifyMethod) { this.rectifyMethod = rectifyMethod; }
    public Double getDurationDays() { return durationDays; }
    public void setDurationDays(Double durationDays) { this.durationDays = durationDays; }
    public Double getWorkHours() { return workHours; }
    public void setWorkHours(Double workHours) { this.workHours = workHours; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
