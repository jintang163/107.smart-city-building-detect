package com.ucm.controller;

import com.ucm.common.ApiResponse;
import com.ucm.common.BizException;
import com.ucm.common.GeoJsonUtil;
import com.ucm.entity.FlightTask;
import com.ucm.entity.Imagery;
import com.ucm.repository.FlightTaskRepository;
import com.ucm.repository.ImageryRepository;
import com.ucm.service.MinioService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 影像入库与查询 */
@RestController
@RequestMapping("/api/imagery")
public class ImageryController {

    private final ImageryRepository imageryRepo;
    private final FlightTaskRepository taskRepo;
    private final MinioService minioService;

    public ImageryController(ImageryRepository imageryRepo, FlightTaskRepository taskRepo, MinioService minioService) {
        this.imageryRepo = imageryRepo;
        this.taskRepo = taskRepo;
        this.minioService = minioService;
    }

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) Long taskId) {
        List<Imagery> list = taskId == null ? imageryRepo.findAll() : imageryRepo.findByTaskId(taskId);
        return ApiResponse.ok(list.stream().map(this::toDto).toList());
    }

    /** 影像入库：文件上传 MinIO + 空间范围入 PostGIS */
    @PostMapping("/upload")
    public ApiResponse<?> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam String name,
                                 @RequestParam(required = false) Long taskId,
                                 @RequestParam(defaultValue = "ORTHO") String type,
                                 @RequestParam(required = false) String captureDate,
                                 @RequestParam Double minx, @RequestParam Double miny,
                                 @RequestParam Double maxx, @RequestParam Double maxy,
                                 @RequestParam(required = false) Integer width,
                                 @RequestParam(required = false) Integer height) {
        if (minx >= maxx || miny >= maxy) {
            throw new BizException("空间范围不合法：需要 minx<maxx 且 miny<maxy");
        }
        String objectName = minioService.upload(minioService.imageryBucket(), "imagery", file);

        Imagery imagery = new Imagery();
        imagery.setName(name);
        imagery.setType(Imagery.Type.valueOf(type));
        if (captureDate != null && !captureDate.isBlank()) {
            imagery.setCaptureDate(LocalDate.parse(captureDate));
        }
        if (taskId != null) {
            FlightTask task = taskRepo.findById(taskId).orElseThrow(() -> new BizException("任务不存在"));
            imagery.setTask(task);
        }
        imagery.setBucket(minioService.imageryBucket());
        imagery.setObjectName(objectName);
        imagery.setMinx(minx); imagery.setMiny(miny);
        imagery.setMaxx(maxx); imagery.setMaxy(maxy);
        imagery.setWidth(width); imagery.setHeight(height);
        imagery.setFootprint(GeoJsonUtil.bboxPolygon(minx, miny, maxx, maxy));
        return ApiResponse.ok(toDto(imageryRepo.save(imagery)));
    }

    /** 获取影像预览预签名 URL */
    @GetMapping("/{id}/preview")
    public ApiResponse<?> preview(@PathVariable Long id) {
        Imagery imagery = imageryRepo.findById(id).orElseThrow(() -> new BizException("影像不存在"));
        return ApiResponse.ok(Map.of("url", minioService.presignedGet(imagery.getBucket(), imagery.getObjectName(), 30)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        imageryRepo.deleteById(id);
        return ApiResponse.ok(null);
    }

    private Map<String, Object> toDto(Imagery i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.getId());
        m.put("name", i.getName());
        m.put("type", i.getType().name());
        m.put("captureDate", i.getCaptureDate());
        m.put("taskId", i.getTask() == null ? null : i.getTask().getId());
        m.put("taskName", i.getTask() == null ? null : i.getTask().getName());
        m.put("objectName", i.getObjectName());
        m.put("bbox", List.of(i.getMinx(), i.getMiny(), i.getMaxx(), i.getMaxy()));
        m.put("footprint", GeoJsonUtil.toGeoJson(i.getFootprint()));
        m.put("createdAt", i.getCreatedAt());
        return m;
    }
}
