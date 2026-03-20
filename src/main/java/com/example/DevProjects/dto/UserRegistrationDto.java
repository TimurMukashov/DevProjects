package com.example.devprojects.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

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
    @Pattern(regexp = "^[а-яА-Яa-zA-Z\\s-]+$", message = "Имя может содержать только буквы, пробелы и дефисы")
    private String firstName;

    @NotBlank(message = "Фамилия обязательна")
    @Size(min = 2, max = 100, message = "Фамилия должна быть от 2 до 100 символов")
    @Pattern(regexp = "^[а-яА-Яa-zA-Z\\s-]+$", message = "Фамилия может содержать только буквы, пробелы и дефисы")
    private String lastName;

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    private String bio;

    private List<SpecializationDto> specializations = new ArrayList<>();
    private List<SkillDto> skills = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecializationDto {
        private Integer specializationId;
        private Double yearsOfExperience;
        private boolean primary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillDto {
        private Integer skillId;
        private Integer proficiencyLevelId;
        private Double yearsOfExperience;
    }
}