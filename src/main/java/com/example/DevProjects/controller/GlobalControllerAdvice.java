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

    @ModelAttribute
    public void addNotificationsToModel(Model model, @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser != null) {
            Integer userId = currentUser.getUser().getId();
            model.addAttribute("unreadNotificationsCount", notificationService.getUnreadCount(userId));
            model.addAttribute("recentNotifications", notificationService.getRecentNotifications(userId));
            model.addAttribute("user", currentUser.getUser());
        }
    }
}