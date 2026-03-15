package com.example.devprojects.dto;

import com.example.devprojects.model.Project;

public record RecommendationDto(
        Project project,
        double score,
        double contentScore,
        double collaborativeScore,
        String explanation
) {}