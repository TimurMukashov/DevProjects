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

        // Используем оптимизированный запрос для предотвращения N+1
        return projectRepository.findAll(pageable)
                .map(ProjectPreviewDto::fromProject);
    }

    @Transactional
    public ProjectPreviewDto getProjectById(Integer id) {

        Project project = projectRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

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

        return projects.stream()
                .map(ProjectPreviewDto::fromProject)
                .toList();
    }
}