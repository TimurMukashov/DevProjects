package com.example.devprojects.dto;

import lombok.Builder;

@Builder
public record SkillDto(
        Integer id,
        String name,
        String category
) {}