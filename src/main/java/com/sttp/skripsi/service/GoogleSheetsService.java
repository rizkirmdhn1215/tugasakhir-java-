package com.sttp.skripsi.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sttp.skripsi.dto.GoogleSheetsRequest;
import com.sttp.skripsi.dto.GoogleSheetsResponse;
import com.sttp.skripsi.exception.AppException;
import com.sttp.skripsi.constant.ErrorMessage;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class GoogleSheetsService {
    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsService.class);
    private final Map<String, GoogleSheetsResponse> processResults = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<GoogleSheetsResponse>> pendingRequests = new ConcurrentHashMap<>();

    private final RabbitMqService rabbitMqService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public GoogleSheetsService(RabbitMqService rabbitMqService) {
        this.rabbitMqService = rabbitMqService;
    }

    // Process sheet using URL (for /process endpoint only) - now synchronous
    public GoogleSheetsResponse processSheet(GoogleSheetsRequest request) {
        logger.info("Processing sheet with URL: {}", request.getSheetUrl());
        
        if (request.getSheetUrl() == null || request.getSheetUrl().trim().isEmpty()) {
            throw AppException.badRequest("Sheet URL is required");
        }
        
        String processId = java.util.UUID.randomUUID().toString();
        CompletableFuture<GoogleSheetsResponse> future = new CompletableFuture<>();
        pendingRequests.put(processId, future);

        // Build message to send to Go service with URL
        Map<String, Object> message = new HashMap<>();
        message.put("processId", processId);
        message.put("sheetUrl", request.getSheetUrl());

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonMessage = mapper.writeValueAsString(message);
            rabbitMqService.sendToQueue("sheet.process", jsonMessage);
            logger.info("Sent message to RabbitMQ: {}", jsonMessage);
            
            // Wait for response with timeout (30 seconds)
            GoogleSheetsResponse response = future.get(30, TimeUnit.SECONDS);
            return response;
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize message for RabbitMQ", e);
            pendingRequests.remove(processId);
            throw AppException.internalServerError(ErrorMessage.SHEET_PROCESSING_FAILED);
        } catch (TimeoutException e) {
            logger.error("Timeout waiting for response from Golang service", e);
            pendingRequests.remove(processId);
            throw AppException.internalServerError("Processing timeout - no response received within 30 seconds");
        } catch (InterruptedException e) {
            logger.error("Thread interrupted while waiting for response", e);
            pendingRequests.remove(processId);
            Thread.currentThread().interrupt();
            throw AppException.internalServerError("Processing interrupted");
        } catch (ExecutionException e) {
            logger.error("Error executing future", e);
            pendingRequests.remove(processId);
            throw AppException.internalServerError("Processing failed: " + e.getMessage());
        }
    }

    // Refresh project using project ID (converts to sheet ID internally)
    public String refreshProject(Long projectId) {
        logger.info("Refreshing project with ID: {}", projectId);
        
        // Get sheet ID from database using project ID
        String sheetId = getSheetIdByProjectId(projectId);
        if (sheetId == null || sheetId.trim().isEmpty()) {
            throw AppException.notFound("Project not found with ID: " + projectId);
        }
        
        return processSheetById(sheetId);
    }

    // Process sheet using sheet ID (for refresh endpoints only)
    private String processSheetById(String sheetId) {
        logger.info("Processing sheet with ID: {}", sheetId);
        
        if (sheetId == null || sheetId.trim().isEmpty()) {
            throw AppException.badRequest("Sheet ID is required");
        }
        
        String processId = java.util.UUID.randomUUID().toString();

        // Build message to send to Go service with sheet ID
        Map<String, Object> message = new HashMap<>();
        message.put("processId", processId);
        message.put("sheetId", sheetId);

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonMessage = mapper.writeValueAsString(message);
            rabbitMqService.sendToQueue("sheet.process", jsonMessage);
            logger.info("Sent message to RabbitMQ: {}", jsonMessage);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize message for RabbitMQ", e);
            throw AppException.internalServerError(ErrorMessage.SHEET_PROCESSING_FAILED);
        }

        return processId;
    }

    // Refresh all projects
    public List<String> refreshAllProjects() {
        logger.info("Refreshing all projects");
        
        // Get all projects with their sheet IDs
        List<Map<String, Object>> projects = getAllProjectsFromDatabase();
        List<String> processIds = new ArrayList<>();
        
        logger.info("Found {} projects to refresh", projects.size());
        
        for (Map<String, Object> project : projects) {
            try {
                Long projectId = ((Number) project.get("id")).longValue();
                String projectName = (String) project.get("name");
                String sheetId = (String) project.get("sheet_id");
                
                logger.info("Processing project: {} (ID: {}, Sheet ID: {})", projectName, projectId, sheetId);
                
                if (sheetId != null && !sheetId.trim().isEmpty()) {
                    String processId = processSheetById(sheetId);
                    processIds.add(processId);
                    logger.info("Initiated refresh for project: {} with processId: {}", projectName, processId);
                } else {
                    logger.warn("Skipping project {} - no sheet ID found", projectName);
                }
            } catch (Exception e) {
                logger.error("Failed to refresh project: {}", project.get("name"), e);
            }
        }
        
        return processIds;
    }

    // Listen for results from Go
    @RabbitListener(queues = "sheet.process.result")
    public void receiveResult(byte[] message) {
        String json = new String(message, StandardCharsets.UTF_8);
        logger.info("Received result message: {}", json);
        try {
            ObjectMapper mapper = new ObjectMapper();
            GoogleSheetsResponse result = mapper.readValue(json, GoogleSheetsResponse.class);
            logger.info("Parsed result for processId: {}", result.getProcessId());
            
            // Store result for status endpoint
            processResults.put(result.getProcessId(), result);
            
            // Complete the pending future if exists
            CompletableFuture<GoogleSheetsResponse> future = pendingRequests.remove(result.getProcessId());
            if (future != null) {
                future.complete(result);
                logger.info("Completed future for processId: {}", result.getProcessId());
            }
        } catch (Exception e) {
            logger.error("Failed to process result message: {}", e.getMessage(), e);
        }
    }

    public GoogleSheetsResponse getStatus(String processId) {
        return processResults.get(processId);
    }

    // Helper methods
    private String getSheetIdByProjectId(Long projectId) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT sheet_id FROM projects WHERE id = ?",
                String.class, projectId
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private List<Map<String, Object>> getAllProjectsFromDatabase() {
        return jdbcTemplate.queryForList(
            "SELECT id, name, sheet_id, created_at FROM projects ORDER BY created_at DESC"
        );
    }

    private String extractSheetId(String url) {
        // Extract the sheet ID from the Google Sheets URL
        // Example URL: https://docs.google.com/spreadsheets/d/SHEET_ID/edit
        String[] parts = url.split("/");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("d") && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        throw AppException.badRequest(ErrorMessage.INVALID_REQUEST);
    }
}