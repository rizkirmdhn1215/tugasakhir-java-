package com.sttp.skripsi.controller;

import com.sttp.skripsi.constant.ErrorMessage;
import com.sttp.skripsi.dto.GoogleSheetsRequest;
import com.sttp.skripsi.dto.GoogleSheetsResponse;
import com.sttp.skripsi.exception.AppException;
import com.sttp.skripsi.service.GoogleSheetsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sheets")
@RequiredArgsConstructor
@Tag(name = "Google Sheets", description = "Google Sheets data extraction APIs")
public class GoogleSheetsController {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsController.class);
    private final GoogleSheetsService googleSheetsService;

    @PostMapping("/process")
    @Operation(summary = "Process Google Sheets data", description = "Extract and process data from Google Sheets using URL")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Data processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "500", description = "Internal server error"),
        @ApiResponse(responseCode = "408", description = "Processing timeout")
    })
    public ResponseEntity<GoogleSheetsResponse> processSheet(@RequestBody GoogleSheetsRequest request) {
        logger.info("Received request to process sheet with URL: {}", request.getSheetUrl());
        
        if (request.getSheetUrl() == null || request.getSheetUrl().trim().isEmpty()) {
            throw AppException.badRequest("Sheet URL is required for processing");
        }
        
        GoogleSheetsResponse response = googleSheetsService.processSheet(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{processId}")
    @Operation(summary = "Get processing status", description = "Check the status of sheet processing")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Status not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<GoogleSheetsResponse> getStatus(@PathVariable String processId) {
        GoogleSheetsResponse response = googleSheetsService.getStatus(processId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh/project/{projectId}")
    @Operation(summary = "Refresh specific project data", description = "Refresh data for a specific project by project ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Refresh initiated successfully"),
        @ApiResponse(responseCode = "404", description = "Project not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, String>> refreshProject(@PathVariable Long projectId) {
        logger.info("Received request to refresh project: {}", projectId);
        String processId = googleSheetsService.refreshProject(projectId);
        Map<String, String> response = new HashMap<>();
        response.put("processId", processId);
        response.put("message", "Refresh initiated for project: " + projectId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh/all")
    @Operation(summary = "Refresh all projects", description = "Refresh data for all stored projects")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Refresh initiated successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> refreshAllProjects() {
        logger.info("Received request to refresh all projects");
        List<String> processIds = googleSheetsService.refreshAllProjects();
        Map<String, Object> response = new HashMap<>();
        response.put("processIds", processIds);
        response.put("message", "Refresh initiated for " + processIds.size() + " projects");
        return ResponseEntity.ok(response);
    }
}