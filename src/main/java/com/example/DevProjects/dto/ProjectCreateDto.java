package com.example.devprojects.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record ProjectCreateDto(
        @NotBlank(message = "Название проекта обязательно")
        @Size(max = 255)
        String title,

        @NotBlank(message = "Описание проекта обязательно")
        String description,

        @FutureOrPresent(message = "Дедлайн не может быть в прошлом")
        LocalDate deadline,

        List<RoleDto> roles,
        List<SkillDto> skills
) {
    // Компактный конструктор для инициализации списков
    public ProjectCreateDto {
        if (roles == null) roles = new ArrayList<>();
        if (skills == null) skills = new ArrayList<>();
    }

    public record RoleDto(
            @NotNull Integer specializationId,
            String title,
            String description,
            @Min(1) Integer vacanciesCount
    ) {}

    public record SkillDto(
            @NotNull Integer skillId,
            boolean required
    ) {}
}