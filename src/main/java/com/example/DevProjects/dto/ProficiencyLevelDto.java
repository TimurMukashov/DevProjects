package com.example.devprojects.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProficiencyLevelDto {
    private Integer id;
    private String name;
    private String displayName;
    private String description;
    private Integer sortOrder;

    public ProficiencyLevelDto(Integer id, String name, String displayName, String description) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.sortOrder = 0;
    }
}