package com.example.devprojects.dto;

import lombok.Builder;

@Builder
public record SpecializationDto(
        Integer id,
        String name,
        String description
) {}