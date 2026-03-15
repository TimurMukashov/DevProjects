package com.example.devprojects.repository;

import com.example.devprojects.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {

    // Оптимизированный запрос: тянем проект + автора + роли + навыки одним махом
    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.author " +
            "LEFT JOIN FETCH p.roles " +
            "LEFT JOIN FETCH p.requiredSkills rs " +
            "LEFT JOIN FETCH rs.skill " +
            "WHERE p.status = :status")
    Page<Project> findByStatusWithDetails(@Param("status") Project.ProjectStatus status, Pageable pageable);

    // Поиск по ID со всеми связями
    @Query("SELECT p FROM Project p " +
            "LEFT JOIN FETCH p.author " +
            "LEFT JOIN FETCH p.roles " +
            "LEFT JOIN FETCH p.requiredSkills rs " +
            "LEFT JOIN FETCH rs.skill " +
            "WHERE p.id = :id")
    Optional<Project> findByIdWithDetails(@Param("id") Integer id);

    // Полнотекстовый поиск (оставляем твою логику, но в нужном пакете)
    @Query("SELECT p FROM Project p WHERE LOWER(p.title) LIKE %:query% OR LOWER(p.description) LIKE %:query%")
    Page<Project> search(@Param("query") String query, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.requiredSkills rs " +
            "LEFT JOIN FETCH rs.skill " +
            "WHERE LOWER(p.title) LIKE %:query% OR LOWER(p.description) LIKE %:query%")
    List<Project> liveSearchWithSkills(@Param("query") String query);
}