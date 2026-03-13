package com.example.devprojects.service;

import com.example.devprojects.dto.UserRegistrationDto;
import com.example.devprojects.model.*;
import com.example.devprojects.repository.UserRepository;
import com.example.devprojects.repository.RoleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // private final String AVATAR_UPLOAD_DIR = "uploads/avatars/";

    @Transactional
    public User registerNewUser(UserRegistrationDto registrationDto) {

        if (!registrationDto.getPassword().equals(registrationDto.getConfirmPassword()))
            throw new IllegalArgumentException("Пароли не совпадают");

        if (userRepository.existsByEmail(registrationDto.getEmail()))
            throw new IllegalArgumentException("Пользователь с таким email уже существует");

        Role defaultRole = roleRepository.findByName("user")
                .orElseThrow(() -> new RuntimeException("Роль 'user' не найдена"));

        // String avatarUrl = null;
        // if (registrationDto.getAvatar() != null && !registrationDto.getAvatar().isEmpty())
        //     avatarUrl = saveAvatar(registrationDto.getAvatar());

        User user = User.builder()
                .email(registrationDto.getEmail())
                .passwordHash(passwordEncoder.encode(registrationDto.getPassword()))
                .firstName(registrationDto.getFirstName())
                .lastName(registrationDto.getLastName())
                .bio(registrationDto.getBio())
                // .avatarUrl(avatarUrl)
                .role(defaultRole)
                .projects(new HashSet<>())
                .applications(new HashSet<>())
                .specializations(new HashSet<>())
                .skills(new HashSet<>())
                .favorites(new HashSet<>())
                .notifications(new HashSet<>())
                .build();

        User savedUser = userRepository.save(user);

        if (registrationDto.getSpecializations() != null && !registrationDto.getSpecializations().isEmpty()) {
            for (UserRegistrationDto.SpecializationDto specDto : registrationDto.getSpecializations()) {
                if (specDto.getSpecializationId() != null) {
                    Specialization specialization = entityManager.getReference(
                            Specialization.class, specDto.getSpecializationId());

                    UserSpecialization userSpecialization = UserSpecialization.builder()
                            .user(savedUser)
                            .specialization(specialization)
                            .yearsOfExperience(specDto.getYearsOfExperience() != null
                                    ? BigDecimal.valueOf(specDto.getYearsOfExperience())
                                    : BigDecimal.ZERO)
                            .isPrimary(specDto.isPrimary())
                            .build();

                    entityManager.persist(userSpecialization);
                    savedUser.getSpecializations().add(userSpecialization);
                }
            }
        }

        if (registrationDto.getSkills() != null && !registrationDto.getSkills().isEmpty()) {
            for (UserRegistrationDto.SkillDto skillDto : registrationDto.getSkills()) {
                if (skillDto.getSkillId() != null && skillDto.getProficiencyLevelId() != null) {
                    Skill skill = entityManager.getReference(Skill.class, skillDto.getSkillId());
                    ProficiencyLevel level = entityManager.getReference(
                            ProficiencyLevel.class, skillDto.getProficiencyLevelId());

                    UserSkill userSkill = UserSkill.builder()
                            .user(savedUser)
                            .skill(skill)
                            .proficiencyLevel(level)
                            .yearsOfExperience(skillDto.getYearsOfExperience() != null
                                    ? BigDecimal.valueOf(skillDto.getYearsOfExperience())
                                    : BigDecimal.ZERO)
                            .build();

                    entityManager.persist(userSkill);
                    savedUser.getSkills().add(userSkill);
                }
            }
        }

        log.info("Пользователь успешно зарегистрирован: {}", savedUser.getEmail());
        return savedUser;
    }

    /*
    private String saveAvatar(MultipartFile file) {
        try {
            String rootPath = System.getProperty("user.dir");
            Path uploadPath = Paths.get(rootPath, AVATAR_UPLOAD_DIR);

            if (!Files.exists(uploadPath))
                Files.createDirectories(uploadPath);

            if (file.getSize() > 25 * 1024 * 1024) {
                throw new RuntimeException("Файл слишком большой. Максимальный размер 25MB");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RuntimeException("Можно загружать только изображения");
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";

            String filename = UUID.randomUUID() + extension;
            Path filePath = uploadPath.resolve(filename);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.debug("Аватар сохранен: {}", filename);
            return "/uploads/avatars/" + filename;

        } catch (IOException e) {
            log.error("Ошибка при сохранении аватара", e);
            throw new RuntimeException("Не удалось сохранить аватар: " + e.getMessage());
        }
    }
    */

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + email));
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public User getUserWithAllData(String email) {
        return userRepository.findByEmailWithAllData(email)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + email));
    }

    @Transactional(readOnly = true)
    public User getUserWithAllData(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден с id: " + userId));
    }
}