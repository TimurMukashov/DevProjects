package com.example.devprojects.repository;

import com.example.devprojects.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    boolean existsBySpecialistIdAndProjectRoleId(Integer specialistId, Integer projectRoleId);

    boolean existsBySpecialistIdAndProjectRoleIdAndStatusNot(Integer specialistId, Integer projectRoleId, Application.Status status);

    @Query("SELECT DISTINCT a FROM Application a " +
            "JOIN FETCH a.projectRole pr " +
            "JOIN FETCH pr.specialization " +
            "LEFT JOIN FETCH pr.proficiencyLevel " +
            "JOIN FETCH pr.project p " +
            "JOIN FETCH p.author " +
            "LEFT JOIN FETCH a.attachments " +
            "WHERE a.specialist.id = :specialistId " +
            "ORDER BY a.createdAt DESC")
    List<Application> findAllBySpecialistIdWithProjectData(@Param("specialistId") Integer specialistId);

    @Query("SELECT DISTINCT a FROM Application a " +
            "JOIN FETCH a.specialist " +
            "JOIN FETCH a.projectRole pr " +
            "JOIN FETCH pr.specialization " +
            "LEFT JOIN FETCH pr.proficiencyLevel " +
            "LEFT JOIN FETCH a.attachments " +
            "WHERE pr.project.id = :projectId " +
            "ORDER BY a.createdAt DESC")
    List<Application> findAllByProjectId(@Param("projectId") Integer projectId);

    @Query("SELECT a FROM Application a " +
            "JOIN FETCH a.projectRole pr " +
            "JOIN FETCH pr.specialization " +
            "LEFT JOIN FETCH pr.proficiencyLevel " +
            "JOIN FETCH pr.project p " +
            "LEFT JOIN FETCH a.attachments " +
            "WHERE a.id = :id")
    Optional<Application> findByIdWithProjectData(@Param("id") Integer id);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.projectRole.project.id = :projectId")
    long countByProjectId(@Param("projectId") Integer projectId);
}