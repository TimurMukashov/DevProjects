package com.example.devprojects.dto;

import com.example.devprojects.model.Project;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecommendationDto {
    private Project project;
    private double score;
    private double contentScore;
    private double collaborativeScore;
    private String explanation;
}