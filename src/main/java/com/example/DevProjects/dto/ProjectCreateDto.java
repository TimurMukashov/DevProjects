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
    public ProjectCreateDto {
        if (roles == null) roles = new ArrayList<>();
        if (skills == null) skills = new ArrayList<>();
    }

    public record RoleDto(
            @NotNull(message = "Специализация обязательна")
            Integer specializationId,

            @NotNull(message = "Уровень владения обязателен")
            Integer proficiencyLevelId,

            String description,

            @Min(value = 1, message = "Количество мест должно быть не менее 1")
            Integer vacanciesCount
    ) {}

    public record SkillDto(
            @NotNull Integer skillId,
            boolean required
    ) {}
}