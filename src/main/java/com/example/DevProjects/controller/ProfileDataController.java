package com.example.devprojects.controller;

import com.example.devprojects.dto.ProficiencyLevelDto;
import com.example.devprojects.dto.SkillDto;
import com.example.devprojects.dto.SpecializationDto;
import com.example.devprojects.model.ProficiencyLevel;
import com.example.devprojects.model.Skill;
import com.example.devprojects.model.Specialization;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/profile-data")
public class ProfileDataController {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/specializations")
    public List<SpecializationDto> getSpecializations() {
        List<Specialization> specializations = entityManager
                .createQuery("SELECT s FROM Specialization s ORDER BY s.name", Specialization.class)
                .getResultList();

        return specializations.stream()
                .map(s -> new SpecializationDto(s.getId(), s.getName(), s.getDescription()))
                .collect(Collectors.toList());
    }

    @GetMapping("/skills")
    public List<SkillDto> getSkills() {
        List<Skill> skills = entityManager
                .createQuery("SELECT s FROM Skill s ORDER BY s.category, s.name", Skill.class)
                .getResultList();

        return skills.stream()
                .map(s -> new SkillDto(s.getId(), s.getName(), s.getCategory()))
                .collect(Collectors.toList());
    }

    @GetMapping("/proficiency-levels")
    public List<ProficiencyLevelDto> getProficiencyLevels() {
        List<ProficiencyLevel> levels = entityManager
                .createQuery("SELECT p FROM ProficiencyLevel p ORDER BY p.sortOrder", ProficiencyLevel.class)
                .getResultList();

        return levels.stream()
                .map(p -> new ProficiencyLevelDto(
                        p.getId(),
                        p.getName(),
                        p.getDisplayName(),
                        p.getDescription()))
                .collect(Collectors.toList());
    }
}