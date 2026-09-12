package com.ucm.controller;

import com.ucm.common.ApiResponse;
import com.ucm.common.GeoJsonUtil;
import com.ucm.entity.ChangeSpot;
import com.ucm.entity.WorkOrder;
import com.ucm.service.SpotService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** 变化图斑审核 */
@RestController
@RequestMapping("/api/spots")
public class SpotController {

    private final SpotService spotService;

    public SpotController(SpotService spotService) {
        this.spotService = spotService;
    }

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String status,
                               @RequestParam(required = false) Long compareTaskId) {
        ChangeSpot.Status st = status == null || status.isBlank() ? null : ChangeSpot.Status.valueOf(status);
        return ApiResponse.ok(spotService.list(st, compareTaskId).stream().map(this::toDto).toList());
    }

    /** 审核：approved=true 确认违建并自动生成工单；false 排除 */
    @PostMapping("/{id}/review")
    public ApiResponse<?> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        String comment = body.get("comment") == null ? null : body.get("comment").toString();
        WorkOrder order = spotService.review(id, approved, comment);
        return ApiResponse.ok(order == null ? null : Map.of("orderId", order.getId(), "orderCode", order.getCode()));
    }

    private Map<String, Object> toDto(ChangeSpot s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("compareTaskId", s.getCompareTask().getId());
        m.put("geom", GeoJsonUtil.toGeoJson(s.getGeom()));
        m.put("areaM2", s.getAreaM2());
        m.put("confidence", s.getConfidence());
        m.put("changeType", s.getChangeType());
        m.put("status", s.getStatus().name());
        m.put("reviewBy", s.getReviewBy());
        m.put("reviewAt", s.getReviewAt());
        m.put("reviewComment", s.getReviewComment());
        m.put("createdAt", s.getCreatedAt());
        return m;
    }
}
