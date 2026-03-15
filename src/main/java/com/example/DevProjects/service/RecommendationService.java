package com.example.devprojects.service;

import com.example.devprojects.dto.RecommendationDto;
import com.example.devprojects.model.*;
import com.example.devprojects.repository.ProjectRepository;
import com.example.devprojects.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    private static final double SKILL_WEIGHT = 0.35;        // вес совпадения навыков
    private static final double SPECIALIZATION_WEIGHT = 0.25; // вес совпадения специализаций
    private static final double COLLABORATIVE_WEIGHT = 0.40;  // вес коллаборативной фильтрации
    private static final double MIN_SCORE = 0.1;

    @Transactional(readOnly = true)
    public List<RecommendationDto> getRecommendationsForUser(User user) {
        log.info("Генерация рекомендаций для пользователя: {} (ID: {})", user.getEmail(), user.getId());

        logUserSkills(user);
        logUserSpecializations(user);

        // Используем новый эффективный метод репозитория
        List<Project> allProjects = projectRepository.findAllOpenWithDetails();
        log.info("Всего открытых проектов для анализа: {}", allProjects.size());

        Map<User, Double> similarUsers = findSimilarUsers(user);
        log.info("Найдено похожих пользователей: {}", similarUsers.size());

        Map<Long, Double> collaborativeScores = calculateCollaborativeScores(user, similarUsers);

        List<RecommendationDto> recommendations = new ArrayList<>();

        for (Project project : allProjects) {
            // Исключаем свои проекты
            if (project.getAuthor() != null && project.getAuthor().getId().equals(user.getId()))
                continue;

            // Исключаем уже лайкнутые
            if (isProjectLikedByUser(user, project))
                continue;

            double skillScore = calculateSkillScore(user, project);
            double specScore = calculateSpecializationScore(user, project);
            double collabScore = collaborativeScores.getOrDefault(project.getId().longValue(), 0.0);

            // Финальный расчет по оригинальной формуле
            double finalScore = (skillScore * SKILL_WEIGHT) +
                    (specScore * SPECIALIZATION_WEIGHT) +
                    (collabScore * COLLABORATIVE_WEIGHT);

            if (finalScore > MIN_SCORE) {
                String explanation = buildExplanation(skillScore, specScore, collabScore);
                recommendations.add(new RecommendationDto(
                        project,
                        finalScore,
                        skillScore,
                        collabScore,
                        explanation
                ));
            }
        }

        // Сортировка по убыванию веса (Record accessor syntax)
        recommendations.sort((a, b) -> Double.compare(b.score(), a.score()));

        return recommendations;
    }

    private Map<User, Double> findSimilarUsers(User currentUser) {
        Map<User, Double> similarUsers = new HashMap<>();

        if (currentUser.getFavorites() == null || currentUser.getFavorites().isEmpty()) {
            return findSimilarUsersBySpecialization(currentUser);
        }

        Set<Integer> currentUserLikes = extractProjectIds(currentUser.getFavorites());
        Set<Integer> currentUserSpecs = extractSpecializationIds(currentUser);

        List<User> allUsers = userRepository.findAll();

        for (User otherUser : allUsers) {
            if (otherUser.getId().equals(currentUser.getId())) continue;

            double similarity = calculateUserSimilarity(
                    currentUser, otherUser,
                    currentUserLikes, currentUserSpecs
            );

            if (similarity > 0.1) {
                similarUsers.put(otherUser, similarity);
            }
        }
        return sortSimilarUsers(similarUsers);
    }

    private Map<User, Double> findSimilarUsersBySpecialization(User currentUser) {
        Map<User, Double> similarUsers = new HashMap<>();
        Set<Integer> currentUserSpecs = extractSpecializationIds(currentUser);

        if (currentUserSpecs.isEmpty()) return similarUsers;

        List<User> allUsers = userRepository.findAll();

        for (User otherUser : allUsers) {
            if (otherUser.getId().equals(currentUser.getId())) continue;

            Set<Integer> otherUserSpecs = extractSpecializationIds(otherUser);
            if (!otherUserSpecs.isEmpty()) {
                Set<Integer> intersection = new HashSet<>(currentUserSpecs);
                intersection.retainAll(otherUserSpecs);

                if (!intersection.isEmpty()) {
                    double similarity = (double) intersection.size() /
                            Math.max(currentUserSpecs.size(), otherUserSpecs.size());
                    if (similarity > 0.2) similarUsers.put(otherUser, similarity);
                }
            }
        }
        return similarUsers;
    }

    private double calculateUserSimilarity(User currentUser, User otherUser,
                                           Set<Integer> currentUserLikes,
                                           Set<Integer> currentUserSpecs) {
        double likeSimilarity = 0.0;
        double specSimilarity = 0.0;

        if (!currentUserLikes.isEmpty() && otherUser.getFavorites() != null) {
            Set<Integer> otherUserLikes = extractProjectIds(otherUser.getFavorites());
            if (!otherUserLikes.isEmpty()) {
                Set<Integer> intersection = new HashSet<>(currentUserLikes);
                intersection.retainAll(otherUserLikes);
                Set<Integer> union = new HashSet<>(currentUserLikes);
                union.addAll(otherUserLikes);
                likeSimilarity = (double) intersection.size() / union.size();
            }
        }

        Set<Integer> otherUserSpecs = extractSpecializationIds(otherUser);
        if (!currentUserSpecs.isEmpty() && !otherUserSpecs.isEmpty()) {
            Set<Integer> intersection = new HashSet<>(currentUserSpecs);
            intersection.retainAll(otherUserSpecs);
            specSimilarity = (double) intersection.size() /
                    Math.max(currentUserSpecs.size(), otherUserSpecs.size());
        }

        return (likeSimilarity * 0.6) + (specSimilarity * 0.4);
    }

    private Map<Long, Double> calculateCollaborativeScores(User currentUser, Map<User, Double> similarUsers) {
        Map<Long, Double> scores = new HashMap<>();
        if (similarUsers.isEmpty()) return scores;

        Set<Integer> currentUserLikes = extractProjectIds(currentUser.getFavorites());

        for (Map.Entry<User, Double> entry : similarUsers.entrySet()) {
            User similarUser = entry.getKey();
            Double similarity = entry.getValue();

            if (similarUser.getFavorites() == null) continue;

            Set<Integer> similarUserLikes = extractProjectIds(similarUser.getFavorites());
            for (Integer projectId : similarUserLikes) {
                if (!currentUserLikes.contains(projectId)) {
                    scores.merge(projectId.longValue(), similarity, Double::sum);
                }
            }
        }
        return normalizeScores(scores);
    }

    private double calculateSkillScore(User user, Project project) {
        if (project.getRequiredSkills() == null || project.getRequiredSkills().isEmpty()) return 0.0;

        Set<Integer> userSkillIds = extractSkillIds(user);
        if (userSkillIds.isEmpty()) return 0.0;

        Set<Integer> projectSkillIds = project.getRequiredSkills().stream()
                .map(ps -> ps.getSkill().getId())
                .collect(Collectors.toSet());

        Set<Integer> intersection = new HashSet<>(userSkillIds);
        intersection.retainAll(projectSkillIds);

        long requiredCount = project.getRequiredSkills().stream()
                .filter(ps -> Boolean.TRUE.equals(ps.getIsRequired())).count();

        long matchedRequired = project.getRequiredSkills().stream()
                .filter(ps -> Boolean.TRUE.equals(ps.getIsRequired()) &&
                        userSkillIds.contains(ps.getSkill().getId())).count();

        double baseScore = requiredCount > 0 ? (double) matchedRequired / requiredCount : 0.0;

        long optionalCount = projectSkillIds.size() - requiredCount;
        long matchedOptional = intersection.size() - matchedRequired;
        double optionalBonus = optionalCount > 0 ? (double) matchedOptional / optionalCount * 0.2 : 0.0;

        return Math.min(baseScore + optionalBonus, 1.0);
    }

    private double calculateSpecializationScore(User user, Project project) {
        if (project.getRoles() == null || project.getRoles().isEmpty()) return 0.0;

        Set<Integer> userSpecIds = extractSpecializationIds(user);
        if (userSpecIds.isEmpty()) return 0.0;

        Set<Integer> projectSpecIds = project.getRoles().stream()
                .map(pr -> pr.getSpecialization().getId())
                .collect(Collectors.toSet());

        Set<Integer> intersection = new HashSet<>(userSpecIds);
        intersection.retainAll(projectSpecIds);

        if (intersection.isEmpty()) return 0.0;

        boolean hasPrimarySpec = user.getSpecializations().stream()
                .filter(us -> Boolean.TRUE.equals(us.getIsPrimary()))
                .anyMatch(us -> projectSpecIds.contains(us.getSpecialization().getId()));

        double baseScore = (double) intersection.size() / projectSpecIds.size();
        return hasPrimarySpec ? Math.min(baseScore + 0.3, 1.0) : baseScore;
    }

    private String buildExplanation(double skillScore, double specScore, double collabScore) {
        List<String> parts = new ArrayList<>();
        if (skillScore > 0) parts.add(String.format("Навыки: %.0f%%", skillScore * 100));
        if (specScore > 0) parts.add(String.format("Специализации: %.0f%%", specScore * 100));
        if (collabScore > 0) parts.add(String.format("Похожие интересы: %.0f%%", collabScore * 100));
        return String.join(", ", parts);
    }

    private Set<Integer> extractProjectIds(Set<Favorite> favorites) {
        if (favorites == null) return Collections.emptySet();
        return favorites.stream().map(f -> f.getProject().getId()).collect(Collectors.toSet());
    }

    private Set<Integer> extractSkillIds(User user) {
        if (user.getSkills() == null) return Collections.emptySet();
        return user.getSkills().stream().map(us -> us.getSkill().getId()).collect(Collectors.toSet());
    }

    private Set<Integer> extractSpecializationIds(User user) {
        if (user.getSpecializations() == null) return Collections.emptySet();
        return user.getSpecializations().stream().map(us -> us.getSpecialization().getId()).collect(Collectors.toSet());
    }

    private boolean isProjectLikedByUser(User user, Project project) {
        if (user.getFavorites() == null) return false;
        return user.getFavorites().stream().anyMatch(f -> f.getProject().getId().equals(project.getId()));
    }

    private Map<User, Double> sortSimilarUsers(Map<User, Double> similarUsers) {
        return similarUsers.entrySet().stream()
                .sorted(Map.Entry.<User, Double>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private Map<Long, Double> normalizeScores(Map<Long, Double> scores) {
        if (scores.isEmpty()) return scores;
        double max = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (max == 0) return scores;
        Map<Long, Double> normalized = new HashMap<>();
        for (Map.Entry<Long, Double> entry : scores.entrySet())
            normalized.put(entry.getKey(), entry.getValue() / max);
        return normalized;
    }

    private void logUserSkills(User user) {
        if (user.getSkills() != null && !user.getSkills().isEmpty()) {
            user.getSkills().forEach(us -> log.debug("Навык: {} ID: {}", us.getSkill().getName(), us.getSkill().getId()));
        }
    }

    private void logUserSpecializations(User user) {
        if (user.getSpecializations() != null && !user.getSpecializations().isEmpty()) {
            user.getSpecializations().forEach(us -> log.debug("Спец: {} ID: {}", us.getSpecialization().getName(), us.getSpecialization().getId()));
        }
    }
}