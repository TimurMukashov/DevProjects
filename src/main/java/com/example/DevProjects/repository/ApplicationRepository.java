package com.example.devprojects.repository;

import com.example.devprojects.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    // Проверка: подавал ли уже этот пользователь заявку на эту конкретную роль?
    boolean existsBySpecialistIdAndProjectRoleId(Integer specialistId, Integer projectRoleId);

    // ИСПРАВЛЕНО: Добавлен JOIN FETCH для загрузки роли, проекта и автора проекта одним запросом
    @Query("SELECT a FROM Application a " +
            "JOIN FETCH a.projectRole pr " +
            "JOIN FETCH pr.project p " +
            "JOIN FETCH p.author " +
            "WHERE a.specialist.id = :specialistId " +
            "ORDER BY a.createdAt DESC")
    List<Application> findAllBySpecialistIdWithProjectData(@Param("specialistId") Integer specialistId);

    // Получить все заявки на конкретный проект
    @Query("SELECT a FROM Application a " +
            "JOIN FETCH a.specialist " +
            "JOIN FETCH a.projectRole pr " +
            "WHERE pr.project.id = :projectId " +
            "ORDER BY a.createdAt DESC")
    List<Application> findAllByProjectId(@Param("projectId") Integer projectId);

    // Подсчет количества заявок на проект
    @Query("SELECT COUNT(a) FROM Application a WHERE a.projectRole.project.id = :projectId")
    long countByProjectId(@Param("projectId") Integer projectId);
}