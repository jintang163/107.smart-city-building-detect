package com.ucm.controller;

import com.ucm.common.ApiResponse;
import com.ucm.common.BizException;
import com.ucm.entity.FlightTask;
import com.ucm.repository.FlightRouteRepository;
import com.ucm.repository.FlightTaskRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 航拍任务管理 */
@RestController
@RequestMapping("/api/tasks")
public class FlightTaskController {

    private final FlightTaskRepository taskRepo;
    private final FlightRouteRepository routeRepo;

    public FlightTaskController(FlightTaskRepository taskRepo, FlightRouteRepository routeRepo) {
        this.taskRepo = taskRepo;
        this.routeRepo = routeRepo;
    }

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.ok(taskRepo.findAll().stream().map(this::toDto).toList());
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody Map<String, Object> body) {
        FlightTask task = new FlightTask();
        apply(task, body);
        return ApiResponse.ok(toDto(taskRepo.save(task)));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        FlightTask task = taskRepo.findById(id).orElseThrow(() -> new BizException("任务不存在"));
        apply(task, body);
        return ApiResponse.ok(toDto(taskRepo.save(task)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        taskRepo.deleteById(id);
        return ApiResponse.ok(null);
    }

    private void apply(FlightTask task, Map<String, Object> body) {
        task.setName((String) body.get("name"));
        task.setOperator((String) body.get("operator"));
        if (body.get("status") != null) {
            task.setStatus(FlightTask.Status.valueOf(body.get("status").toString()));
        }
        if (body.get("plannedDate") != null) {
            task.setPlannedDate(LocalDate.parse(body.get("plannedDate").toString()));
        }
        if (body.get("routeId") != null) {
            task.setRoute(routeRepo.findById(((Number) body.get("routeId")).longValue())
                    .orElseThrow(() -> new BizException("航线不存在")));
        }
    }

    private Map<String, Object> toDto(FlightTask t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getName());
        m.put("status", t.getStatus().name());
        m.put("plannedDate", t.getPlannedDate());
        m.put("operator", t.getOperator());
        m.put("routeId", t.getRoute() == null ? null : t.getRoute().getId());
        m.put("routeName", t.getRoute() == null ? null : t.getRoute().getName());
        m.put("createdAt", t.getCreatedAt());
        return m;
    }
}
