package com.example.devprojects.dto;

import com.example.devprojects.model.Project;
import com.example.devprojects.model.ProjectRole;
import com.example.devprojects.model.ProjectSkill;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@Slf4j
public class ProjectPreviewDto {

    private Integer id;
    private String title;
    private String description;
    private String shortDescription;
    private Project.ProjectStatus status;
    private LocalDate deadline;
    private Integer viewsCount;
    private Integer applicationsCount;
    private String authorName;
    private LocalDateTime createdAt;
    private List<RoleDto> roles;
    private List<SkillDto> skills;
    private long daysLeft;
    private int totalVacancies;
    private int filledVacancies;

    @Data
    @Builder
    public static class RoleDto {
        private String title;
        private String specialization;
        private int vacancies;
        private int filled;
        private boolean isOpen;
    }

    @Data
    @Builder
    public static class SkillDto {
        private String name;
        private boolean isRequired;
    }

    public static ProjectPreviewDto fromProject(Project project) {
        log.debug("Преобразование проекта ID {} в DTO", project.getId());

        List<RoleDto> roleDtos = Collections.emptyList();
        try {
            if (project.getRoles() != null && !project.getRoles().isEmpty()) {
                roleDtos = project.getRoles().stream()
                        .map(role -> RoleDto.builder()
                                .title(role.getTitle() != null ? role.getTitle() : role.getSpecializationName())
                                .specialization(role.getSpecializationName())
                                .vacancies(role.getVacanciesCount() != null ? role.getVacanciesCount() : 1)
                                .filled(role.getFilledCount() != null ? role.getFilledCount() : 0)
                                .isOpen(role.isOpen())
                                .build())
                        .collect(Collectors.toList());
                log.debug("Проект {} имеет {} ролей", project.getId(), roleDtos.size());
            }
        } catch (Exception e) {
            log.error("Ошибка при преобразовании ролей проекта {}", project.getId(), e);
            roleDtos = Collections.emptyList();
        }

        List<SkillDto> skillDtos = Collections.emptyList();
        try {
            if (project.getRequiredSkills() != null && !project.getRequiredSkills().isEmpty()) {
                log.debug("Проект {} имеет {} навыков", project.getId(), project.getRequiredSkills().size());

                skillDtos = project.getRequiredSkills().stream()
                        .map(skill -> {
                            log.debug("  Навык: {}, обязательный: {}",
                                    skill.getSkill().getName(),
                                    skill.getIsRequired());
                            return SkillDto.builder()
                                    .name(skill.getSkill().getName())
                                    .isRequired(skill.getIsRequired() != null ? skill.getIsRequired() : true)
                                    .build();
                        })
                        .collect(Collectors.toList());
            } else {
                log.debug("Проект {} не имеет навыков", project.getId());
            }
        } catch (Exception e) {
            log.error("Ошибка при преобразовании навыков проекта {}", project.getId(), e);
            skillDtos = Collections.emptyList();
        }

        long daysLeft = 0;
        try {
            if (project.getDeadline() != null) {
                daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), project.getDeadline());
            }
        } catch (Exception e) {
            log.error("Ошибка при расчете дней до дедлайна проекта {}", project.getId(), e);
            daysLeft = 0;
        }

        int totalVacancies = 0;
        int filledVacancies = 0;
        try {
            if (project.getRoles() != null) {
                totalVacancies = project.getRoles().stream()
                        .mapToInt(r -> r.getVacanciesCount() != null ? r.getVacanciesCount() : 0)
                        .sum();
                filledVacancies = project.getRoles().stream()
                        .mapToInt(r -> r.getFilledCount() != null ? r.getFilledCount() : 0)
                        .sum();
            }
        } catch (Exception e) {
            log.error("Ошибка при подсчете вакансий проекта {}", project.getId(), e);
            totalVacancies = 0;
            filledVacancies = 0;
        }

        ProjectPreviewDto dto = ProjectPreviewDto.builder()
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

        log.debug("DTO проекта {} создан: ролей {}, навыков {}",
                project.getId(),
                dto.getRoles() != null ? dto.getRoles().size() : 0,
                dto.getSkills() != null ? dto.getSkills().size() : 0);

        return dto;
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