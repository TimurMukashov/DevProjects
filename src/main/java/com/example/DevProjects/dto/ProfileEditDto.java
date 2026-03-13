package com.example.devprojects.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ProfileEditDto {
    private String firstName;
    private String lastName;
    private String bio;
    private MultipartFile avatar;
}