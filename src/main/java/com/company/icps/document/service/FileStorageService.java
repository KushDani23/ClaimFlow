package com.company.icps.document.service;

import com.company.icps.common.exception.FileUploadException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * Create the upload directory on application startup if it doesn't exist.
     */
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new FileUploadException("Could not create upload directory", e);
        }
    }

    /**
     * Store a file on disk with a UUID-based name to prevent collisions.
     * Returns the stored file name.
     */
    public String storeFile(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new FileUploadException("File name is invalid");
        }

        // Generate unique file name: UUID + original extension
        String extension = getFileExtension(originalFileName);
        String storedFileName = UUID.randomUUID().toString() + extension;

        try {
            Path targetLocation = uploadDir.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return storedFileName;
        } catch (IOException e) {
            throw new FileUploadException("Failed to store file: " + originalFileName, e);
        }
    }

    /**
     * Load a file as a Spring Resource for download.
     */
    public Resource loadFile(String storedFileName) {
        try {
            Path filePath = uploadDir.resolve(storedFileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileUploadException("File not found: " + storedFileName);
            }
        } catch (MalformedURLException e) {
            throw new FileUploadException("File not found: " + storedFileName, e);
        }
    }

    /**
     * Delete a file from disk.
     */
    public void deleteFile(String storedFileName) {
        try {
            Path filePath = uploadDir.resolve(storedFileName).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new FileUploadException("Failed to delete file: " + storedFileName, e);
        }
    }

    /**
     * Get the full path for a stored file.
     */
    public String getFilePath(String storedFileName) {
        return uploadDir.resolve(storedFileName).toString();
    }

    /**
     * Extract file extension from the original file name.
     */
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(dotIndex);
        }
        return "";
    }
}
