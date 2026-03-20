package com.example.devprojects.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Переименовано из user в recipient, чтобы работал метод .recipient() в билдере сервиса
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User recipient;

    // Изменено на String, так как сервис передает строки (напр. "application_received")
    @Column(nullable = false, length = 255)
    private String type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    // Изменено на примитив boolean, чтобы Lombok сгенерировал именно isRead() и setRead()
    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    // Переименовано в targetType, как ожидает сервис
    @Column(name = "target_type", length = 50)
    private String targetType;

    // Переименовано в targetId, как ожидает сервис
    @Column(name = "target_id")
    private Integer targetId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}