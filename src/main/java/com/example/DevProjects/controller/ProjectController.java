package com.example.devprojects.controller;

import com.example.devprojects.dto.ProjectCreateDto;
import com.example.devprojects.dto.ProjectEditDto;
import com.example.devprojects.dto.ProjectPreviewDto;
import com.example.devprojects.security.CustomUserDetails;
import com.example.devprojects.service.FavoriteService;
import com.example.devprojects.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;
    private final FavoriteService favoriteService;

    @GetMapping("/new")
    public String createProjectForm(Model model) {
        // ИСПРАВЛЕНО: Передаем null во все поля рекорда
        model.addAttribute("projectCreateDto", new ProjectCreateDto(null, null, null, null, null));
        return "projects/create";
    }

    @PostMapping("/new")
    public String createProject(@Valid @ModelAttribute("projectCreateDto") ProjectCreateDto dto,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal CustomUserDetails currentUser,
                                RedirectAttributes redirectAttributes,
                                Model model) { // ИСПРАВЛЕНО: Добавлен Model model

        if (bindingResult.hasErrors()) {
            return "projects/create";
        }

        try {
            Integer projectId = projectService.createProject(dto, currentUser.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Проект успешно создан!");
            return "redirect:/projects/" + projectId;
        } catch (Exception e) {
            log.error("Ошибка при создании проекта", e);
            model.addAttribute("errorMessage", "Ошибка при создании проекта: " + e.getMessage());
            return "projects/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editProjectForm(@PathVariable Integer id,
                                  @AuthenticationPrincipal CustomUserDetails currentUser,
                                  Model model) {
        ProjectEditDto projectDto = projectService.getProjectForEdit(id);

        if (!projectDto.authorEmail().equals(currentUser.getUsername())) {
            return "redirect:/projects/" + id;
        }

        model.addAttribute("projectEditDto", projectDto);
        return "projects/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateProject(@PathVariable Integer id,
                                @Valid @ModelAttribute("projectEditDto") ProjectEditDto dto,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal CustomUserDetails currentUser,
                                RedirectAttributes redirectAttributes,
                                Model model) {

        if (bindingResult.hasErrors()) {
            return "projects/edit";
        }

        try {
            projectService.updateProject(dto, currentUser.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Проект успешно обновлен!");
            return "redirect:/projects/" + id;
        } catch (Exception e) {
            log.error("Ошибка обновления проекта", e);
            model.addAttribute("errorMessage", "Ошибка: " + e.getMessage());
            return "projects/edit";
        }
    }

    @GetMapping("/{id}")
    public String getProject(@PathVariable Integer id,
                             @AuthenticationPrincipal CustomUserDetails currentUser,
                             Model model) {
        ProjectPreviewDto project = projectService.getProjectById(id);
        model.addAttribute("project", project);

        boolean isFavorite = false;
        if (currentUser != null) {
            isFavorite = favoriteService.isFavorite(currentUser.getUser(), id);
        }
        model.addAttribute("isFavorite", isFavorite);

        return "projects/view";
    }
}