package com.example.devprojects.controller;

import com.example.devprojects.dto.ProficiencyLevelDto;
import com.example.devprojects.dto.SkillDto;
import com.example.devprojects.dto.SpecializationDto;
import com.example.devprojects.repository.ProficiencyLevelRepository;
import com.example.devprojects.repository.SkillRepository;
import com.example.devprojects.repository.SpecializationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/profile-data")
@RequiredArgsConstructor // Автоматически создаст конструктор для финальных полей
public class ProfileDataController {

    private final SpecializationRepository specializationRepository;
    private final SkillRepository skillRepository;
    private final ProficiencyLevelRepository proficiencyLevelRepository;

    @GetMapping("/specializations")
    public List<SpecializationDto> getSpecializations() {
        return specializationRepository.findAllByOrderByNameAsc().stream()
                .map(s -> new SpecializationDto(s.getId(), s.getName(), s.getDescription()))
                .collect(Collectors.toList());
    }

    @GetMapping("/skills")
    public List<SkillDto> getSkills() {
        return skillRepository.findAllByOrderByCategoryAscNameAsc().stream()
                .map(s -> new SkillDto(s.getId(), s.getName(), s.getCategory()))
                .collect(Collectors.toList());
    }

    @GetMapping("/proficiency-levels")
    public List<ProficiencyLevelDto> getProficiencyLevels() {
        return proficiencyLevelRepository.findAllByOrderBySortOrderAsc().stream()
                .map(p -> new ProficiencyLevelDto(
                        p.getId(),
                        p.getName(),
                        p.getDisplayName(),
                        p.getDescription(),
                        p.getSortOrder()))
                .collect(Collectors.toList());
    }
}