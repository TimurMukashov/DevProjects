package com.example.devprojects.repository;

import com.example.devprojects.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    long countByRecipientIdAndIsReadFalse(Integer recipientId);

    List<Notification> findTop5ByRecipientIdOrderByCreatedAtDesc(Integer recipientId);

    List<Notification> findAllByRecipientIdOrderByCreatedAtDesc(Integer recipientId);
}