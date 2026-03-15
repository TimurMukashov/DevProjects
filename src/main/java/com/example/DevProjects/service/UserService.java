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

import java.math.BigDecimal;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public User registerNewUser(UserRegistrationDto registrationDto) {

        if (!registrationDto.password().equals(registrationDto.confirmPassword()))
            throw new IllegalArgumentException("Пароли не совпадают");

        if (userRepository.existsByEmail(registrationDto.email()))
            throw new IllegalArgumentException("Пользователь с таким email уже существует");

        Role defaultRole = roleRepository.findByName("user")
                .orElseThrow(() -> new RuntimeException("Роль 'user' не найдена"));

        User user = User.builder()
                .email(registrationDto.email())
                .passwordHash(passwordEncoder.encode(registrationDto.password()))
                .firstName(registrationDto.firstName())
                .lastName(registrationDto.lastName())
                .bio(registrationDto.bio())
                .role(defaultRole)
                .projects(new HashSet<>())
                .applications(new HashSet<>())
                .specializations(new HashSet<>())
                .skills(new HashSet<>())
                .favorites(new HashSet<>())
                .notifications(new HashSet<>())
                .build();

        User savedUser = userRepository.save(user);

        if (registrationDto.specializations() != null && !registrationDto.specializations().isEmpty()) {
            for (UserRegistrationDto.SpecializationDto specDto : registrationDto.specializations()) {
                if (specDto.specializationId() != null) {
                    Specialization specialization = entityManager.getReference(
                            Specialization.class, specDto.specializationId());

                    UserSpecialization userSpecialization = UserSpecialization.builder()
                            .user(savedUser)
                            .specialization(specialization)
                            .yearsOfExperience(specDto.yearsOfExperience() != null
                                    ? BigDecimal.valueOf(specDto.yearsOfExperience())
                                    : BigDecimal.ZERO)
                            .isPrimary(specDto.primary())
                            .build();

                    entityManager.persist(userSpecialization);
                    savedUser.getSpecializations().add(userSpecialization);
                }
            }
        }

        if (registrationDto.skills() != null && !registrationDto.skills().isEmpty()) {
            for (UserRegistrationDto.SkillDto skillDto : registrationDto.skills()) {
                if (skillDto.skillId() != null && skillDto.proficiencyLevelId() != null) {
                    Skill skill = entityManager.getReference(Skill.class, skillDto.skillId());
                    ProficiencyLevel level = entityManager.getReference(
                            ProficiencyLevel.class, skillDto.proficiencyLevelId());

                    UserSkill userSkill = UserSkill.builder()
                            .user(savedUser)
                            .skill(skill)
                            .proficiencyLevel(level)
                            .yearsOfExperience(skillDto.yearsOfExperience() != null
                                    ? BigDecimal.valueOf(skillDto.yearsOfExperience())
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