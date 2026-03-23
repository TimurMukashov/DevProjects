package com.example.devprojects.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

public record ProjectEditDto(
        @NotNull Integer id,
        @NotBlank String title,
        @NotBlank String description,
        @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate deadline,
        List<RoleDto> roles,
        List<SkillDto> skills,
        String authorEmail
) {
    public ProjectEditDto {
        if (roles == null) roles = new ArrayList<>();
        if (skills == null) skills = new ArrayList<>();
    }

    public record RoleDto(
            Integer id,
            @NotNull Integer specializationId,
            @NotNull Integer proficiencyLevelId,
            String description,
            @Min(1) Integer vacanciesCount
    ) {}

    public record SkillDto(
            Integer id,
            @NotNull Integer skillId,
            boolean required
    ) {}
}