package com.ucm.controller;

import com.ucm.common.ApiResponse;
import com.ucm.service.MinioService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** 通用文件上传（现场核查照片等） */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final MinioService minioService;

    public FileController(MinioService minioService) {
        this.minioService = minioService;
    }

    @PostMapping("/upload")
    public ApiResponse<?> upload(@RequestParam("file") MultipartFile file) {
        String objectName = minioService.upload(minioService.evidenceBucket(), "evidence", file);
        return ApiResponse.ok(Map.of("objectName", objectName));
    }

    /** 照片预览预签名 URL */
    @GetMapping("/preview")
    public ApiResponse<?> preview(@RequestParam String objectName) {
        return ApiResponse.ok(Map.of("url", minioService.presignedGet(minioService.evidenceBucket(), objectName, 30)));
    }
}
