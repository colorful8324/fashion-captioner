package com.fashionai.captioning.fashion_captioner.service;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
public class MinioService {

    private final MinioClient minioClient;
    @Value("${minio.bucket}")
    private String bucket;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public void uploadFile(String filename, InputStream stream, String contentType) throws Exception {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .object(filename)
                .stream(stream, -1, 10485760) // -1 for unknown size, 10MB max part
                .contentType(contentType)
                .build()
        );
    }

    public InputStream downloadFile(String filename) throws Exception {
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucket)
                .object(filename)
                .build()
        );
    }

    public String getObjectUrl(String objectName) throws Exception {
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(objectName)
                .expiry(1, TimeUnit.HOURS) // Link valid for 1 hour
                .build()
        );
    }
}
