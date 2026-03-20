package com.example.devprojects.controller;

import com.example.devprojects.dto.UserRegistrationDto;
import com.example.devprojects.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginForm(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (error != null)
            model.addAttribute("errorMessage", "Неверный email или пароль");
        if (logout != null)
            model.addAttribute("successMessage", "Вы успешно вышли из системы");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("userRegistrationDto", new UserRegistrationDto());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("userRegistrationDto") UserRegistrationDto registrationDto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {

        log.info("Попытка регистрации пользователя: {}", registrationDto.getEmail());

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        if (!registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.user", "Пароли не совпадают");
            return "auth/register";
        }

        if (userService.existsByEmail(registrationDto.getEmail())) {
            bindingResult.rejectValue("email", "error.user", "Email уже используется");
            return "auth/register";
        }

        try {
            userService.registerNewUser(registrationDto, null);
            redirectAttributes.addFlashAttribute("successMessage", "Регистрация успешна! Теперь вы можете войти.");
            return "redirect:/login";
        } catch (Exception e) {
            log.error("Ошибка при регистрации", e);
            model.addAttribute("errorMessage", "Ошибка при регистрации: " + e.getMessage());
            return "auth/register";
        }
    }
}