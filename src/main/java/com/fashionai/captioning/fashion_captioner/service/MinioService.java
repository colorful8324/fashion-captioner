package com.fashionai.captioning.fashion_captioner.service;

import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    @Value("${minio.bucket}")
    private String bucket;

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

    public boolean objectExistsByName(String objectName) {
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build()
            );
            return true;
        } catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            return false;
        }
    }

    public String getObjectUrl(String objectName) throws Exception {

        if (!objectExistsByName(objectName)) {
            throw new Exception("Đối tượng không tồn tại trong MinIO: " + objectName);
        }

        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(objectName)
                .build()
        );
    }
}
