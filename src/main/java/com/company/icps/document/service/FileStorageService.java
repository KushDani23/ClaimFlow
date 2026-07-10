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

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new FileUploadException("Could not create upload directory", e);
        }
    }

    public String storeFile(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new FileUploadException("File name is invalid");
        }

        String extension = originalFileName.substring(originalFileName.lastIndexOf('.'));
        String storedFileName = UUID.randomUUID() + extension;

        try {
            Files.copy(file.getInputStream(), uploadDir.resolve(storedFileName), StandardCopyOption.REPLACE_EXISTING);
            return storedFileName;
        } catch (IOException e) {
            throw new FileUploadException("Failed to store file: " + originalFileName, e);
        }
    }

    public Resource loadFile(String storedFileName) {
        try {
            Resource resource = new UrlResource(uploadDir.resolve(storedFileName).normalize().toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new FileUploadException("File not found: " + storedFileName);
        } catch (MalformedURLException e) {
            throw new FileUploadException("File not found: " + storedFileName, e);
        }
    }

    public void deleteFile(String storedFileName) {
        try {
            Files.deleteIfExists(uploadDir.resolve(storedFileName).normalize());
        } catch (IOException e) {
            throw new FileUploadException("Failed to delete file: " + storedFileName, e);
        }
    }

    public String getFilePath(String storedFileName) {
        return uploadDir.resolve(storedFileName).toString();
    }
}
