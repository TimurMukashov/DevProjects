package com.example.devprojects.controller;

import com.example.devprojects.dto.ProjectPreviewDto;
import com.example.devprojects.dto.RecommendationDto;
import com.example.devprojects.model.User;
import com.example.devprojects.security.CustomUserDetails;
import com.example.devprojects.service.ProjectService;
import com.example.devprojects.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final ProjectService projectService;
    private final RecommendationService recommendationService;

    @GetMapping("/")
    public String home(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "false") boolean useRecommendations,
            Model model) {

        if (currentUser != null && useRecommendations) {
            log.debug("Генерация рекомендаций для пользователя: {}", currentUser.getUsername());

            User user = currentUser.getUser();
            List<RecommendationDto> recommendations = recommendationService.getRecommendationsForUser(user);

            List<ProjectPreviewDto> recommendedProjects = recommendations.stream()
                    .map(rec -> ProjectPreviewDto.fromProject(rec.getProject()))
                    .limit(20)
                    .collect(Collectors.toList());

            model.addAttribute("projects", recommendedProjects);
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 1);
            model.addAttribute("totalItems", recommendedProjects.size());
            model.addAttribute("isRecommendations", true);
            model.addAttribute("recommendations", recommendations);

        } else {
            Page<ProjectPreviewDto> projectsPage = projectService.getOpenProjects(page, 20);

            model.addAttribute("projects", projectsPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", projectsPage.getTotalPages());
            model.addAttribute("totalItems", projectsPage.getTotalElements());
            model.addAttribute("isRecommendations", false);
        }

        model.addAttribute("isAuthenticated", currentUser != null);

        return "index";
    }

    @GetMapping("/recommended")
    public String recommendedProjects(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            Model model) {

        if (currentUser == null) {
            return "redirect:/login";
        }

        return "redirect:/?useRecommendations=true";
    }
}