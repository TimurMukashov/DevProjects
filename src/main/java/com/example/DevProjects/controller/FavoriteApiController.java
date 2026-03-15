package com.example.devprojects.controller;

import com.example.devprojects.security.CustomUserDetails;
import com.example.devprojects.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Slf4j
public class FavoriteApiController {

    private final FavoriteService favoriteService;

    @PostMapping("/toggle/{projectId}")
    public ResponseEntity<?> toggle(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    @PathVariable Integer projectId) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Необходима авторизация"));
        }

        try {
            boolean added = favoriteService.toggleFavorite(userDetails.getUser(), projectId);
            return ResponseEntity.ok(Map.of("status", added ? "added" : "removed"));
        } catch (Exception e) {
            log.error("Ошибка при переключении избранного", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}