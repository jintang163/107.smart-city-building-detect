package com.ucm.controller;

import com.ucm.common.ApiResponse;
import com.ucm.common.GeoJsonUtil;
import com.ucm.entity.ChangeSpot;
import com.ucm.entity.Imagery;
import com.ucm.repository.ChangeSpotRepository;
import com.ucm.repository.ImageryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 地图查询：图斑 / 影像范围 以 GeoJSON 输出 */
@RestController
@RequestMapping("/api/map")
public class MapController {

    private final ChangeSpotRepository spotRepo;
    private final ImageryRepository imageryRepo;

    public MapController(ChangeSpotRepository spotRepo, ImageryRepository imageryRepo) {
        this.spotRepo = spotRepo;
        this.imageryRepo = imageryRepo;
    }

    /** 图斑 GeoJSON（可按状态过滤） */
    @GetMapping("/spots")
    public ApiResponse<?> spots(@RequestParam(required = false) String status) {
        List<ChangeSpot> spots = (status == null || status.isBlank())
                ? spotRepo.findAll()
                : spotRepo.findByStatus(ChangeSpot.Status.valueOf(status));
        List<Map<String, Object>> features = spots.stream().map(s -> {
            Map<String, Object> props = new HashMap<>();
            props.put("id", s.getId());
            props.put("areaM2", s.getAreaM2());
            props.put("confidence", s.getConfidence());
            props.put("changeType", s.getChangeType());
            props.put("status", s.getStatus().name());
            return GeoJsonUtil.feature(s.getGeom(), props);
        }).toList();
        return ApiResponse.ok(GeoJsonUtil.featureCollection(features));
    }

    /** 影像覆盖范围 GeoJSON */
    @GetMapping("/imagery")
    public ApiResponse<?> imagery() {
        List<Map<String, Object>> features = imageryRepo.findAll().stream().map(i -> {
            Map<String, Object> props = new HashMap<>();
            props.put("id", i.getId());
            props.put("name", i.getName());
            props.put("type", i.getType().name());
            props.put("captureDate", i.getCaptureDate() == null ? null : i.getCaptureDate().toString());
            return GeoJsonUtil.feature(i.getFootprint(), props);
        }).toList();
        return ApiResponse.ok(GeoJsonUtil.featureCollection(features));
    }
}
