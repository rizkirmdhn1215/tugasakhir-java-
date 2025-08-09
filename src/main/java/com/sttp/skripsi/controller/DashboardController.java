package com.sttp.skripsi.controller;

import com.sttp.skripsi.dto.DashboardSummaryDTO;
import com.sttp.skripsi.dto.ProjectTimelineDTO;
import com.sttp.skripsi.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard APIs")
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary", description = "Returns a summary of project statuses, workloads, delayed tasks, upcoming deadlines, and top performers")
    @ApiResponse(responseCode = "200", description = "Successful operation", 
                 content = @Content(schema = @Schema(implementation = DashboardSummaryDTO.class)))
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary() {
        return ResponseEntity.ok(dashboardService.getDashboardSummary());
    }

    @GetMapping("/newest-projects")
    @Operation(summary = "Get 5 newest projects", description = "Returns timeline information for the 5 most recently created projects")
    @ApiResponse(responseCode = "200", description = "Successful operation", 
                 content = @Content(schema = @Schema(implementation = ProjectTimelineDTO.class)))
    public ResponseEntity<ProjectTimelineDTO> getNewestProjects() {
        return ResponseEntity.ok(dashboardService.getNewestProjects());
    }
}