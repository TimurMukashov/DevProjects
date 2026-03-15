package com.example.devprojects.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileService {

    @Value("${app.upload.avatar-path}")
    private String avatarPath;

    public String saveAvatar(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty())
            return null;

        Path uploadPath = Paths.get(avatarPath);
        if (!Files.exists(uploadPath))
            Files.createDirectories(uploadPath);

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/avatars/" + fileName;
    }
}