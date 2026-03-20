package com.example.devprojects.controller;

import com.example.devprojects.model.User;
import com.example.devprojects.security.CustomUserDetails;
import com.example.devprojects.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final NotificationService notificationService;

    /**
     * Этот метод автоматически добавляет данные уведомлений во все модели всех контроллеров.
     * Благодаря этому переменные unreadNotificationsCount и recentNotifications
     * доступны в любом HTML-шаблоне без ручного добавления в каждом методе @GetMapping.
     */
    @ModelAttribute
    public void addNotificationsToModel(Model model, @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser != null) {
            Integer userId = currentUser.getUser().getId();

            // Количество непрочитанных для красного баджа на колокольчике
            model.addAttribute("unreadNotificationsCount", notificationService.getUnreadCount(userId));

            // Список последних уведомлений для выпадающего списка
            model.addAttribute("recentNotifications", notificationService.getRecentNotifications(userId));

            // Добавляем объект пользователя, чтобы навбар всегда мог отобразить имя и фамилию
            model.addAttribute("user", currentUser.getUser());
        }
    }
}