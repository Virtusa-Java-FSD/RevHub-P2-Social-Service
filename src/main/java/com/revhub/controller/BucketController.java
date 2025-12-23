package com.revhub.controller;
import com.revhub.service.BucketService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class BucketController {
    private final BucketService bucketService;
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("=== UPLOAD REQUEST RECEIVED ===");
            System.out.println("File: " + file.getOriginalFilename());
            System.out.println("Size: " + file.getSize() + " bytes");
            System.out.println("Content-Type: " + file.getContentType());
            System.out.println("Bucket: " + bucketName);
            // Validate file
            if (file.isEmpty()) {
                System.out.println("ERROR: File is empty");
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }
            // Validate file size (10MB for images, 50MB for videos)
            long maxSize = file.getContentType() != null && file.getContentType().startsWith("video")
                    ? 50 * 1024 * 1024
                    : 10 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                System.out.println("ERROR: File size exceeds limit");
                return ResponseEntity.badRequest().body(Map.of(
                        "error",
                        "File size exceeds limit: " + (maxSize / (1024 * 1024)) + "MB"));
            }
            // Upload to S3
            System.out.println("Calling BucketService.uploadFile()...");
            String fileUrl = bucketService.uploadFile(file, bucketName);
            System.out.println("Upload successful! URL: " + fileUrl);
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (Exception e) {
            System.out.println("ERROR in upload: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    @PostMapping("/upload/multiple")
    public ResponseEntity<Map<String, Object>> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files) {
        try {
            List<String> urls = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            for (MultipartFile file : files) {
                try {
                    if (!file.isEmpty()) {
                        String fileUrl = bucketService.uploadFile(file, bucketName);
                        urls.add(fileUrl);
                    }
                } catch (Exception e) {
                    errors.add(file.getOriginalFilename() + ": " + e.getMessage());
                }
            }
            Map<String, Object> response = new HashMap<>();
            response.put("urls", urls);
            response.put("uploadedCount", urls.size());
            if (!errors.isEmpty()) {
                response.put("errors", errors);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, String>> deleteFile(@RequestParam("url") String fileUrl) {
        try {
            bucketService.deleteFile(fileUrl, bucketName);
            return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
