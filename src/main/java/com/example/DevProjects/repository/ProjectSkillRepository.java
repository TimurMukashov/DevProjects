package com.example.devprojects.repository;

import com.example.devprojects.model.ProjectSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectSkillRepository extends JpaRepository<ProjectSkill, Integer> {
}