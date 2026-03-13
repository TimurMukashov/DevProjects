package com.example.devprojects.service;

import com.example.devprojects.model.User;
import com.example.devprojects.repository.UserRepository;
import com.example.devprojects.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Загрузка пользователя по email: {}", email);

        try {
            User user = userRepository.findByEmailWithAllData(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + email));

            log.debug("Пользователь найден: {}, роль: {}, специализаций: {}, навыков: {}, избранное: {}",
                    user.getEmail(),
                    user.getRole() != null ? user.getRole().getName() : "не указана",
                    user.getSpecializations() != null ? user.getSpecializations().size() : 0,
                    user.getSkills() != null ? user.getSkills().size() : 0,
                    user.getFavorites() != null ? user.getFavorites().size() : 0);

            return new CustomUserDetails(user);

        } catch (UsernameNotFoundException e) {
            log.warn("Пользователь не найден: {}", email);
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при загрузке пользователя {}: {}", email, e.getMessage(), e);
            throw new UsernameNotFoundException("Ошибка при загрузке пользователя: " + email);
        }
    }
}