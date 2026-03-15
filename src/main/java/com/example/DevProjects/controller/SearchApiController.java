package com.example.devprojects.controller;

import com.example.devprojects.dto.ProjectPreviewDto;
import com.example.devprojects.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchApiController {

    private final ProjectService projectService;

    @GetMapping("/live")
    public List<ProjectPreviewDto> liveSearch(@RequestParam(required = false) String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return projectService.liveSearch(query.trim());
    }
}