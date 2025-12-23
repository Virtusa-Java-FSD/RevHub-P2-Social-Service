package com.revhub.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BucketService {
    private final AmazonS3 amazonS3;

    public String uploadFile(MultipartFile file, String bucketName) {
        try {
            log.info("Starting file upload - File: {}, Size: {} bytes, Type: {}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());
            // Generate unique file name
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String fileName = UUID.randomUUID().toString() + extension;
            log.info("Generated filename: {}", fileName);
            // Set metadata
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            log.info("Uploading to S3 bucket: {}", bucketName);
            // Upload to S3 (without ACL - bucket uses bucket-level permissions)
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName,
                    fileName,
                    file.getInputStream(),
                    metadata);
            amazonS3.putObject(putObjectRequest);
            // Return public URL
            String fileUrl = amazonS3.getUrl(bucketName, fileName).toString();
            log.info("File uploaded successfully: {}", fileUrl);
            return fileUrl;
        } catch (IOException e) {
            log.error("IOException while uploading file to S3", e);
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error uploading file to S3", e);
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage());
        }
    }

    public void deleteFile(String fileUrl, String bucketName) {
        try {
            // Extract file name from URL
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            amazonS3.deleteObject(bucketName, fileName);
            log.info("File deleted successfully: {}", fileName);
        } catch (Exception e) {
            log.error("Error deleting file from S3", e);
            throw new RuntimeException("Failed to delete file from S3: " + e.getMessage());
        }
    }
}
