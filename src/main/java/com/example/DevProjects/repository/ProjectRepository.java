package com.example.devprojects.repository;

import com.example.devprojects.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Integer> {

    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.author " +
            "LEFT JOIN FETCH p.roles r " +
            "LEFT JOIN FETCH r.specialization " +
            "LEFT JOIN FETCH r.proficiencyLevel " + // Добавлено
            "LEFT JOIN FETCH p.requiredSkills rs " +
            "LEFT JOIN FETCH rs.skill " +
            "WHERE p.author.email = :email " +
            "ORDER BY p.createdAt DESC")
    List<Project> findAllByAuthorEmail(@Param("email") String email);

    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.author " +
            "LEFT JOIN FETCH p.roles r " +
            "LEFT JOIN FETCH r.specialization " +
            "LEFT JOIN FETCH r.proficiencyLevel " + // Добавлено
            "LEFT JOIN FETCH p.requiredSkills rs " +
            "LEFT JOIN FETCH rs.skill " +
            "WHERE p.status = com.example.devprojects.model.Project$ProjectStatus.open")
    List<Project> findAllOpenWithDetails();

    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.author " +
            "LEFT JOIN FETCH p.roles r " +
            "LEFT JOIN FETCH r.specialization " +
            "LEFT JOIN FETCH r.proficiencyLevel " + // Добавлено
            "LEFT JOIN FETCH p.requiredSkills rs " +
            "LEFT JOIN FETCH rs.skill sk " +
            "WHERE p.status = :status")
    Page<Project> findByStatusWithDetails(@Param("status") Project.ProjectStatus status, Pageable pageable);

    @Query("SELECT p FROM Project p " +
            "LEFT JOIN FETCH p.author " +
            "LEFT JOIN FETCH p.roles r " +
            "LEFT JOIN FETCH r.specialization " +
            "LEFT JOIN FETCH r.proficiencyLevel " + // Добавлено
            "LEFT JOIN FETCH p.requiredSkills rs " +
            "LEFT JOIN FETCH rs.skill " +
            "WHERE p.id = :id")
    Optional<Project> findByIdWithDetails(@Param("id") Integer id);

    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.author " +
            "LEFT JOIN FETCH p.roles r " +
            "LEFT JOIN FETCH r.specialization s " +
            "LEFT JOIN FETCH r.proficiencyLevel " +
            "LEFT JOIN FETCH p.requiredSkills rs " +
            "LEFT JOIN FETCH rs.skill sk " +
            "WHERE p.id IN ( " +
            "  SELECT p2.id FROM Project p2 " +
            "  LEFT JOIN p2.roles r2 " +
            "  LEFT JOIN r2.specialization s2 " +
            "  LEFT JOIN p2.requiredSkills rs2 " +
            "  LEFT JOIN rs2.skill sk2 " +
            "  WHERE p2.status = com.example.devprojects.model.Project$ProjectStatus.open " +
            "  AND (LOWER(p2.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "  OR LOWER(p2.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "  OR LOWER(s2.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "  OR LOWER(sk2.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            ")")
    List<Project> liveSearchWithDetails(@Param("query") String query);

    @Query(value = "SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.author " +
            "LEFT JOIN FETCH p.roles r " +
            "LEFT JOIN FETCH r.specialization " +
            "LEFT JOIN FETCH r.proficiencyLevel " +
            "LEFT JOIN FETCH p.requiredSkills rs " +
            "LEFT JOIN FETCH rs.skill " +
            "WHERE p.id IN ( " +
            "  SELECT p2.id FROM Project p2 " +
            "  WHERE (LOWER(p2.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "  OR LOWER(p2.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            ")",
            countQuery = "SELECT COUNT(DISTINCT p) FROM Project p " +
                    "WHERE (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
                    "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Project> search(@Param("query") String query, Pageable pageable);
}