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

    @Query("""
        SELECT p
        FROM Project p
        LEFT JOIN FETCH p.author
        WHERE p.status = :status
    """)
    Page<Project> findByStatus(@Param("status") Project.ProjectStatus status, Pageable pageable);


    @Query("""
        SELECT DISTINCT p
        FROM Project p
        LEFT JOIN FETCH p.author
        LEFT JOIN FETCH p.roles r
        LEFT JOIN FETCH r.specialization
        WHERE p.status = :status
        ORDER BY p.createdAt DESC
    """)
    Page<Project> findByStatusWithRoles(@Param("status") Project.ProjectStatus status, Pageable pageable);


    @Query("""
        SELECT DISTINCT p
        FROM Project p
        LEFT JOIN FETCH p.requiredSkills rs
        LEFT JOIN FETCH rs.skill
        WHERE p.id IN :ids
    """)
    List<Project> findProjectsWithSkillsByIds(@Param("ids") List<Integer> ids);


    @Query("""
        SELECT DISTINCT p
        FROM Project p
        LEFT JOIN FETCH p.author
        LEFT JOIN FETCH p.roles r
        LEFT JOIN FETCH r.specialization
        WHERE p.id = :id
    """)
    Optional<Project> findByIdWithRoles(@Param("id") Integer id);


    @Query("""
        SELECT DISTINCT p
        FROM Project p
        LEFT JOIN FETCH p.author
        LEFT JOIN FETCH p.roles r
        LEFT JOIN FETCH r.specialization
        WHERE p.status = 'open'
        ORDER BY p.createdAt DESC
    """)
    List<Project> findAllOpenProjectsWithRoles();


    @Query("""
        SELECT DISTINCT p
        FROM Project p
        LEFT JOIN FETCH p.requiredSkills rs
        LEFT JOIN FETCH rs.skill
        WHERE p.status = 'open'
        ORDER BY p.createdAt DESC
    """)
    List<Project> findAllOpenProjectsWithSkills();


    @Query(
            value = """
        SELECT DISTINCT p
        FROM Project p
        LEFT JOIN FETCH p.author
        LEFT JOIN p.roles r
        LEFT JOIN r.specialization s
        WHERE p.status = 'open'
        AND (
            LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        """,
            countQuery = """
        SELECT COUNT(DISTINCT p)
        FROM Project p
        LEFT JOIN p.roles r
        LEFT JOIN r.specialization s
        WHERE p.status = 'open'
        AND (
            LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        """
    )
    Page<Project> search(@Param("query") String query, Pageable pageable);


    @Query("""
        SELECT DISTINCT p
        FROM Project p
        LEFT JOIN FETCH p.author
        LEFT JOIN FETCH p.roles r
        LEFT JOIN FETCH r.specialization s
        WHERE p.status = 'open'
        AND (
            LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        ORDER BY p.createdAt DESC
    """)
    List<Project> liveSearch(@Param("query") String query);


    @Query("""
        SELECT DISTINCT p
        FROM Project p
        LEFT JOIN p.roles r
        LEFT JOIN r.specialization s
        LEFT JOIN p.requiredSkills rs
        LEFT JOIN rs.skill sk
        WHERE p.status = 'open'
        AND (
            LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(sk.name) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        GROUP BY p.id
        ORDER BY p.createdAt DESC
    """)
    List<Project> liveSearchWithSkills(@Param("query") String query);


    List<Project> findByStatus(Project.ProjectStatus status);
}