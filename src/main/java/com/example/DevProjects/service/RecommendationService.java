package com.example.devprojects.service;

import com.example.devprojects.dto.RecommendationDto;
import com.example.devprojects.model.*;
import com.example.devprojects.repository.ApplicationRepository;
import com.example.devprojects.repository.ProjectRepository;
import com.example.devprojects.repository.ProjectViewRepository;
import com.example.devprojects.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final ProjectViewRepository projectViewRepository;

    private static final double SKILL_WEIGHT = 0.35;
    private static final double SPECIALIZATION_WEIGHT = 0.30;
    private static final double COLLABORATIVE_WEIGHT = 0.35;
    private static final double MIN_SCORE = 0.1;

    // Веса для коллабративной фильтрации
    private static final double WEIGHT_APPLY = 1.0;
    private static final double WEIGHT_FAVORITE = 0.5;
    private static final double WEIGHT_VIEW = 0.1;

    @Transactional(readOnly = true)
    public List<RecommendationDto> getRecommendationsForUser(User user) {
        log.info("Генерация рекомендаций для пользователя: {} (ID: {})", user.getEmail(), user.getId());

        List<Project> allProjects = projectRepository.findAllOpenWithDetails();

        // получаем карту взаимодействий текущего пользователя
        Map<Integer, Double> currentUserInteractions = getUserInteractions(user);

        Map<User, Double> similarUsers = findSimilarUsers(user, currentUserInteractions);
        Map<Long, Double> collaborativeScores = calculateCollaborativeScores(currentUserInteractions, similarUsers);

        List<RecommendationDto> recommendations = new ArrayList<>();

        for (Project project : allProjects) {
            if (project.getAuthor() != null && project.getAuthor().getId().equals(user.getId()))
                continue;
            if (hasUserAppliedToProject(currentUserInteractions, project.getId()))
                continue;

            double skillScore = calculateSkillScore(user, project);
            double specScore = calculateSpecializationScore(user, project);
            double collabScore = collaborativeScores.getOrDefault(project.getId().longValue(), 0.0);

            double baseScore = (skillScore * SKILL_WEIGHT) +
                    (specScore * SPECIALIZATION_WEIGHT) +
                    (collabScore * COLLABORATIVE_WEIGHT);

            // Фактор свежести
            long daysOld = ChronoUnit.DAYS.between(project.getCreatedAt().toLocalDate(), LocalDate.now());
            daysOld = Math.max(0, daysOld);
            double timeDecayMultiplier = Math.exp(-0.0231 * daysOld);

            double finalScore = baseScore * timeDecayMultiplier;

            if (finalScore > MIN_SCORE) {
                String explanation = buildExplanation(skillScore, specScore, collabScore, timeDecayMultiplier);
                recommendations.add(new RecommendationDto(
                        project,
                        finalScore,
                        skillScore,
                        collabScore,
                        explanation
                ));
            }
        }

        recommendations.sort((a, b) -> Double.compare(b.score(), a.score()));

        return recommendations;
    }

    // Сбор всех сигналов пользователя
    private Map<Integer, Double> getUserInteractions(User user) {
        Map<Integer, Double> interactions = new HashMap<>();

        // Просмотры проектов
        Set<Integer> viewedIds = projectViewRepository.findViewedProjectIdsByUser(user.getId());
        if (viewedIds != null && !viewedIds.isEmpty())
            viewedIds.forEach(id -> interactions.merge(id, WEIGHT_VIEW, Double::sum));
        // Избранное
        if (user.getFavorites() != null)
            user.getFavorites().forEach(f -> interactions.merge(f.getProject().getId(), WEIGHT_FAVORITE, Double::sum));
        // Заявки
        List<Application> applications = applicationRepository.findAllBySpecialistIdWithProjectData(user.getId());
        if (applications != null)
            applications.forEach(app -> interactions.merge(app.getProjectRole().getProject().getId(), WEIGHT_APPLY, Double::sum));

        return interactions;
    }

    private boolean hasUserAppliedToProject(Map<Integer, Double> interactions, Integer projectId) {
        return interactions.getOrDefault(projectId, 0.0) >= WEIGHT_APPLY;
    }

    private Map<User, Double> findSimilarUsers(User currentUser, Map<Integer, Double> currentUserInteractions) {
        Map<User, Double> similarUsers = new HashMap<>();
        Set<Integer> currentUserSpecs = extractSpecializationIds(currentUser);

        List<User> allUsers = userRepository.findAll();

        for (User otherUser : allUsers) {
            if (otherUser.getId().equals(currentUser.getId())) continue;

            Map<Integer, Double> otherUserInteractions = getUserInteractions(otherUser);
            Set<Integer> otherUserSpecs = extractSpecializationIds(otherUser);

            double similarity = calculateUserSimilarity(
                    currentUserInteractions, otherUserInteractions,
                    currentUserSpecs, otherUserSpecs
            );

            if (similarity > 0.1)
                similarUsers.put(otherUser, similarity);
        }
        return sortSimilarUsers(similarUsers);
    }

    private double calculateUserSimilarity(Map<Integer, Double> currentInteractions, Map<Integer, Double> otherInteractions,
                                           Set<Integer> currentSpecs, Set<Integer> otherSpecs) {
        double interactionSimilarity = 0.0;
        double specSimilarity = 0.0;

        if (!currentInteractions.isEmpty() && !otherInteractions.isEmpty()) {
            double intersectionScore = 0.0;
            double unionScore = 0.0;

            Set<Integer> allProjectIds = new HashSet<>(currentInteractions.keySet());
            allProjectIds.addAll(otherInteractions.keySet());

            for (Integer id : allProjectIds) {
                double val1 = currentInteractions.getOrDefault(id, 0.0);
                double val2 = otherInteractions.getOrDefault(id, 0.0);
                intersectionScore += Math.min(val1, val2);
                unionScore += Math.max(val1, val2);
            }
            interactionSimilarity = unionScore > 0 ? intersectionScore / unionScore : 0.0;
        }

        if (!currentSpecs.isEmpty() && !otherSpecs.isEmpty()) {
            Set<Integer> intersection = new HashSet<>(currentSpecs);
            intersection.retainAll(otherSpecs);
            specSimilarity = (double) intersection.size() / Math.max(currentSpecs.size(), otherSpecs.size());
        }

        return (interactionSimilarity * 0.7) + (specSimilarity * 0.3);
    }

    private Map<Long, Double> calculateCollaborativeScores(Map<Integer, Double> currentUserInteractions, Map<User, Double> similarUsers) {
        Map<Long, Double> scores = new HashMap<>();
        if (similarUsers.isEmpty()) return scores;

        for (Map.Entry<User, Double> entry : similarUsers.entrySet()) {
            User similarUser = entry.getKey();
            Double userSimilarity = entry.getValue();

            Map<Integer, Double> similarUserInteractions = getUserInteractions(similarUser);

            for (Map.Entry<Integer, Double> interaction : similarUserInteractions.entrySet()) {
                Integer projectId = interaction.getKey();
                Double interactionStrength = interaction.getValue();

                if (currentUserInteractions.getOrDefault(projectId, 0.0) < WEIGHT_APPLY)
                    scores.merge(projectId.longValue(), userSimilarity * interactionStrength, Double::sum);
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
        if (user.getSpecializations() == null || user.getSpecializations().isEmpty()) return 0.0;

        double maxRoleScore = 0.0;

        for (ProjectRole role : project.getRoles()) {
            if (role.getFilledCount() >= role.getVacanciesCount()) continue;

            for (UserSpecialization userSpec : user.getSpecializations()) {
                if (role.getSpecialization().getId().equals(userSpec.getSpecialization().getId())) {

                    double currentRoleScore = 0.8;

                    Integer reqLevelId = role.getProficiencyLevel().getId();
                    Integer userLevelId = userSpec.getProficiencyLevel().getId();

                    if (reqLevelId.equals(userLevelId)) {
                        currentRoleScore = 1.0;
                    } else if (userLevelId > reqLevelId) {
                        currentRoleScore = 0.9;
                    } else {
                        currentRoleScore = 0.5;
                    }

                    if (Boolean.TRUE.equals(userSpec.getIsPrimary()))
                        currentRoleScore = Math.min(currentRoleScore + 0.15, 1.0);
                    if (currentRoleScore > maxRoleScore)
                        maxRoleScore = currentRoleScore;
                }
            }
        }

        return maxRoleScore;
    }

    private String buildExplanation(double skillScore, double specScore, double collabScore, double decayMultiplier) {
        List<String> parts = new ArrayList<>();
        if (skillScore > 0) parts.add(String.format("Стек: %.0f%%", skillScore * 100));
        if (specScore > 0) parts.add(String.format("Роль: %.0f%%", specScore * 100));
        if (collabScore > 0) parts.add(String.format("Интересы: %.0f%%", collabScore * 100));
        if (decayMultiplier < 0.8) parts.add("Архивный проект (снижен приоритет)");
        return String.join(", ", parts);
    }

    private Set<Integer> extractSkillIds(User user) {
        if (user.getSkills() == null) return Collections.emptySet();
        return user.getSkills().stream().map(us -> us.getSkill().getId()).collect(Collectors.toSet());
    }

    private Set<Integer> extractSpecializationIds(User user) {
        if (user.getSpecializations() == null) return Collections.emptySet();
        return user.getSpecializations().stream().map(us -> us.getSpecialization().getId()).collect(Collectors.toSet());
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
}