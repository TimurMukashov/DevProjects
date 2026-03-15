package com.example.devprojects.dto;

public record ProficiencyLevelDto(
        Integer id,
        String name,
        String displayName,
        String description,
        Integer sortOrder
) {
    public ProficiencyLevelDto(Integer id, String name, String displayName, String description) {
        this(id, name, displayName, description, 0);
    }
}