package com.toolcnc.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/upload")
@CrossOrigin(origins = "*")
public class FileUploadController {

    private final String uploadDir = "uploads/products/";

    @PostMapping("/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";

        String newFilename = UUID.randomUUID() + ext;

        try {
            Path dirPath = Paths.get(uploadDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            Path filePath = dirPath.resolve(newFilename);
            file.transferTo(filePath.toAbsolutePath().toFile());

            String fileUrl = "/uploads/products/" + newFilename;
            return ResponseEntity.ok(Map.of("url", fileUrl, "filename", newFilename));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Could not save file: " + e.getMessage());
        }
    }
}
