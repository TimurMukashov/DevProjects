package com.example.devprojects.controller;

import com.example.devprojects.dto.ProfileEditDto;
import com.example.devprojects.dto.ProjectPreviewDto;
import com.example.devprojects.model.Application;
import com.example.devprojects.model.Notification;
import com.example.devprojects.model.User;
import com.example.devprojects.security.CustomUserDetails;
import com.example.devprojects.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional; // Добавлено
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
    private final FileService fileService;
    private final ApplicationService applicationService;
    private final NotificationService notificationService;

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

    @GetMapping("/profile/{id}")
    public String viewPublicProfile(@PathVariable Integer id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        model.addAttribute("roleName", user.getRole() != null ? user.getRole().getName() : "Пользователь");
        model.addAttribute("isPublic", true);
        return "dashboard/profile";
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
    @Transactional(readOnly = true) // Добавлено для решения проблемы LazyInitializationException
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
        model.addAttribute("isPublic", false);
        return "dashboard/profile";
    }

    @GetMapping("/edit")
    public String editProfile(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        User user = userService.getUserWithAllData(currentUser.getUsername());
        model.addAttribute("user", user);
        ProfileEditDto profileEditDto = new ProfileEditDto(user.getFirstName(), user.getLastName(), user.getBio());
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
            if (avatar != null && !avatar.isEmpty()) avatarUrl = fileService.saveAvatar(avatar);
            userService.updateProfile(currentUser.getUser().getId(), profileEditDto, avatarUrl);
            redirectAttributes.addFlashAttribute("successMessage", "Профиль успешно обновлен");
        } catch (Exception e) {
            log.error("Ошибка обновления профиля", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при обновлении данных: " + e.getMessage());
            return "redirect:/dashboard/edit";
        }
        return "redirect:/dashboard/profile";
    }

    @GetMapping("/projects/{id}/applications")
    public String projectApplications(@PathVariable Integer id,
                                      @AuthenticationPrincipal CustomUserDetails currentUser,
                                      Model model) {
        User user = userService.getUserWithAllData(currentUser.getUsername());
        model.addAttribute("user", user);
        try {
            ProjectPreviewDto project = projectService.getProjectById(id);
            if (!project.authorEmail().equals(currentUser.getUsername())) {
                return "redirect:/dashboard/projects";
            }
            List<Application> applications = applicationService.getProjectApplications(id, user.getId());
            model.addAttribute("project", project);
            model.addAttribute("applications", applications);
            model.addAttribute("roleName", user.getRole() != null ? user.getRole().getName() : "Пользователь");
        } catch (Exception e) {
            log.error("Ошибка загрузки заявок", e);
            return "redirect:/dashboard/projects";
        }
        return "dashboard/applications";
    }

    @PostMapping("/applications/{id}/accept")
    public String acceptApplication(@PathVariable Integer id,
                                    @RequestParam Integer projectId,
                                    @AuthenticationPrincipal CustomUserDetails currentUser,
                                    RedirectAttributes redirectAttributes) {
        try {
            applicationService.acceptApplication(id, currentUser.getUser().getId());
            redirectAttributes.addFlashAttribute("successMessage", "Заявка успешно принята! Кандидат уведомлен.");
        } catch (Exception e) {
            log.error("Ошибка принятия заявки", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/dashboard/projects/" + projectId + "/applications";
    }

    @PostMapping("/applications/{id}/reject")
    public String rejectApplication(@PathVariable Integer id,
                                    @RequestParam Integer projectId,
                                    @AuthenticationPrincipal CustomUserDetails currentUser,
                                    RedirectAttributes redirectAttributes) {
        try {
            applicationService.rejectApplication(id, currentUser.getUser().getId());
            redirectAttributes.addFlashAttribute("successMessage", "Заявка отклонена. Кандидат уведомлен.");
        } catch (Exception e) {
            log.error("Ошибка отклонения заявки", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/dashboard/projects/" + projectId + "/applications";
    }

    @GetMapping("/my-applications")
    public String myApplications(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        User user = userService.getUserWithAllData(currentUser.getUsername());
        List<Application> applications = applicationService.getUserApplications(user.getId());
        model.addAttribute("user", user);
        model.addAttribute("applications", applications);
        model.addAttribute("roleName", user.getRole() != null ? user.getRole().getName() : "Пользователь");
        return "dashboard/my-applications";
    }

    @GetMapping("/notifications/{id}/click")
    public String handleNotificationClick(@PathVariable Integer id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        Notification notification = notificationService.getById(id);
        if (!notification.getRecipient().getId().equals(currentUser.getUser().getId())) {
            return "redirect:/dashboard";
        }
        notificationService.markAsRead(id);
        if ("application_received".equals(notification.getType())) {
            Application app = applicationService.getById(notification.getTargetId());
            return "redirect:/dashboard/projects/" + app.getProjectRole().getProject().getId() + "/applications";
        }
        if ("application_status_changed".equals(notification.getType())) {
            return "redirect:/dashboard/my-applications";
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/notifications/read")
    @ResponseBody
    public void markNotificationsAsRead(@AuthenticationPrincipal CustomUserDetails currentUser) {
        notificationService.markAllAsRead(currentUser.getUser().getId());
    }
}