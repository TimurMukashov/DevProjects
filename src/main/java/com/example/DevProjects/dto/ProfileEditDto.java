package com.example.devprojects.dto;

import org.springframework.web.multipart.MultipartFile;

public record ProfileEditDto(
        String firstName,
        String lastName,
        String bio
) {}