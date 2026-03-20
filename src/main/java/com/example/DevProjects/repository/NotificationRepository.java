package com.example.devprojects.repository;

import com.example.devprojects.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    // Считаем только непрочитанные, чтобы показать цифру на колокольчике
    // Используем RecipientId, так как в модели поле называется recipient
    long countByRecipientIdAndIsReadFalse(Integer recipientId);

    // Получаем последние 5 уведомлений (и прочитанные, и нет), чтобы показать в выпадающем списке
    List<Notification> findTop5ByRecipientIdOrderByCreatedAtDesc(Integer recipientId);

    // Метод для получения вообще всех уведомлений пользователя
    List<Notification> findAllByRecipientIdOrderByCreatedAtDesc(Integer recipientId);
}