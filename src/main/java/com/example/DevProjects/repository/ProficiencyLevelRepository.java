package com.example.devprojects.repository;

import com.example.devprojects.model.ProficiencyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProficiencyLevelRepository extends JpaRepository<ProficiencyLevel, Integer> {
    // Сортировка уровней владения согласно заданному порядку (sort_order)
    List<ProficiencyLevel> findAllByOrderBySortOrderAsc();
}