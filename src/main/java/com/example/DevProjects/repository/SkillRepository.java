package com.example.devprojects.repository;

import com.example.devprojects.model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Integer> {
    // Группировка по категории и сортировка по имени внутри категории
    List<Skill> findAllByOrderByCategoryAscNameAsc();
}