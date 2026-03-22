package com.example.devprojects.service;

import com.example.devprojects.model.*;
import com.example.devprojects.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ProjectRoleRepository projectRoleRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // 1. Подача заявки пользователем
    @Transactional
    public Application applyForRole(Integer specialistId, Integer projectRoleId, String coverLetter) {
        // ИСПРАВЛЕНО: Теперь разрешаем повторную подачу, если предыдущая заявка была отклонена (rejected)
        // Запрещаем только если есть активная заявка (на рассмотрении или уже принятая)
        if (applicationRepository.existsBySpecialistIdAndProjectRoleIdAndStatusNot(
                specialistId, projectRoleId, Application.Status.rejected)) {
            throw new IllegalStateException("У вас уже есть активная заявка на эту роль (на рассмотрении или принята)");
        }

        User specialist = userRepository.findById(specialistId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        ProjectRole projectRole = projectRoleRepository.findById(projectRoleId)
                .orElseThrow(() -> new IllegalArgumentException("Роль в проекте не найдена"));

        Project project = projectRole.getProject();

        if (projectRole.getFilledCount() >= projectRole.getVacanciesCount()) {
            throw new IllegalStateException("Все места на эту роль уже заняты");
        }
        if (project.getAuthor().getId().equals(specialistId)) {
            throw new IllegalStateException("Вы не можете откликнуться на свой собственный проект");
        }

        Application application = Application.builder()
                .specialist(specialist)
                .projectRole(projectRole)
                .coverLetter(coverLetter)
                .status(Application.Status.pending)
                .build();

        Application savedApplication = applicationRepository.save(application);

        // Увеличиваем счетчик заявок в самом проекте (для красивой статистики)
        project.setApplicationsCount(project.getApplicationsCount() + 1);

        // Уведомляем автора проекта
        notificationService.sendNotification(
                project.getAuthor(),
                "application_received",
                "Новый отклик на проект",
                specialist.getFirstName() + " " + specialist.getLastName() + " откликнулся на роль '" + projectRole.getTitle() + "' в проекте '" + project.getTitle() + "'",
                "application",
                savedApplication.getId()
        );

        log.info("Пользователь {} подал заявку на роль {} в проекте {}", specialist.getEmail(), projectRole.getTitle(), project.getTitle());
        return savedApplication;
    }

    // 2. Получение списка заявок для автора проекта (входящие)
    @Transactional(readOnly = true)
    public List<Application> getProjectApplications(Integer projectId, Integer authorId) {
        return applicationRepository.findAllByProjectId(projectId);
    }

    // НОВЫЙ МЕТОД: Получение списка всех заявок, которые подал сам пользователь (исходящие)
    @Transactional(readOnly = true)
    public List<Application> getUserApplications(Integer specialistId) {
        return applicationRepository.findAllBySpecialistIdWithProjectData(specialistId);
    }

    // ДОБАВЛЕНО: Получение заявки по ID
    @Transactional(readOnly = true)
    public Application getById(Integer id) {
        return applicationRepository.findByIdWithProjectData(id)
                .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена"));
    }

    // 3. Автор принимает заявку
    @Transactional
    public void acceptApplication(Integer applicationId, Integer authorId) {
        processApplicationStatus(applicationId, authorId, Application.Status.accepted);
    }

    // 4. Автор отклоняет заявку
    @Transactional
    public void rejectApplication(Integer applicationId, Integer authorId) {
        processApplicationStatus(applicationId, authorId, Application.Status.rejected);
    }

    // Внутренний метод для безопасной смены статуса
    private void processApplicationStatus(Integer applicationId, Integer authorId, Application.Status newStatus) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена"));

        ProjectRole role = application.getProjectRole();
        Project project = role.getProject();

        // Строгая проверка безопасности: только автор проекта может менять статусы
        if (!project.getAuthor().getId().equals(authorId)) {
            throw new SecurityException("У вас нет прав на изменение статуса этой заявки");
        }

        if (application.getStatus() == newStatus) {
            return; // Статус уже стоит
        }

        // Если принимаем заявку, нужно занять место (vacancies_count)
        if (newStatus == Application.Status.accepted) {
            if (role.getFilledCount() >= role.getVacanciesCount()) {
                throw new IllegalStateException("Нет свободных мест на эту роль");
            }
            role.setFilledCount(role.getFilledCount() + 1);
        }

        application.setStatus(newStatus);
        applicationRepository.save(application);

        // Уведомляем специалиста о решении автора
        String statusText = newStatus == Application.Status.accepted ? "принята" : "отклонена";
        notificationService.sendNotification(
                application.getSpecialist(),
                "application_status_changed",
                "Статус вашей заявки",
                "Ваша заявка на роль '" + role.getTitle() + "' в проекте '" + project.getTitle() + "' была " + statusText + ".",
                "application",
                application.getId()
        );
    }
}