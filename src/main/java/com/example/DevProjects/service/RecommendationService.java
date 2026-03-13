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
        log.info(" Генераций рекомендаций: ");
        log.info("Пользователь: {} (ID: {})", user.getEmail(), user.getId());

        logUserSkills(user);
        logUserSpecializations(user);
        logUserFavorites(user);

        List<Project> allProjects = getAllProjectsWithDetails();
        log.info("Всего открытых проектов: {}", allProjects.size());
        Map<User, Double> similarUsers = findSimilarUsers(user);
        log.info("Найдено похожих пользователей: {}", similarUsers.size());

        Map<Long, Double> collaborativeScores = calculateCollaborativeScores(user, similarUsers);

        List<RecommendationDto> recommendations = new ArrayList<>();

        for (Project project : allProjects) {
            // пропускаем проекты пользователя
            if (project.getAuthor().getId().equals(user.getId()))
                continue;
            // пропускаем проекты, которые пользователь уже лайкнул
            if (isProjectLikedByUser(user, project))
                continue;

            // считаем оценки
            double skillScore = calculateSkillScore(user, project);
            double specScore = calculateSpecializationScore(user, project);
            double collabScore = collaborativeScores.getOrDefault(project.getId().longValue(), 0.0);

            log.debug("Проект: '{}' (ID: {})", project.getTitle(), project.getId());
            log.debug("  skillScore = {:.3f}, specScore = {:.3f}, collabScore = {:.3f}",
                    skillScore, specScore, collabScore);

            // финальная оценка
            double finalScore = calculateFinalScore(skillScore, specScore, collabScore);

            if (finalScore > MIN_SCORE) {
                String explanation = buildExplanation(skillScore, specScore, collabScore);
                recommendations.add(new RecommendationDto(
                        project,
                        finalScore,
                        skillScore,
                        collabScore,
                        explanation
                ));

                log.debug("  Добавлен с score = {:.3f}", finalScore);
            }
        }

        recommendations.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        logRecommendations(recommendations);

        return recommendations;
    }


    // получить все открытые проекты с деталями
    private List<Project> getAllProjectsWithDetails() {
        List<Project> projectsWithRoles = projectRepository.findAllOpenProjectsWithRoles();
        List<Project> projectsWithSkills = projectRepository.findAllOpenProjectsWithSkills();

        Map<Integer, Project> projectsMap = new HashMap<>();

        for (Project p : projectsWithRoles) {
            projectsMap.put(p.getId(), p);
        }

        for (Project p : projectsWithSkills) {
            if (projectsMap.containsKey(p.getId())) {
                Project existing = projectsMap.get(p.getId());
                existing.setRequiredSkills(p.getRequiredSkills());
            } else
                projectsMap.put(p.getId(), p);

        }

        return new ArrayList<>(projectsMap.values());
    }

    // найти похожих пользователей на основе общих лайков и специализаций
    private Map<User, Double> findSimilarUsers(User currentUser) {
        Map<User, Double> similarUsers = new HashMap<>();

        if (currentUser.getFavorites() == null || currentUser.getFavorites().isEmpty()) {
            log.debug("У пользователя нет лайков, поиск похожих только по специализациям");
            return findSimilarUsersBySpecialization(currentUser);
        }

        Set<Integer> currentUserLikes = extractProjectIds(currentUser.getFavorites());
        Set<Integer> currentUserSpecs = extractSpecializationIds(currentUser);

        List<User> allUsers = userRepository.findAll();

        for (User otherUser : allUsers) {
            if (otherUser.getId().equals(currentUser.getId()))
                continue;

            double similarity = calculateUserSimilarity(
                    currentUser, otherUser,
                    currentUserLikes, currentUserSpecs
            );

            if (similarity > 0.1) {
                similarUsers.put(otherUser, similarity);
                log.debug("Найден похожий пользователь ID: {}, схожесть: {:.3f}",
                        otherUser.getId(), similarity);
            }
        }
        return sortSimilarUsers(similarUsers);
    }


    // поиск похожих пользователей только по специализациям (для новых пользователей)
    private Map<User, Double> findSimilarUsersBySpecialization(User currentUser) {
        Map<User, Double> similarUsers = new HashMap<>();
        Set<Integer> currentUserSpecs = extractSpecializationIds(currentUser);

        if (currentUserSpecs.isEmpty())
            return similarUsers;

        List<User> allUsers = userRepository.findAll();

        for (User otherUser : allUsers) {
            if (otherUser.getId().equals(currentUser.getId()))
                continue;

            Set<Integer> otherUserSpecs = extractSpecializationIds(otherUser);

            if (!otherUserSpecs.isEmpty()) {
                Set<Integer> intersection = new HashSet<>(currentUserSpecs);
                intersection.retainAll(otherUserSpecs);

                if (!intersection.isEmpty()) {
                    double similarity = (double) intersection.size() /
                            Math.max(currentUserSpecs.size(), otherUserSpecs.size());
                    if (similarity > 0.2)
                        similarUsers.put(otherUser, similarity);
                }
            }
        }

        return similarUsers;
    }


    // расчет схожести между пользователями
    private double calculateUserSimilarity(User currentUser, User otherUser,
                                           Set<Integer> currentUserLikes,
                                           Set<Integer> currentUserSpecs) {
        double likeSimilarity = 0.0;
        double specSimilarity = 0.0;

        // схожесть по лайкам (Джакарт)
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

        // схожесть по специализациям
        Set<Integer> otherUserSpecs = extractSpecializationIds(otherUser);

        if (!currentUserSpecs.isEmpty() && !otherUserSpecs.isEmpty()) {
            Set<Integer> intersection = new HashSet<>(currentUserSpecs);
            intersection.retainAll(otherUserSpecs);

            specSimilarity = (double) intersection.size() /
                    Math.max(currentUserSpecs.size(), otherUserSpecs.size());
        }

        // тута комбинируем схожести
        return (likeSimilarity * 0.6) + (specSimilarity * 0.4);
    }


    // оценки для коллаборативности
    private Map<Long, Double> calculateCollaborativeScores(User currentUser,
                                                           Map<User, Double> similarUsers) {
        Map<Long, Double> scores = new HashMap<>();

        if (similarUsers.isEmpty())
            return scores;

        Set<Integer> currentUserLikes = extractProjectIds(currentUser.getFavorites());

        for (Map.Entry<User, Double> entry : similarUsers.entrySet()) {
            User similarUser = entry.getKey();
            Double similarity = entry.getValue();

            if (similarUser.getFavorites() == null) continue;

            Set<Integer> similarUserLikes = extractProjectIds(similarUser.getFavorites());

            for (Integer projectId : similarUserLikes)
                if (!currentUserLikes.contains(projectId))
                    scores.merge(projectId.longValue(), similarity, Double::sum);
        }

        return normalizeScores(scores);
    }

    // оценки для контент-бэйсд (по навыкам)
    private double calculateSkillScore(User user, Project project) {
        if (project.getRequiredSkills() == null || project.getRequiredSkills().isEmpty())
            return 0.0;

        Set<Integer> userSkillIds = extractSkillIds(user);
        if (userSkillIds.isEmpty())
            return 0.0;

        Set<Integer> projectSkillIds = project.getRequiredSkills().stream()
                .map(ps -> ps.getSkill().getId())
                .collect(Collectors.toSet());

        if (projectSkillIds.isEmpty())
            return 0.0;

        // количество совпавших навыков
        Set<Integer> intersection = new HashSet<>(userSkillIds);
        intersection.retainAll(projectSkillIds);

        // учитываем обязательные и опциональные навыки
        long requiredSkillsCount = project.getRequiredSkills().stream()
                .filter(ps -> Boolean.TRUE.equals(ps.getIsRequired()))
                .count();

        long matchedRequired = project.getRequiredSkills().stream()
                .filter(ps -> Boolean.TRUE.equals(ps.getIsRequired()) &&
                        userSkillIds.contains(ps.getSkill().getId()))
                .count();

        // базовая оценка = процент совпадения обязательных навыков
        double baseScore = requiredSkillsCount > 0 ?
                (double) matchedRequired / requiredSkillsCount : 0.0;

        // юонус за опциональные навыки (макс + 0.2)
        long optionalSkills = projectSkillIds.size() - requiredSkillsCount;
        long matchedOptional = intersection.size() - matchedRequired;
        double optionalBonus = optionalSkills > 0 ?
                (double) matchedOptional / optionalSkills * 0.2 : 0.0;

        return Math.min(baseScore + optionalBonus, 1.0);
    }

    // оценки для контент-бэйсд (по специализациям)
    private double calculateSpecializationScore(User user, Project project) {
        if (project.getRoles() == null || project.getRoles().isEmpty())
            return 0.0;


        Set<Integer> userSpecIds = extractSpecializationIds(user);
        if (userSpecIds.isEmpty())
            return 0.0;

        Set<Integer> projectSpecIds = project.getRoles().stream()
                .map(pr -> pr.getSpecialization().getId())
                .collect(Collectors.toSet());

        Set<Integer> intersection = new HashSet<>(userSpecIds);
        intersection.retainAll(projectSpecIds);

        if (intersection.isEmpty())
            return 0.0;

        // если есть основная специализация пользователя в проекте
        boolean hasPrimarySpec = user.getSpecializations().stream()
                .filter(us -> Boolean.TRUE.equals(us.getIsPrimary()))
                .anyMatch(us -> projectSpecIds.contains(us.getSpecialization().getId()));

        // база = процент совпадения специализаций
        double baseScore = (double) intersection.size() / projectSpecIds.size();

        // бонус за основную специализацию
        return hasPrimarySpec ? Math.min(baseScore + 0.3, 1.0) : baseScore;
    }

    // финальная оценка
    private double calculateFinalScore(double skillScore, double specScore, double collabScore) {
        return (skillScore * SKILL_WEIGHT) +
                (specScore * SPECIALIZATION_WEIGHT) +
                (collabScore * COLLABORATIVE_WEIGHT);
    }

    // объяснялка
    private String buildExplanation(double skillScore, double specScore, double collabScore) {
        List<String> parts = new ArrayList<>();
        if (skillScore > 0)
            parts.add(String.format("Навыки: %.0f%%", skillScore * 100));
        if (specScore > 0)
            parts.add(String.format("Специализации: %.0f%%", specScore * 100));
        if (collabScore > 0)
            parts.add(String.format("Похожие пользователи: %.0f%%", collabScore * 100));
        return String.join(", ", parts);
    }

    /**
     * всякие методы
     */
    private Set<Integer> extractProjectIds(Set<Favorite> favorites) {
        return favorites.stream()
                .map(f -> f.getProject().getId())
                .collect(Collectors.toSet());
    }

    private Set<Integer> extractSkillIds(User user) {
        return user.getSkills().stream()
                .map(us -> us.getSkill().getId())
                .collect(Collectors.toSet());
    }

    private Set<Integer> extractSpecializationIds(User user) {
        return user.getSpecializations().stream()
                .map(us -> us.getSpecialization().getId())
                .collect(Collectors.toSet());
    }

    private boolean isProjectLikedByUser(User user, Project project) {
        return user.getFavorites().stream()
                .anyMatch(f -> f.getProject().getId().equals(project.getId()));
    }

    /**
     * сортировка похожих пользователей по убыванию схожести
     */
    private Map<User, Double> sortSimilarUsers(Map<User, Double> similarUsers) {
        return similarUsers.entrySet().stream()
                .sorted(Map.Entry.<User, Double>comparingByValue().reversed())
                .limit(10) // Оставляем только 10 самых похожих
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * нормализация оценок
     */
    private Map<Long, Double> normalizeScores(Map<Long, Double> scores) {
        if (scores.isEmpty())
            return scores;

        double max = scores.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(1.0);
        if (max == 0)
            return scores;

        Map<Long, Double> normalized = new HashMap<>();
        for (Map.Entry<Long, Double> entry : scores.entrySet())
            normalized.put(entry.getKey(), entry.getValue() / max);

        return normalized;
    }

    /**
     * логируем для отладки
     */
    private void logUserSkills(User user) {
        log.info("Навыки для пользователя");
        if (user.getSkills() != null && !user.getSkills().isEmpty()) {
            user.getSkills().forEach(us ->
                    log.info("  - {} (ID: {}, уровень: {}, опыт: {})",
                            us.getSkill().getName(),
                            us.getSkill().getId(),
                            us.getProficiencyLevel().getDisplayName(),
                            us.getYearsOfExperience()));
        } else
            log.info("  Навыки не указаны");
    }

    private void logUserSpecializations(User user) {
        log.info("Специализации для пользователя:");
        if (user.getSpecializations() != null && !user.getSpecializations().isEmpty()) {
            user.getSpecializations().forEach(us ->
                    log.info("  - {} (ID: {}, основная: {}, опыт: {})",
                            us.getSpecialization().getName(),
                            us.getSpecialization().getId(),
                            us.getIsPrimary() ? "да" : "нет",
                            us.getYearsOfExperience()));
        } else
            log.info("  Специализации не указаны");
    }

    private void logUserFavorites(User user) {
        log.info("Избранное для пользователя:");
        if (user.getFavorites() != null && !user.getFavorites().isEmpty()) {
            user.getFavorites().forEach(f ->
                    log.info("  - Проект: {} (ID: {})",
                            f.getProject().getTitle(),
                            f.getProject().getId()));
        } else
            log.info("  Избранное отсутствует");
    }

    private void logRecommendations(List<RecommendationDto> recommendations) {
        log.info("Результат !!!");
        log.info("Найдено рекомендаций: {}", recommendations.size());

        for (int i = 0; i < Math.min(10, recommendations.size()); i++) {
            RecommendationDto rec = recommendations.get(i);
            log.info("  {}. {} - score: {:.3f} ({})",
                    i + 1,
                    rec.getProject().getTitle(),
                    rec.getScore(),
                    rec.getExplanation());
        }

        if (recommendations.size() > 10)
            log.info("  ... и еще {} рекомендаций", recommendations.size() - 10);
        log.info("_____________________________________________");
    }
}