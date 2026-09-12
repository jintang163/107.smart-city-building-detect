package com.ucm.service;

import com.ucm.common.BizException;
import com.ucm.common.GeoJsonUtil;
import com.ucm.entity.*;
import com.ucm.repository.*;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 时序对比任务编排：创建任务 -> 异步调用 AI 检测服务 -> 落库变化图斑 */
@Service
public class CompareService {

    private static final Logger log = LoggerFactory.getLogger(CompareService.class);

    private final CompareTaskRepository compareRepo;
    private final ImageryRepository imageryRepo;
    private final ChangeSpotRepository spotRepo;
    private final MinioService minioService;
    private final RestTemplate restTemplate;
    private final String detectUrl;

    public CompareService(CompareTaskRepository compareRepo,
                          ImageryRepository imageryRepo,
                          ChangeSpotRepository spotRepo,
                          MinioService minioService,
                          @Value("${ai.detect-url}") String detectUrl,
                          @Value("${ai.connect-timeout-ms:10000}") int connectTimeout,
                          @Value("${ai.read-timeout-ms:300000}") int readTimeout) {
        this.compareRepo = compareRepo;
        this.imageryRepo = imageryRepo;
        this.spotRepo = spotRepo;
        this.minioService = minioService;
        this.detectUrl = detectUrl;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        this.restTemplate = new RestTemplate(factory);
    }

    @Transactional
    public CompareTask create(Long baseImageryId, Long newImageryId, Double minAreaM2) {
        Imagery base = imageryRepo.findById(baseImageryId)
                .orElseThrow(() -> new BizException("历史底图不存在: " + baseImageryId));
        Imagery newer = imageryRepo.findById(newImageryId)
                .orElseThrow(() -> new BizException("新影像不存在: " + newImageryId));
        if (base.getId().equals(newer.getId())) {
            throw new BizException("对比的两期影像不能相同");
        }
        CompareTask task = new CompareTask();
        task.setBaseImagery(base);
        task.setNewImagery(newer);
        if (minAreaM2 != null) task.setMinAreaM2(minAreaM2);
        return compareRepo.save(task);
    }

    /** 异步执行：拉预签名 URL -> 调 AI 服务 -> 保存图斑 */
    @Async
    @Transactional
    public void runAsync(Long taskId) {
        CompareTask task = compareRepo.findById(taskId).orElse(null);
        if (task == null) return;
        task.setStatus(CompareTask.Status.RUNNING);
        compareRepo.save(task);
        try {
            Imagery base = task.getBaseImagery();
            Imagery newer = task.getNewImagery();
            String baseUrl = minioService.presignedGet(base.getBucket(), base.getObjectName(), 60);
            String newUrl = minioService.presignedGet(newer.getBucket(), newer.getObjectName(), 60);

            Map<String, Object> req = Map.of(
                    "base_url", baseUrl,
                    "new_url", newUrl,
                    "bbox", List.of(newer.getMinx(), newer.getMiny(), newer.getMaxx(), newer.getMaxy()),
                    "min_area_m2", task.getMinAreaM2()
            );
            log.info("调用 AI 检测服务, taskId={}", taskId);
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(detectUrl, req, Map.class);
            if (resp == null || !resp.containsKey("features")) {
                throw new BizException("AI 服务返回异常");
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> features = (List<Map<String, Object>>) resp.get("features");
            int count = 0;
            for (Map<String, Object> f : features) {
                @SuppressWarnings("unchecked")
                Map<String, Object> geometry = (Map<String, Object>) f.get("geometry");
                Polygon polygon = GeoJsonUtil.polygonFromGeoJson(geometry);
                ChangeSpot spot = new ChangeSpot();
                spot.setCompareTask(task);
                spot.setGeom(polygon);
                spot.setAreaM2(toDouble(f.get("area_m2")));
                spot.setConfidence(toDouble(f.get("confidence")));
                Object ct = f.get("change_type");
                if (ct != null) spot.setChangeType(ct.toString());
                spotRepo.save(spot);
                count++;
            }
            task.setSpotCount(count);
            task.setStatus(CompareTask.Status.SUCCESS);
            task.setFinishedAt(LocalDateTime.now());
            compareRepo.save(task);
            log.info("对比任务完成, taskId={}, 图斑数={}", taskId, count);
        } catch (Exception e) {
            log.error("对比任务失败, taskId=" + taskId, e);
            task.setStatus(CompareTask.Status.FAILED);
            task.setErrorMsg(e.getMessage());
            task.setFinishedAt(LocalDateTime.now());
            compareRepo.save(task);
        }
    }

    private Double toDouble(Object o) {
        return o == null ? null : ((Number) o).doubleValue();
    }
}
