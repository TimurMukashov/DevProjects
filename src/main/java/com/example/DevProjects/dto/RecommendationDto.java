package com.example.devprojects.dto;

import com.example.devprojects.model.Project;

public record RecommendationDto(
        Project project,
        Double score,
        Double skillScore,
        Double collaborativeScore,
        String explanation
) {
}