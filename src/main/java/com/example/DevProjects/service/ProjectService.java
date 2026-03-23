package com.example.devprojects.service;

import com.example.devprojects.dto.ProjectCreateDto;
import com.example.devprojects.dto.ProjectEditDto;
import com.example.devprojects.dto.ProjectPreviewDto;
import com.example.devprojects.model.*;
import com.example.devprojects.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final SkillRepository skillRepository;
    private final SpecializationRepository specializationRepository;
    private final UserRepository userRepository;
    private final ProjectRoleRepository projectRoleRepository;
    private final ProjectSkillRepository projectSkillRepository;
    private final ProficiencyLevelRepository proficiencyLevelRepository; // ДОБАВЛЕНО

    @Transactional(readOnly = true)
    public Page<ProjectPreviewDto> getOpenProjects(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return projectRepository.findByStatusWithDetails(Project.ProjectStatus.open, pageable)
                .map(ProjectPreviewDto::fromProject);
    }

    @Transactional
    public ProjectPreviewDto getProjectById(Integer id) {
        Project project = projectRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        project.setViewsCount(project.getViewsCount() != null ? project.getViewsCount() + 1 : 1);
        return ProjectPreviewDto.fromProject(project);
    }

    @Transactional(readOnly = true)
    public Page<ProjectPreviewDto> search(String query, int page, int size) {
        if (query == null || query.isBlank()) return Page.empty();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return projectRepository.search(query.trim().toLowerCase(), pageable)
                .map(ProjectPreviewDto::fromProject);
    }

    @Transactional(readOnly = true)
    public List<ProjectPreviewDto> liveSearch(String query) {
        if (query == null || query.trim().length() < 2) return List.of();
        return projectRepository.liveSearchWithDetails(query.trim().toLowerCase())
                .stream()
                .map(ProjectPreviewDto::fromProject)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectPreviewDto> getProjectsByAuthor(String email) {
        return projectRepository.findAllByAuthorEmail(email).stream()
                .map(ProjectPreviewDto::fromProject)
                .toList();
    }

    @Transactional
    public Integer createProject(ProjectCreateDto dto, String authorEmail) {
        User author = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new RuntimeException("Автор не найден"));

        Project project = Project.builder()
                .title(dto.title())
                .description(dto.description())
                .deadline(dto.deadline())
                .author(author)
                .status(Project.ProjectStatus.open)
                .build();

        if (dto.roles() != null) {
            project.setRoles(dto.roles().stream().map(roleDto -> {
                Specialization spec = specializationRepository.findById(roleDto.specializationId())
                        .orElseThrow(() -> new RuntimeException("Специализация не найдена"));

                ProficiencyLevel level = proficiencyLevelRepository.findById(roleDto.proficiencyLevelId())
                        .orElseThrow(() -> new RuntimeException("Уровень владения не найден"));

                return ProjectRole.builder()
                        .project(project)
                        .specialization(spec)
                        .proficiencyLevel(level)
                        .description(roleDto.description())
                        .vacanciesCount(roleDto.vacanciesCount())
                        .build();
            }).collect(Collectors.toSet()));
        }

        if (dto.skills() != null) {
            project.setRequiredSkills(dto.skills().stream().map(skillDto -> {
                Skill skill = skillRepository.findById(skillDto.skillId())
                        .orElseThrow(() -> new RuntimeException("Навык не найден"));
                return ProjectSkill.builder()
                        .project(project)
                        .skill(skill)
                        .isRequired(skillDto.required())
                        .build();
            }).collect(Collectors.toSet()));
        }

        return projectRepository.save(project).getId();
    }

    @Transactional
    public void closeProject(Integer projectId, String userEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Проект не найден"));

        if (!project.getAuthor().getEmail().equals(userEmail))
            throw new RuntimeException("У вас нет прав для изменения этого проекта");

        project.setStatus(Project.ProjectStatus.completed);
    }

    @Transactional(readOnly = true)
    public ProjectEditDto getProjectForEdit(Integer id) {
        Project project = projectRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Проект не найден"));

        List<ProjectEditDto.RoleDto> roleDtos = project.getRoles().stream()
                .map(r -> new ProjectEditDto.RoleDto(
                        r.getId(),
                        r.getSpecialization().getId(),
                        r.getProficiencyLevel() != null ? r.getProficiencyLevel().getId() : null, // ЗАМЕНЕНО: r.getTitle()
                        r.getDescription(),
                        r.getVacanciesCount()
                ))
                .collect(Collectors.toList());

        List<ProjectEditDto.SkillDto> skillDtos = project.getRequiredSkills().stream()
                .map(s -> new ProjectEditDto.SkillDto(
                        s.getId(),
                        s.getSkill().getId(),
                        s.getIsRequired()
                ))
                .collect(Collectors.toList());

        return new ProjectEditDto(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getDeadline(),
                roleDtos,
                skillDtos,
                project.getAuthor().getEmail()
        );
    }

    @Transactional
    public void updateProject(ProjectEditDto dto, String authorEmail) {
        Project project = projectRepository.findByIdWithDetails(dto.id())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getAuthor().getEmail().equals(authorEmail)) {
            throw new RuntimeException("Нет прав на редактирование");
        }

        project.setTitle(dto.title());
        project.setDescription(dto.description());
        project.setDeadline(dto.deadline());
        updateRoles(project, dto.roles());
        updateSkills(project, dto.skills());

        projectRepository.save(project);
    }

    private void updateRoles(Project project, List<ProjectEditDto.RoleDto> roleDtos) {
        if (roleDtos == null) return;

        Map<Integer, ProjectRole> existingRoles = project.getRoles().stream()
                .filter(r -> r.getId() != null)
                .collect(Collectors.toMap(ProjectRole::getId, Function.identity()));

        Set<Integer> rolesFromDtoIds = roleDtos.stream()
                .map(ProjectEditDto.RoleDto::id)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        List<ProjectRole> rolesToRemove = existingRoles.values().stream()
                .filter(r -> !rolesFromDtoIds.contains(r.getId()))
                .toList();
        project.getRoles().removeAll(rolesToRemove);
        projectRoleRepository.deleteAll(rolesToRemove);

        for (ProjectEditDto.RoleDto rd : roleDtos) {
            Specialization spec = specializationRepository.findById(rd.specializationId())
                    .orElseThrow(() -> new RuntimeException("Специализация не найдена"));

            ProficiencyLevel level = proficiencyLevelRepository.findById(rd.proficiencyLevelId())
                    .orElseThrow(() -> new RuntimeException("Уровень не найден"));

            if (rd.id() != null && existingRoles.containsKey(rd.id())) {
                ProjectRole r = existingRoles.get(rd.id());
                r.setSpecialization(spec);
                r.setProficiencyLevel(level);
                r.setDescription(rd.description());
                r.setVacanciesCount(rd.vacanciesCount());
            } else {
                project.getRoles().add(ProjectRole.builder()
                        .project(project)
                        .specialization(spec)
                        .proficiencyLevel(level)
                        .description(rd.description())
                        .vacanciesCount(rd.vacanciesCount())
                        .build());
            }
        }
    }

    private void updateSkills(Project project, List<ProjectEditDto.SkillDto> skillDtos) {
        if (skillDtos == null) return;

        Map<Integer, ProjectSkill> existingSkills = project.getRequiredSkills().stream()
                .filter(s -> s.getId() != null)
                .collect(Collectors.toMap(ProjectSkill::getId, Function.identity()));

        Set<Integer> skillsFromDtoIds = skillDtos.stream()
                .map(ProjectEditDto.SkillDto::id)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        List<ProjectSkill> skillsToRemove = existingSkills.values().stream()
                .filter(s -> !skillsFromDtoIds.contains(s.getId()))
                .toList();
        project.getRequiredSkills().removeAll(skillsToRemove);
        projectSkillRepository.deleteAll(skillsToRemove);

        for (ProjectEditDto.SkillDto sd : skillDtos) {
            Skill skill = skillRepository.findById(sd.skillId())
                    .orElseThrow(() -> new RuntimeException("Навык не найден"));

            if (sd.id() != null && existingSkills.containsKey(sd.id())) {
                ProjectSkill s = existingSkills.get(sd.id());
                s.setSkill(skill);
                s.setIsRequired(sd.required());
            } else {
                boolean alreadyExists = project.getRequiredSkills().stream()
                        .anyMatch(ps -> ps.getSkill().getId().equals(skill.getId()));

                if (!alreadyExists) {
                    project.getRequiredSkills().add(ProjectSkill.builder()
                            .project(project)
                            .skill(skill)
                            .isRequired(sd.required())
                            .build());
                }
            }
        }
    }
}