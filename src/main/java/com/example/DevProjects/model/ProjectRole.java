package com.example.devprojects.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialization_id", nullable = false)
    private Specialization specialization;

    // ЗАМЕНЕНО: вместо String title теперь связь с объектом уровня владения
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proficiency_level_id", nullable = false)
    private ProficiencyLevel proficiencyLevel;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "vacancies_count")
    @Builder.Default
    private Integer vacanciesCount = 1;

    @Column(name = "filled_count")
    @Builder.Default
    private Integer filledCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getSpecializationName() {
        return specialization != null ? specialization.getName() : "";
    }

    public String getProficiencyDisplayName() {
        return proficiencyLevel != null ? proficiencyLevel.getDisplayName() : "";
    }

    public boolean isOpen() {
        return filledCount < vacanciesCount;
    }
}