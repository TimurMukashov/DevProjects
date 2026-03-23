package com.example.devprojects.repository;

import com.example.devprojects.model.ProjectView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface ProjectViewRepository extends JpaRepository<ProjectView, Long> {

    @Query("SELECT DISTINCT pv.project.id FROM ProjectView pv WHERE pv.user.id = :userId")
    Set<Integer> findViewedProjectIdsByUser(@Param("userId") Integer userId);
}