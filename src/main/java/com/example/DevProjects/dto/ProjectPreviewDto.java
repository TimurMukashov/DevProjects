package com.example.devprojects.dto;

import com.example.devprojects.model.Project;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Builder
public record ProjectPreviewDto(
        Integer id,
        String title,
        String description,
        String shortDescription,
        String statusText,
        String statusColor,
        Integer viewsCount,
        Integer applicationsCount,
        Integer authorId, // Добавлено поле ID автора
        String authorName,
        String authorEmail,
        LocalDateTime createdAt,
        LocalDate deadline,
        List<RoleDto> roles,
        List<SkillDto> skills,
        long daysLeft,
        int totalVacancies,
        int filledVacancies
) {
    @Builder
    public record RoleDto(
            Integer id,
            String specialization,
            int vacancies,
            int filled,
            String title,
            String proficiencyName,
            boolean isOpen
    ) {}

    public record SkillDto(
            String name,
            @JsonProperty("required") boolean isRequired
    ) {}

    public static ProjectPreviewDto fromProject(Project project) {
        List<RoleDto> roleDtos = project.getRoles().stream()
                .map(r -> RoleDto.builder()
                        .id(r.getId())
                        .specialization(r.getSpecialization().getName())
                        .vacancies(r.getVacanciesCount())
                        .filled(r.getFilledCount())
                        .title(r.getProficiencyLevel() != null ? r.getProficiencyLevel().getDisplayName() : "")
                        .proficiencyName(r.getProficiencyLevel() != null ? r.getProficiencyLevel().getDisplayName() : "")
                        .isOpen(r.getFilledCount() < r.getVacanciesCount())
                        .build())
                .toList();

        List<SkillDto> skillDtos = project.getRequiredSkills().stream()
                .map(s -> new SkillDto(s.getSkill().getName(), s.getIsRequired()))
                .toList();

        return ProjectPreviewDto.builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .shortDescription(project.getDescription().length() > 120 ? project.getDescription().substring(0, 117) + "..." : project.getDescription())
                .statusText(mapStatus(project.getStatus()))
                .statusColor(mapColor(project.getStatus()))
                .viewsCount(project.getViewsCount())
                .applicationsCount(project.getApplicationsCount())
                .authorId(project.getAuthor().getId()) // Маппинг ID автора
                .authorName(project.getAuthorFullName())
                .authorEmail(project.getAuthor().getEmail())
                .createdAt(project.getCreatedAt())
                .deadline(project.getDeadline())
                .roles(roleDtos)
                .skills(skillDtos)
                .daysLeft(project.getDeadline() != null ? ChronoUnit.DAYS.between(LocalDate.now(), project.getDeadline()) : 0)
                .totalVacancies(project.getRoles().stream().mapToInt(r -> r.getVacanciesCount()).sum())
                .filledVacancies(project.getRoles().stream().mapToInt(r -> r.getFilledCount()).sum())
                .build();
    }

    private static String mapStatus(Project.ProjectStatus s) {
        return switch (s) { case open -> "Открыт"; case in_progress -> "В работе"; case completed -> "Завершён"; default -> "Черновик"; };
    }
    private static String mapColor(Project.ProjectStatus s) {
        return switch (s) { case open -> "success"; case in_progress -> "primary"; default -> "secondary"; };
    }
}