package com.example.devprojects.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationDto {

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{6,}$",
            message = "Пароль должен содержать минимум 6 символов, включать цифру, заглавную и строчную букву, спецсимвол")
    private String password;

    @NotBlank(message = "Подтверждение пароля обязательно")
    private String confirmPassword;

    @NotBlank(message = "Имя обязательно")
    @Size(min = 2, max = 100, message = "Имя должно быть от 2 до 100 символов")
    @Pattern(regexp = "^[а-яА-ЯёЁa-zA-Z\\s-]+$", message = "Имя может содержать только буквы, пробелы и дефисы")
    private String firstName;

    @NotBlank(message = "Фамилия обязательна")
    @Size(min = 2, max = 100, message = "Фамилия должна быть от 2 до 100 символов")
    @Pattern(regexp = "^[а-яА-Яa-zA-Z\\s-]+$", message = "Фамилия может содержать только буквы, пробелы и дефисы")
    private String lastName;

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    private String bio;

    @Valid
    private List<SpecializationDto> specializations = new ArrayList<>();

    @Valid
    private List<SkillDto> skills = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecializationDto {
        private Integer specializationId;
        private Integer proficiencyLevelId;

        @Min(value = 0, message = "Опыт не может быть отрицательным")
        @Max(value = 60, message = "Значение опыта выглядит неправдоподобно")
        private Double yearsOfExperience;

        private boolean isPrimary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillDto {
        private Integer skillId;

        @Min(value = 0, message = "Опыт не может быть отрицательным")
        @Max(value = 60, message = "Значение опыта выглядит неправдоподобно")
        private Double yearsOfExperience;
    }
}