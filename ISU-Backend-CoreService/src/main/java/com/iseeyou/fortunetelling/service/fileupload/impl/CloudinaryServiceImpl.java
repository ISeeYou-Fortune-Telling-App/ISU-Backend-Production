package com.iseeyou.fortunetelling.service.fileupload.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.iseeyou.fortunetelling.service.fileupload.CloudinaryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    @Transactional
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        // Determine resource type based on content type
        String resourceType = getResourceType(file);

        return cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", resourceType))
                .get("secure_url").toString();
    }

    private static String getResourceType(MultipartFile file) {
        String contentType = file.getContentType();
        String resourceType = "auto"; // Default to auto-detect

        if (contentType != null) {
            if (contentType.startsWith("video/")) {
                resourceType = "video";
            } else if (contentType.startsWith("image/")) {
                resourceType = "image";
            } else if (contentType.startsWith("audio/")) {
                resourceType = "raw";
            } else if (contentType.startsWith("application/")) {
                // Handle documents: PDF, Word, Excel, etc.
                resourceType = "raw";
            }
        }
        return resourceType;
    }

    @Override
    @Transactional
    public void uploadMultipleFiles(MultipartFile[] files, String folder) throws IOException {
        // Sử dụng ExecutorService để xử lý song song
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<?>> futures = new ArrayList<>();

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                Future<?> future = executor.submit(() -> {
                    try {
                        uploadFile(file, folder);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to upload file: " + file.getOriginalFilename(), e);
                    }
                });
                futures.add(future);
            }
        }

        // Đợi tất cả các task hoàn thành
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Upload process was interrupted", e);
            }
        }

        executor.shutdown();
    }

    @Override
    @Transactional
    public void deleteFile(String url) throws IOException {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        int uploadIndex = url.indexOf("/upload/");
        if (uploadIndex == -1) {
            throw new IllegalArgumentException("Invalid Cloudinary URL format");
        }

        String publicId = getString(url, uploadIndex);

        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    private static String getString(String url, int uploadIndex) {
        String afterUpload = url.substring(uploadIndex + 8);

        if (afterUpload.startsWith("v")) {
            int versionEndIndex = afterUpload.indexOf("/");
            if (versionEndIndex != -1) {
                afterUpload = afterUpload.substring(versionEndIndex + 1);
            }
        }

        int extensionIndex = afterUpload.lastIndexOf(".");
        if (extensionIndex != -1) {
            afterUpload = afterUpload.substring(0, extensionIndex);
        }

        return afterUpload;
    }
}
