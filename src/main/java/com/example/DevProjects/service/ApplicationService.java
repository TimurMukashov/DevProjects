package com.example.devprojects.service;

import com.example.devprojects.model.*;
import com.example.devprojects.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ProjectRoleRepository projectRoleRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final FileService fileService;

    private static final int MAX_FILES_COUNT = 10;
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;

    @Transactional
    public Application applyForRole(Integer specialistId, Integer projectRoleId, String coverLetter, List<MultipartFile> files) throws IOException {

        if (applicationRepository.existsBySpecialistIdAndProjectRoleIdAndStatusNot(
                specialistId, projectRoleId, Application.Status.rejected))
            throw new IllegalStateException("У вас уже есть активная заявка на эту роль");

        if (files != null && files.size() > MAX_FILES_COUNT)
            throw new IllegalArgumentException("Нельзя прикрепить более " + MAX_FILES_COUNT + " файлов");

        User specialist = userRepository.findById(specialistId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        ProjectRole projectRole = projectRoleRepository.findById(projectRoleId)
                .orElseThrow(() -> new IllegalArgumentException("Роль в проекте не найдена"));

        Project project = projectRole.getProject();

        if (projectRole.getFilledCount() >= projectRole.getVacanciesCount())
            throw new IllegalStateException("Все места на эту роль уже заняты");

        if (project.getAuthor().getId().equals(specialistId))
            throw new IllegalStateException("Вы не можете откликнуться на свой собственный проект");

        Application application = Application.builder()
                .specialist(specialist)
                .projectRole(projectRole)
                .coverLetter(coverLetter)
                .status(Application.Status.pending)
                .build();

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                        throw new IllegalArgumentException("Размер файла " + file.getOriginalFilename() + " превышает лимит 5 МБ");
                    }

                    String filePath = fileService.saveDocument(file);

                    ApplicationAttachment attachment = ApplicationAttachment.builder()
                            .application(application)
                            .fileName(file.getOriginalFilename())
                            .filePath(filePath)
                            .fileSizeBytes(file.getSize())
                            .mimeType(file.getContentType())
                            .build();

                    application.getAttachments().add(attachment);
                }
            }
        }

        Application savedApplication = applicationRepository.save(application);

        project.setApplicationsCount(project.getApplicationsCount() + 1);

        notificationService.sendNotification(
                project.getAuthor(),
                "application_received",
                "Новый отклик на проект",
                "откликнулся на роль " + projectRole.getSpecialization().getName() + " в проекте " + project.getTitle(),
                "application",
                savedApplication.getId(),
                specialist.getId(),
                specialist.getFirstName() + " " + specialist.getLastName()
        );

        log.info("Пользователь {} подал заявку на роль {} в проекте {} с {} вложениями",
                specialist.getEmail(), projectRole.getSpecialization().getName(), project.getTitle(),
                application.getAttachments().size());

        return savedApplication;
    }

    @Transactional(readOnly = true)
    public List<Application> getProjectApplications(Integer projectId, Integer authorId) {
        return applicationRepository.findAllByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<Application> getUserApplications(Integer specialistId) {
        return applicationRepository.findAllBySpecialistIdWithProjectData(specialistId);
    }

    @Transactional(readOnly = true)
    public Application getById(Integer id) {
        return applicationRepository.findByIdWithProjectData(id)
                .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена"));
    }

    @Transactional
    public void acceptApplication(Integer applicationId, Integer authorId) {
        processApplicationStatus(applicationId, authorId, Application.Status.accepted);
    }

    @Transactional
    public void rejectApplication(Integer applicationId, Integer authorId) {
        processApplicationStatus(applicationId, authorId, Application.Status.rejected);
    }

    private void processApplicationStatus(Integer applicationId, Integer authorId, Application.Status newStatus) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена"));

        ProjectRole role = application.getProjectRole();
        Project project = role.getProject();

        if (!project.getAuthor().getId().equals(authorId))
            throw new SecurityException("У вас нет прав на изменение статуса этой заявки");

        if (application.getStatus() == newStatus)
            return;

        if (newStatus == Application.Status.accepted) {
            if (role.getFilledCount() >= role.getVacanciesCount())
                throw new IllegalStateException("Нет свободных мест на эту роль");
            role.setFilledCount(role.getFilledCount() + 1);
        }

        application.setStatus(newStatus);
        applicationRepository.save(application);

        String statusText = newStatus == Application.Status.accepted ? "принята" : "отклонена";

        notificationService.sendNotification(
                application.getSpecialist(),
                "application_status_changed",
                "Статус вашей заявки",
                "изменил статус вашей заявки на роль " + role.getSpecialization().getName() + " в проекте " + project.getTitle() + " на: " + statusText,
                "application",
                application.getId(),
                project.getAuthor().getId(),
                project.getAuthor().getFirstName() + " " + project.getAuthor().getLastName()
        );
    }
}