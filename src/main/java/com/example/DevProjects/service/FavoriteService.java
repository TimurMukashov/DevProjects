package com.example.devprojects.service;

import com.example.devprojects.model.Favorite;
import com.example.devprojects.model.Project;
import com.example.devprojects.model.User;
import com.example.devprojects.repository.FavoriteRepository;
import com.example.devprojects.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public boolean toggleFavorite(User user, Integer projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Проект не найден"));

        if (favoriteRepository.existsByUserIdAndProjectId(user.getId(), projectId)) {
            favoriteRepository.deleteByUserAndProject(user, project);
            log.info("Проект {} удален из избранного пользователя {}", projectId, user.getEmail());
            return false;
        } else {
            Favorite favorite = Favorite.builder()
                    .user(user)
                    .project(project)
                    .build();
            favoriteRepository.save(favorite);
            log.info("Проект {} добавлен в избранное пользователя {}", projectId, user.getEmail());
            return true;
        }
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(User user, Integer projectId) {
        if (user == null || projectId == null) return false;
        return favoriteRepository.existsByUserIdAndProjectId(user.getId(), projectId);
    }
}