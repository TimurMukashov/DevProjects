package com.example.devprojects.dto;

import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.ArrayList;

public record UserRegistrationDto(
        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный формат email")
        String email,

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{6,}$",
                message = "Пароль должен содержать минимум 6 символов, включать цифру, заглавную и строчную букву, спецсимвол")
        String password,

        @NotBlank(message = "Подтверждение пароля обязательно")
        String confirmPassword,

        @NotBlank(message = "Имя обязательно")
        @Size(min = 2, max = 100, message = "Имя должно быть от 2 до 100 символов")
        @Pattern(regexp = "^[а-яА-Яa-zA-Z\\s-]+$", message = "Имя может содержать только буквы, пробелы и дефисы")
        String firstName,

        @NotBlank(message = "Фамилия обязательна")
        @Size(min = 2, max = 100, message = "Фамилия должна быть от 2 до 100 символов")
        @Pattern(regexp = "^[а-яА-Яa-zA-Z\\s-]+$", message = "Фамилия может содержать только буквы, пробелы и дефисы")
        String lastName,

        @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
        String bio,

        MultipartFile avatar,

        List<SpecializationDto> specializations,
        List<SkillDto> skills
) {
    public UserRegistrationDto {
        if (specializations == null) specializations = new ArrayList<>();
        if (skills == null) skills = new ArrayList<>();
    }

    public record SpecializationDto(
            Integer specializationId,
            Double yearsOfExperience,
            boolean primary
    ) {}

    public record SkillDto(
            Integer skillId,
            Integer proficiencyLevelId,
            Double yearsOfExperience
    ) {}
}