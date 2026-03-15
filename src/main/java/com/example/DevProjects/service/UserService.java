package com.example.devprojects.service;

import com.example.devprojects.dto.ProfileEditDto;
import com.example.devprojects.dto.UserRegistrationDto;
import com.example.devprojects.model.*;
import com.example.devprojects.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    private final SkillRepository skillRepository;
    private final SpecializationRepository specializationRepository;
    private final ProficiencyLevelRepository proficiencyLevelRepository;

    @Transactional
    public void updateProfile(Integer userId, ProfileEditDto dto, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setBio(dto.bio());

        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }

        userRepository.save(user);
    }

    @Transactional
    public User registerNewUser(UserRegistrationDto registrationDto, String avatarUrl) {
        if (!registrationDto.getPassword().equals(registrationDto.getConfirmPassword()))
            throw new IllegalArgumentException("Пароли не совпадают");

        if (userRepository.existsByEmail(registrationDto.getEmail()))
            throw new IllegalArgumentException("Пользователь с таким email уже существует");

        Role defaultRole = roleRepository.findByName("user")
                .orElseThrow(() -> new RuntimeException("Роль 'user' не найдена"));

        User user = User.builder()
                .email(registrationDto.getEmail())
                .passwordHash(passwordEncoder.encode(registrationDto.getPassword()))
                .firstName(registrationDto.getFirstName())
                .lastName(registrationDto.getLastName())
                .bio(registrationDto.getBio())
                .avatarUrl(avatarUrl != null ? avatarUrl : "/img/default-avatar.png") // Дефолт, если не выбрано
                .role(defaultRole)
                .projects(new HashSet<>())
                .applications(new HashSet<>())
                .specializations(new HashSet<>())
                .skills(new HashSet<>())
                .favorites(new HashSet<>())
                .build();

        // Обработка специализаций с проверкой на дубликаты
        if (registrationDto.getSpecializations() != null) {
            Set<Integer> seenSpecs = new HashSet<>();
            for (var specDto : registrationDto.getSpecializations()) {
                if (specDto.getSpecializationId() != null && seenSpecs.add(specDto.getSpecializationId())) {
                    Specialization specialization = specializationRepository.findById(specDto.getSpecializationId())
                            .orElseThrow(() -> new RuntimeException("Специализация не найдена ID: " + specDto.getSpecializationId()));

                    UserSpecialization us = UserSpecialization.builder()
                            .user(user)
                            .specialization(specialization)
                            .yearsOfExperience(specDto.getYearsOfExperience() != null ?
                                    BigDecimal.valueOf(specDto.getYearsOfExperience()) : BigDecimal.ZERO)
                            .isPrimary(specDto.isPrimary())
                            .build();

                    user.getSpecializations().add(us);
                }
            }
        }

        // Обработка навыков с проверкой на дубликаты
        if (registrationDto.getSkills() != null) {
            Set<Integer> seenSkills = new HashSet<>();
            for (var skillDto : registrationDto.getSkills()) {
                if (skillDto.getSkillId() != null && skillDto.getProficiencyLevelId() != null && seenSkills.add(skillDto.getSkillId())) {
                    Skill skill = skillRepository.findById(skillDto.getSkillId())
                            .orElseThrow(() -> new RuntimeException("Навык не найден ID: " + skillDto.getSkillId()));

                    ProficiencyLevel level = proficiencyLevelRepository.findById(skillDto.getProficiencyLevelId())
                            .orElseThrow(() -> new RuntimeException("Уровень не найден ID: " + skillDto.getProficiencyLevelId()));

                    UserSkill us = UserSkill.builder()
                            .user(user)
                            .skill(skill)
                            .proficiencyLevel(level)
                            .yearsOfExperience(skillDto.getYearsOfExperience() != null ?
                                    BigDecimal.valueOf(skillDto.getYearsOfExperience()) : BigDecimal.ZERO)
                            .build();

                    user.getSkills().add(us);
                }
            }
        }

        log.info("Регистрация нового пользователя: {}", user.getEmail());
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public User getUserWithAllData(String email) {
        return userRepository.findByEmailWithAllData(email)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    }
}