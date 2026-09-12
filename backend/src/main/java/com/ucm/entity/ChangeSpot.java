package com.ucm.entity;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Polygon;
import java.time.LocalDateTime;

/** 变化图斑（AI 自动标注的疑似违建） */
@Entity
@Table(name = "change_spot")
public class ChangeSpot {

    public enum Status { PENDING, CONFIRMED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "compare_task_id", nullable = false)
    private CompareTask compareTask;

    /** 图斑轮廓（EPSG:4326） */
    @Column(columnDefinition = "geometry(Polygon,4326)")
    private Polygon geom;

    /** 面积（平方米） */
    @Column(name = "area_m2")
    private Double areaM2;

    /** AI 置信度 0~1 */
    private Double confidence;

    /** 变化类型：NEW_CONSTRUCTION 新增构筑物 / EXPANSION 扩建 / DEMOLITION 拆除 */
    @Column(name = "change_type", length = 32)
    private String changeType = "NEW_CONSTRUCTION";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.PENDING;

    @Column(name = "review_by", length = 64)
    private String reviewBy;

    @Column(name = "review_at")
    private LocalDateTime reviewAt;

    @Column(name = "review_comment", length = 512)
    private String reviewComment;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CompareTask getCompareTask() { return compareTask; }
    public void setCompareTask(CompareTask compareTask) { this.compareTask = compareTask; }
    public Polygon getGeom() { return geom; }
    public void setGeom(Polygon geom) { this.geom = geom; }
    public Double getAreaM2() { return areaM2; }
    public void setAreaM2(Double areaM2) { this.areaM2 = areaM2; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getReviewBy() { return reviewBy; }
    public void setReviewBy(String reviewBy) { this.reviewBy = reviewBy; }
    public LocalDateTime getReviewAt() { return reviewAt; }
    public void setReviewAt(LocalDateTime reviewAt) { this.reviewAt = reviewAt; }
    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
