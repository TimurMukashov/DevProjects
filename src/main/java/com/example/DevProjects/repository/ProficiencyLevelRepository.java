package com.example.devprojects.repository;

import com.example.devprojects.model.ProficiencyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProficiencyLevelRepository extends JpaRepository<ProficiencyLevel, Integer> {
    List<ProficiencyLevel> findAllByOrderBySortOrderAsc();
}