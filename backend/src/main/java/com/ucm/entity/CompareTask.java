package com.ucm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 时序影像对比任务（历史底图 vs 新影像） */
@Entity
@Table(name = "compare_task")
public class CompareTask {

    public enum Status { PENDING, RUNNING, SUCCESS, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "base_imagery_id", nullable = false)
    private Imagery baseImagery;

    @ManyToOne
    @JoinColumn(name = "new_imagery_id", nullable = false)
    private Imagery newImagery;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.PENDING;

    /** 最小图斑面积（平方米），过滤噪点 */
    @Column(name = "min_area_m2")
    private Double minAreaM2 = 20.0;

    @Column(name = "spot_count")
    private Integer spotCount = 0;

    @Column(name = "error_msg", length = 1024)
    private String errorMsg;

    @OneToMany(mappedBy = "compareTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChangeSpot> spots = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Imagery getBaseImagery() { return baseImagery; }
    public void setBaseImagery(Imagery baseImagery) { this.baseImagery = baseImagery; }
    public Imagery getNewImagery() { return newImagery; }
    public void setNewImagery(Imagery newImagery) { this.newImagery = newImagery; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Double getMinAreaM2() { return minAreaM2; }
    public void setMinAreaM2(Double minAreaM2) { this.minAreaM2 = minAreaM2; }
    public Integer getSpotCount() { return spotCount; }
    public void setSpotCount(Integer spotCount) { this.spotCount = spotCount; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public List<ChangeSpot> getSpots() { return spots; }
    public void setSpots(List<ChangeSpot> spots) { this.spots = spots; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
}
