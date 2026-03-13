package com.example.devprojects.controller;

import com.example.devprojects.dto.ProjectPreviewDto;
import com.example.devprojects.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/{id}")
    public String viewProject(@PathVariable Integer id, Model model) {
        log.info("Запрос на просмотр проекта с ID: {}", id);

        try {
            ProjectPreviewDto project = projectService.getProjectById(id);
            model.addAttribute("project", project);
            return "projects/view";
        } catch (RuntimeException e) {
            log.error("Проект не найден: {}", id);
            return "redirect:/?notfound";
        }
    }
}