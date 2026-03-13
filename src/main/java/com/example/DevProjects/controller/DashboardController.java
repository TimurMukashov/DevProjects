package com.example.devprojects.controller;

import com.example.devprojects.dto.ProfileEditDto;
import com.example.devprojects.security.CustomUserDetails;
import com.example.devprojects.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dashboard")
@Slf4j
@RequiredArgsConstructor
public class DashboardController {

    @GetMapping
    public String dashboard(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        log.debug("Доступ к дашборду пользователя: {}", currentUser.getUsername());

        model.addAttribute("user", currentUser.getUser());
        model.addAttribute("fullName", currentUser.getFullName());

        String roleName = currentUser.getUser().getRole() != null
                ? currentUser.getUser().getRole().getName()
                : "Пользователь";
        model.addAttribute("roleName", roleName);

        return "dashboard/index";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        log.debug("Просмотр профиля пользователя: {}", currentUser.getUsername());

        model.addAttribute("user", currentUser.getUser());

        String roleName = currentUser.getUser().getRole() != null
                ? currentUser.getUser().getRole().getName()
                : "Пользователь";
        model.addAttribute("roleName", roleName);

        model.addAttribute("dateUtils", new DateUtils());

        return "dashboard/profile";
    }

    @GetMapping("/edit")
    public String editProfile(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        log.debug("Редактирование профиля пользователя: {}", currentUser.getUsername());

        model.addAttribute("user", currentUser.getUser());
        model.addAttribute("dateUtils", new DateUtils());

        return "dashboard/edit";
    }

    @PostMapping("/edit")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails currentUser,
                                @ModelAttribute ProfileEditDto profileEditDto,
                                RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute("successMessage", "Профиль успешно обновлен");
        } catch (Exception e) {
            log.error("Ошибка при обновлении профиля", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при обновлении профиля");
        }
        return "redirect:/dashboard/profile";
    }
}