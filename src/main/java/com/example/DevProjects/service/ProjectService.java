package com.example.devprojects.service;

import com.example.devprojects.dto.ProjectPreviewDto;
import com.example.devprojects.model.Project;
import com.example.devprojects.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public Page<ProjectPreviewDto> getOpenProjects(int page, int size) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        Page<Project> projectsWithRoles =
                projectRepository.findByStatusWithRoles(Project.ProjectStatus.open, pageable);

        List<Integer> projectIds = projectsWithRoles.stream()
                .map(Project::getId)
                .toList();

        List<Project> projectsWithSkills =
                projectRepository.findProjectsWithSkillsByIds(projectIds);

        Map<Integer, Project> skillsMap = projectsWithSkills.stream()
                .collect(Collectors.toMap(Project::getId, Function.identity()));

        List<Project> combinedProjects = projectsWithRoles.stream()
                .map(project -> {
                    Project projectWithSkills = skillsMap.get(project.getId());
                    if (projectWithSkills != null && projectWithSkills.getRequiredSkills() != null) {
                        project.setRequiredSkills(projectWithSkills.getRequiredSkills());
                    }
                    return project;
                })
                .toList();

        return new PageImpl<>(combinedProjects, pageable, projectsWithRoles.getTotalElements())
                .map(ProjectPreviewDto::fromProject);
    }

    @Transactional
    public ProjectPreviewDto getProjectById(Integer id) {

        Project project = projectRepository
                .findByIdWithRoles(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        List<Project> projectsWithSkills =
                projectRepository.findProjectsWithSkillsByIds(List.of(id));

        if (!projectsWithSkills.isEmpty()) {
            project.setRequiredSkills(
                    projectsWithSkills.get(0).getRequiredSkills()
            );
        }

        project.setViewsCount(
                project.getViewsCount() != null ? project.getViewsCount() + 1 : 1
        );

        return ProjectPreviewDto.fromProject(project);
    }

    @Transactional(readOnly = true)
    public Page<ProjectPreviewDto> search(String query, int page, int size) {

        if (query == null || query.isBlank())
            return Page.empty();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        return projectRepository
                .search(query.trim().toLowerCase(), pageable)
                .map(ProjectPreviewDto::fromProject);
    }

    @Transactional(readOnly = true)
    public List<ProjectPreviewDto> liveSearch(String query) {

        if (query == null || query.trim().length() < 2)
            return List.of();

        List<Project> projects =
                projectRepository.liveSearchWithSkills(query.trim().toLowerCase());

        if (projects.isEmpty())
            return List.of();

        List<Integer> projectIds = projects.stream()
                .map(Project::getId)
                .toList();

        List<Project> projectsWithSkills =
                projectRepository.findProjectsWithSkillsByIds(projectIds);

        Map<Integer, Project> skillsMap = projectsWithSkills.stream()
                .collect(Collectors.toMap(Project::getId, Function.identity()));

        List<Project> combinedProjects = projects.stream()
                .map(project -> {
                    Project projectWithSkills = skillsMap.get(project.getId());
                    if (projectWithSkills != null && projectWithSkills.getRequiredSkills() != null)
                        project.setRequiredSkills(projectWithSkills.getRequiredSkills());
                    return project;
                })
                .toList();

        return combinedProjects.stream()
                .map(ProjectPreviewDto::fromProject)
                .toList();
    }
}