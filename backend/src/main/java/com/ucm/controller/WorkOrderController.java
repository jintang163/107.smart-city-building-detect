package com.ucm.controller;

import com.ucm.common.ApiResponse;
import com.ucm.common.GeoJsonUtil;
import com.ucm.common.UserContext;
import com.ucm.entity.ChangeSpot;
import com.ucm.entity.WorkOrder;
import com.ucm.entity.WorkOrderLog;
import com.ucm.service.WorkOrderService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 核查工单流转 */
@RestController
@RequestMapping("/api/orders")
public class WorkOrderController {

    private final WorkOrderService orderService;

    public WorkOrderController(WorkOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String status,
                               @RequestParam(required = false) Boolean mine) {
        WorkOrder.Status st = status == null || status.isBlank() ? null : WorkOrder.Status.valueOf(status);
        Long assigneeId = Boolean.TRUE.equals(mine) ? UserContext.get().getId() : null;
        return ApiResponse.ok(orderService.list(st, assigneeId).stream().map(this::toDto).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        WorkOrder o = orderService.detail(id);
        Map<String, Object> m = toDto(o);
        List<Map<String, Object>> logs = orderService.logs(id).stream().map(this::logDto).toList();
        m.put("logs", logs);
        return ApiResponse.ok(m);
    }

    /** 工单动作：ASSIGN/CONFIRM/EXCLUDE/RECTIFY/ARCHIVE */
    @PostMapping("/{id}/action")
    public ApiResponse<?> action(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String action = (String) body.get("action");
        Long assigneeId = body.get("assigneeId") == null ? null : ((Number) body.get("assigneeId")).longValue();
        String comment = body.get("comment") == null ? null : body.get("comment").toString();
        String photos = body.get("photos") == null ? null : body.get("photos").toString();
        return ApiResponse.ok(toDto(orderService.action(id, action, assigneeId, comment, photos)));
    }

    private Map<String, Object> toDto(WorkOrder o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getId());
        m.put("code", o.getCode());
        m.put("title", o.getTitle());
        m.put("description", o.getDescription());
        m.put("status", o.getStatus().name());
        m.put("priority", o.getPriority());
        m.put("deadline", o.getDeadline());
        m.put("assigneeId", o.getAssignee() == null ? null : o.getAssignee().getId());
        m.put("assigneeName", o.getAssignee() == null ? null : o.getAssignee().getRealName());
        ChangeSpot spot = o.getSpot();
        m.put("spotId", spot.getId());
        m.put("spotAreaM2", spot.getAreaM2());
        m.put("spotConfidence", spot.getConfidence());
        m.put("spotGeom", GeoJsonUtil.toGeoJson(spot.getGeom()));
        m.put("createdAt", o.getCreatedAt());
        m.put("updatedAt", o.getUpdatedAt());
        return m;
    }

    private Map<String, Object> logDto(WorkOrderLog l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", l.getId());
        m.put("action", l.getAction());
        m.put("operatorName", l.getOperatorName());
        m.put("comment", l.getComment());
        m.put("photos", l.getPhotos());
        m.put("createdAt", l.getCreatedAt());
        return m;
    }
}
