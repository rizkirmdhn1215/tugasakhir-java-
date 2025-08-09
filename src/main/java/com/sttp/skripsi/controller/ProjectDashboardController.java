package com.sttp.skripsi.controller;

import com.sttp.skripsi.dto.ProjectDashboardDTO;
import com.sttp.skripsi.service.ProjectDashboardService;
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

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Project Dashboard", description = "APIs for project dashboard and analytics")
public class ProjectDashboardController {
    private final ProjectDashboardService projectDashboardService;

    @Operation(
        summary = "Get project dashboard",
        description = "Retrieves comprehensive dashboard data for a specific project including progress metrics, task distribution, and talent involvement",
        parameters = {
            @Parameter(
                name = "projectId",
                description = "ID of the project to get dashboard for",
                example = "1",
                required = true
            )
        }
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved project dashboard",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProjectDashboardDTO.class),
                examples = @ExampleObject(
                    value = "{\n" +
                            "  \"projectName\": \"E-Commerce Platform\",\n" +
                            "  \"totalTasks\": 50,\n" +
                            "  \"overallProgress\": 75.5,\n" +
                            "  \"delayedTasksByCategory\": {\n" +
                            "    \"Frontend\": 2,\n" +
                            "    \"Backend\": 1\n" +
                            "  },\n" +
                            "  \"totalTalents\": 8,\n" +
                            "  \"completedTasks\": 30,\n" +
                            "  \"inProgressTasks\": 15,\n" +
                            "  \"delayedTasks\": 5,\n" +
                            "  \"averageEffortSpent\": 4.5,\n" +
                            "  \"averageProgressChange\": 2.3\n" +
                            "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Project not found",
            content = @Content(
                examples = @ExampleObject(
                    value = "{\n" +
                            "  \"status\": 404,\n" +
                            "  \"message\": \"Project not found with id: 1\"\n" +
                            "}"
                )
            )
        )
    })
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ProjectDashboardDTO> getProjectDashboard(
        @PathVariable Long projectId
    ) {
        ProjectDashboardDTO dashboard = projectDashboardService.getProjectDashboard(projectId);
        return ResponseEntity.ok(dashboard);
    }
} 