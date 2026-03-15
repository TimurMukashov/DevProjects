package com.example.devprojects.controller;

import com.example.devprojects.dto.ProfileEditDto;
import com.example.devprojects.dto.ProjectPreviewDto;
import com.example.devprojects.model.User;
import com.example.devprojects.security.CustomUserDetails;
import com.example.devprojects.service.FileService; // Добавлено
import com.example.devprojects.service.UserService;
import com.example.devprojects.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dashboard")
@Slf4j
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;
    private final ProjectService projectService;
    private final FileService fileService; // Добавлено

    @GetMapping
    public String dashboard(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        User user = userService.getUserWithAllData(currentUser.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("fullName", user.getFullName());
        model.addAttribute("roleName", user.getRole() != null ? user.getRole().getName() : "Пользователь");

        model.addAttribute("myProjectsCount", user.getProjects() != null ? user.getProjects().size() : 0);
        model.addAttribute("myApplicationsCount", user.getApplications() != null ? user.getApplications().size() : 0);
        model.addAttribute("favoritesCount", user.getFavorites() != null ? user.getFavorites().size() : 0);

        return "dashboard/index";
    }

    @GetMapping("/projects")
    public String myProjects(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        User user = userService.getUserWithAllData(currentUser.getUsername());

        List<ProjectPreviewDto> myProjects = projectService.getProjectsByAuthor(currentUser.getUsername());

        model.addAttribute("user", user);
        model.addAttribute("projects", myProjects);
        model.addAttribute("roleName", user.getRole() != null ? user.getRole().getName() : "Пользователь");
        return "dashboard/projects";
    }

    @PostMapping("/projects/{id}/close")
    public String closeProject(@PathVariable Integer id,
                               @AuthenticationPrincipal CustomUserDetails currentUser,
                               RedirectAttributes redirectAttributes) {
        try {
            projectService.closeProject(id, currentUser.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Проект успешно завершен");
        } catch (Exception e) {
            log.error("Ошибка при закрытии проекта", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Не удалось закрыть проект: " + e.getMessage());
        }
        return "redirect:/dashboard/projects";
    }

    @GetMapping("/favorites")
    public String favorites(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        User user = userService.getUserWithAllData(currentUser.getUsername());

        List<ProjectPreviewDto> favoriteProjects = user.getFavorites().stream()
                .map(f -> ProjectPreviewDto.fromProject(f.getProject()))
                .collect(Collectors.toList());

        model.addAttribute("user", user);
        model.addAttribute("projects", favoriteProjects);
        model.addAttribute("roleName", user.getRole() != null ? user.getRole().getName() : "Пользователь");
        return "dashboard/favorites";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        User user = userService.getUserWithAllData(currentUser.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("roleName", user.getRole() != null ? user.getRole().getName() : "Пользователь");
        return "dashboard/profile";
    }

    @GetMapping("/edit")
    public String editProfile(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        User user = userService.getUserWithAllData(currentUser.getUsername());
        model.addAttribute("user", user);

        ProfileEditDto profileEditDto = new ProfileEditDto(
                user.getFirstName(),
                user.getLastName(),
                user.getBio()
        );
        model.addAttribute("profileEditDto", profileEditDto);
        return "dashboard/edit";
    }

    @PostMapping("/edit")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails currentUser,
                                @ModelAttribute ProfileEditDto profileEditDto,
                                @RequestParam(value = "avatar", required = false) MultipartFile avatar,
                                RedirectAttributes redirectAttributes) {
        try {
            String avatarUrl = null;
            // Сохраняем файл через FileService, если он передан
            if (avatar != null && !avatar.isEmpty()) {
                avatarUrl = fileService.saveAvatar(avatar);
            }

            // Вызываем обновленный UserService, передавая путь к файлу (строку)
            userService.updateProfile(currentUser.getUser().getId(), profileEditDto, avatarUrl);

            redirectAttributes.addFlashAttribute("successMessage", "Профиль успешно обновлен");
        } catch (Exception e) {
            log.error("Ошибка обновления профиля", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при обновлении данных: " + e.getMessage());
            return "redirect:/dashboard/edit";
        }
        return "redirect:/dashboard/profile";
    }
}