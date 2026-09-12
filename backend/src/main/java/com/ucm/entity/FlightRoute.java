package com.ucm.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Polygon;
import java.time.LocalDateTime;

/** 无人机航线 */
@Entity
@Table(name = "flight_route")
public class FlightRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    /** 覆盖区域（EPSG:4326） */
    @Column(columnDefinition = "geometry(Polygon,4326)")
    private Polygon region;

    /** 航点 JSON: [{lng,lat,alt}, ...] */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String waypoints;

    /** 航高（米） */
    private Double altitude;

    /** 旁向/航向重叠度（%） */
    private Double overlap;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Polygon getRegion() { return region; }
    public void setRegion(Polygon region) { this.region = region; }
    public String getWaypoints() { return waypoints; }
    public void setWaypoints(String waypoints) { this.waypoints = waypoints; }
    public Double getAltitude() { return altitude; }
    public void setAltitude(Double altitude) { this.altitude = altitude; }
    public Double getOverlap() { return overlap; }
    public void setOverlap(Double overlap) { this.overlap = overlap; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
