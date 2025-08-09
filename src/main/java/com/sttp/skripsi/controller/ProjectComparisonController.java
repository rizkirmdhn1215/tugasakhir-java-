package com.sttp.skripsi.controller;

import com.sttp.skripsi.dto.ProjectComparisonDTO;
import com.sttp.skripsi.service.ProjectComparisonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Project Comparison", description = "APIs for comparing multiple projects")
public class ProjectComparisonController {
    private final ProjectComparisonService projectComparisonService;

    @Operation(
        summary = "Compare projects",
        description = "Compares multiple projects based on various metrics including progress, effort, and task distribution",
        parameters = {
            @Parameter(
                name = "projectIds",
                description = "List of project IDs to compare",
                example = "1,2,3",
                required = true
            )
        }
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully compared projects",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProjectComparisonDTO.class),
                examples = @ExampleObject(
                    value = "{\n" +
                            "  \"projectMetrics\": [\n" +
                            "    {\n" +
                            "      \"projectName\": \"E-Commerce Platform\",\n" +
                            "      \"totalTasks\": 50,\n" +
                            "      \"totalTalents\": 8,\n" +
                            "      \"averageProgress\": 75.5,\n" +
                            "      \"delayedTasks\": 5,\n" +
                            "      \"completedTasks\": 30,\n" +
                            "      \"averageEffortSpent\": 4.5,\n" +
                            "      \"progressChangeRate\": 2.3,\n" +
                            "      \"tasksByCategory\": {\n" +
                            "        \"Frontend\": 20,\n" +
                            "        \"Backend\": 30\n" +
                            "      },\n" +
                            "      \"tasksByStatus\": {\n" +
                            "        \"COMPLETED\": 30,\n" +
                            "        \"IN_PROGRESS\": 15,\n" +
                            "        \"DELAYED\": 5\n" +
                            "      }\n" +
                            "    }\n" +
                            "  ],\n" +
                            "  \"averageMetrics\": {\n" +
                            "    \"averageProgress\": 75.5,\n" +
                            "    \"averageEffort\": 4.5,\n" +
                            "    \"averageTasks\": 50.0,\n" +
                            "    \"averageTalents\": 8.0,\n" +
                            "    \"averageDelayedTasks\": 5.0,\n" +
                            "    \"averageCompletedTasks\": 30.0,\n" +
                            "    \"averageProgressChange\": 2.3\n" +
                            "  }\n" +
                            "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid project IDs provided",
            content = @Content(
                examples = @ExampleObject(
                    value = "{\n" +
                            "  \"status\": 400,\n" +
                            "  \"message\": \"No projects found with the provided IDs\"\n" +
                            "}"
                )
            )
        )
    })
    @GetMapping("/compare")
    public ResponseEntity<ProjectComparisonDTO> compareProjects(
        @RequestParam List<Long> projectIds
    ) {
        ProjectComparisonDTO comparison = projectComparisonService.compareProjects(projectIds);
        return ResponseEntity.ok(comparison);
    }
} 