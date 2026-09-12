package com.ucm.service;

import com.ucm.common.BizException;
import com.ucm.config.MinioConfig.MinioProperties;
import io.minio.*;
import io.minio.http.Method;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** MinIO 对象存储：影像与现场照片 */
@Service
public class MinioService {

    private final MinioClient client;
    private final MinioProperties props;

    public MinioService(MinioClient client, MinioProperties props) {
        this.client = client;
        this.props = props;
    }

    public String imageryBucket() { return props.getBucket().getImagery(); }
    public String evidenceBucket() { return props.getBucket().getEvidence(); }

    /** 上传文件，返回对象名 */
    public String upload(String bucket, String prefix, MultipartFile file) {
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String objectName = prefix + "/" + UUID.randomUUID() + "-" + original;
        try (InputStream in = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                    .build());
            return objectName;
        } catch (Exception e) {
            throw new BizException("文件上传失败: " + e.getMessage());
        }
    }

    /** 生成预签名下载 URL（供 AI 服务拉取影像 / 前端预览） */
    public String presignedGet(String bucket, String objectName, int expireMinutes) {
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectName)
                    .expiry(expireMinutes, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            throw new BizException("生成下载链接失败: " + e.getMessage());
        }
    }
}
