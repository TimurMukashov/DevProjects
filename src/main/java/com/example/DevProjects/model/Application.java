package com.example.devprojects.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Привязываем именно к специалисту, как в SQL (specialist_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialist_id", nullable = false)
    private User specialist;

    // Привязываем к роли в проекте, а не к самому проекту (project_role_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_role_id", nullable = false)
    private ProjectRole projectRole;

    // В базе поле называется cover_letter
    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.pending;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Добавлен статус viewed
    public enum Status {
        pending, viewed, accepted, rejected, withdrawn
    }
}