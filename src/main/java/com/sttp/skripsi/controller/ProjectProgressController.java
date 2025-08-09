package com.sttp.skripsi.controller;

import com.sttp.skripsi.dto.ProjectProgressDTO;
import com.sttp.skripsi.dto.ProjectSummaryDTO;
import com.sttp.skripsi.dto.ProjectDetailDTO;
import com.sttp.skripsi.service.ProjectProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
// Add these imports if missing
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Project Progress", description = "Project Progress Management APIs")
public class ProjectProgressController {
    private final ProjectProgressService projectProgressService;

    @Operation(
        summary = "Get project progress statistics",
        description = "Retrieves detailed progress statistics for a specific project, including overall metrics and talent-specific progress"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved project progress",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProjectProgressDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Project not found",
            content = @Content
        )
    })
    @GetMapping("/{projectId}/progress")
    public ResponseEntity<ProjectProgressDTO> getProjectProgress(
        @Parameter(description = "ID of the project to get progress for")
        @PathVariable Long projectId
    ) {
        ProjectProgressDTO progress = projectProgressService.getProjectProgress(projectId);
        return ResponseEntity.ok(progress);
    }
    @Operation(
        summary = "Get summary of all projects",
        description = "Retrieves a summary of all projects including progress statistics, schedule status, and completion estimates"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved projects summary",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProjectSummaryDTO.class)
            )
        )
    })
    @GetMapping("/all/progress")
    public ResponseEntity<ProjectSummaryDTO> getAllProjectsSummary() {
        ProjectSummaryDTO summary = projectProgressService.getAllProjectsSummary();
        return ResponseEntity.ok(summary);
    }
    @Operation(
        summary = "Get projects by schedule status",
        description = "Retrieves projects filtered by their schedule status (DELAYED, ON_TRACK, AHEAD)"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved projects",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProjectSummaryDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid status parameter",
            content = @Content
        )
    })
    @GetMapping("/schedulestatus")
    public ResponseEntity<ProjectSummaryDTO> getProjectsByStatus(
        @Parameter(description = "Schedule status to filter by (DELAYED, ON_TRACK, AHEAD)", required = true)
        @RequestParam String status
    ) {
        switch(status.toUpperCase()) {
            case "DELAYED":
                return ResponseEntity.ok(projectProgressService.getDelayedProjects());
            case "ON_TRACK":
                return ResponseEntity.ok(projectProgressService.getOnScheduleProjects());
            case "AHEAD":
                return ResponseEntity.ok(projectProgressService.getAheadProjects());
            default:
                return ResponseEntity.badRequest().build();
        }
    }
    @Operation(
        summary = "Get detailed project information",
        description = "Retrieves comprehensive details about a project including progress, team members, and task status"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved project details",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProjectDetailDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Project not found",
            content = @Content
        )
    })
    @GetMapping("/{projectId}/detail")
    public ResponseEntity<ProjectDetailDTO> getProjectDetail(
        @Parameter(description = "ID of the project to get details for")
        @PathVariable Long projectId
    ) {
        ProjectDetailDTO detail = projectProgressService.getProjectDetail(projectId);
        return ResponseEntity.ok(detail);
    }
    @Operation(
        summary = "Get all task categories",
        description = "Retrieves a list of all unique task categories across projects"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved task categories",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = List.class)
            )
        )
    })
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllTaskCategories() {
        List<String> categories = projectProgressService.getAllTaskCategories();
        return ResponseEntity.ok(categories);
    }
} // Add this closing brace for the class