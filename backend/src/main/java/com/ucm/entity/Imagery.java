package com.ucm.entity;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Polygon;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 航拍影像（正射/倾斜），文件存 MinIO，空间范围存 PostGIS */
@Entity
@Table(name = "imagery")
public class Imagery {

    public enum Type { ORTHO, OBLIQUE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "task_id")
    private FlightTask task;

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Type type = Type.ORTHO;

    @Column(name = "capture_date")
    private LocalDate captureDate;

    @Column(nullable = false, length = 64)
    private String bucket;

    @Column(name = "object_name", nullable = false, length = 256)
    private String objectName;

    /** 影像空间范围（EPSG:4326） */
    @Column(columnDefinition = "geometry(Polygon,4326)")
    private Polygon footprint;

    /** 冗余 bbox，便于 AI 服务做像素→地理坐标仿射换算 */
    private Double minx;
    private Double miny;
    private Double maxx;
    private Double maxy;

    /** 像素尺寸（可选） */
    private Integer width;
    private Integer height;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FlightTask getTask() { return task; }
    public void setTask(FlightTask task) { this.task = task; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public LocalDate getCaptureDate() { return captureDate; }
    public void setCaptureDate(LocalDate captureDate) { this.captureDate = captureDate; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public String getObjectName() { return objectName; }
    public void setObjectName(String objectName) { this.objectName = objectName; }
    public Polygon getFootprint() { return footprint; }
    public void setFootprint(Polygon footprint) { this.footprint = footprint; }
    public Double getMinx() { return minx; }
    public void setMinx(Double minx) { this.minx = minx; }
    public Double getMiny() { return miny; }
    public void setMiny(Double miny) { this.miny = miny; }
    public Double getMaxx() { return maxx; }
    public void setMaxx(Double maxx) { this.maxx = maxx; }
    public Double getMaxy() { return maxy; }
    public void setMaxy(Double maxy) { this.maxy = maxy; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
