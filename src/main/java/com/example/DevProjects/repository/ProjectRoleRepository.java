package com.example.devprojects.repository;

import com.example.devprojects.model.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRoleRepository extends JpaRepository<ProjectRole, Integer> {
    List<ProjectRole> findByProjectId(Integer projectId);
}