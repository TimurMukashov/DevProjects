package com.example.devprojects.dto;

import com.example.devprojects.model.Project;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Slf4j
public record ProjectPreviewDto(
        Integer id,
        String title,
        String description,
        String shortDescription,
        Project.ProjectStatus status,
        LocalDate deadline,
        Integer viewsCount,
        Integer applicationsCount,
        String authorName,
        LocalDateTime createdAt,
        List<RoleDto> roles,
        List<SkillDto> skills,
        long daysLeft,
        int totalVacancies,
        int filledVacancies
) {
    @Builder
    public record RoleDto(
            String title,
            String specialization,
            int vacancies,
            int filled,
            boolean isOpen
    ) {}

    @Builder
    public record SkillDto(
            String name,
            boolean isRequired
    ) {}

    // Твоя логика преобразования (в будущем переедет в MapStruct)
    public static ProjectPreviewDto fromProject(Project project) {
        log.debug("Преобразование проекта ID {} в DTO", project.getId());

        List<RoleDto> roleDtos = (project.getRoles() == null) ? Collections.emptyList() :
                project.getRoles().stream()
                        .map(role -> RoleDto.builder()
                                .title(role.getTitle() != null ? role.getTitle() : role.getSpecializationName())
                                .specialization(role.getSpecializationName())
                                .vacancies(role.getVacanciesCount() != null ? role.getVacanciesCount() : 1)
                                .filled(role.getFilledCount() != null ? role.getFilledCount() : 0)
                                .isOpen(role.isOpen())
                                .build())
                        .collect(Collectors.toList());

        List<SkillDto> skillDtos = (project.getRequiredSkills() == null) ? Collections.emptyList() :
                project.getRequiredSkills().stream()
                        .map(skill -> SkillDto.builder()
                                .name(skill.getSkill().getName())
                                .isRequired(skill.getIsRequired() != null ? skill.getIsRequired() : true)
                                .build())
                        .collect(Collectors.toList());

        long daysLeft = project.getDeadline() != null ?
                ChronoUnit.DAYS.between(LocalDate.now(), project.getDeadline()) : 0;

        int totalVacancies = project.getRoles() != null ?
                project.getRoles().stream().mapToInt(r -> r.getVacanciesCount() != null ? r.getVacanciesCount() : 0).sum() : 0;
        int filledVacancies = project.getRoles() != null ?
                project.getRoles().stream().mapToInt(r -> r.getFilledCount() != null ? r.getFilledCount() : 0).sum() : 0;

        return ProjectPreviewDto.builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .shortDescription(truncate(project.getDescription(), 120))
                .status(project.getStatus())
                .deadline(project.getDeadline())
                .viewsCount(project.getViewsCount() != null ? project.getViewsCount() : 0)
                .applicationsCount(project.getApplicationsCount() != null ? project.getApplicationsCount() : 0)
                .authorName(project.getAuthorFullName())
                .createdAt(project.getCreatedAt())
                .roles(roleDtos)
                .skills(skillDtos)
                .daysLeft(daysLeft)
                .totalVacancies(totalVacancies)
                .filledVacancies(filledVacancies)
                .build();
    }

    private static String truncate(String str, int length) {
        if (str == null || str.length() <= length) return str;
        return str.substring(0, length) + "...";
    }

    public String getStatusColor() {
        if (status == null) return "secondary";
        return switch (status) {
            case open -> "success";
            case in_progress -> "primary";
            case completed -> "secondary";
            case draft -> "warning";
            case archived -> "dark";
        };
    }

    public String getStatusText() {
        if (status == null) return "Неизвестно";
        return switch (status) {
            case open -> "Открыт";
            case in_progress -> "В работе";
            case completed -> "Завершён";
            case draft -> "Черновик";
            case archived -> "Архив";
        };
    }
}