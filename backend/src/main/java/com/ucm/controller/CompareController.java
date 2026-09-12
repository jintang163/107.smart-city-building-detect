package com.ucm.controller;

import com.ucm.common.ApiResponse;
import com.ucm.common.BizException;
import com.ucm.entity.CompareTask;
import com.ucm.repository.CompareTaskRepository;
import com.ucm.service.CompareService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** 时序对比任务 */
@RestController
@RequestMapping("/api/compare")
public class CompareController {

    private final CompareTaskRepository compareRepo;
    private final CompareService compareService;

    public CompareController(CompareTaskRepository compareRepo, CompareService compareService) {
        this.compareRepo = compareRepo;
        this.compareService = compareService;
    }

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.ok(compareRepo.findAll().stream().map(this::toDto).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        return ApiResponse.ok(toDto(compareRepo.findById(id)
                .orElseThrow(() -> new BizException("对比任务不存在"))));
    }

    /** 创建对比任务并异步执行 AI 检测 */
    @PostMapping
    public ApiResponse<?> create(@RequestBody Map<String, Object> body) {
        Long baseId = ((Number) body.get("baseImageryId")).longValue();
        Long newId = ((Number) body.get("newImageryId")).longValue();
        Double minArea = body.get("minAreaM2") == null ? null : ((Number) body.get("minAreaM2")).doubleValue();
        CompareTask task = compareService.create(baseId, newId, minArea);
        compareService.runAsync(task.getId());
        return ApiResponse.ok(toDto(task));
    }

    private Map<String, Object> toDto(CompareTask t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("baseImageryId", t.getBaseImagery().getId());
        m.put("baseImageryName", t.getBaseImagery().getName());
        m.put("newImageryId", t.getNewImagery().getId());
        m.put("newImageryName", t.getNewImagery().getName());
        m.put("status", t.getStatus().name());
        m.put("minAreaM2", t.getMinAreaM2());
        m.put("spotCount", t.getSpotCount());
        m.put("errorMsg", t.getErrorMsg());
        m.put("createdAt", t.getCreatedAt());
        m.put("finishedAt", t.getFinishedAt());
        return m;
    }
}
