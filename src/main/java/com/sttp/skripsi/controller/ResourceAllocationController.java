package com.sttp.skripsi.controller;

import com.sttp.skripsi.dto.ResourceAllocationDTO;
import com.sttp.skripsi.service.ResourceAllocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Tag(name = "Resource Allocation", description = "APIs for managing resource allocation and workload")
public class ResourceAllocationController {
    private final ResourceAllocationService resourceAllocationService;

    @Operation(
        summary = "Get resource allocation",
        description = "Retrieves comprehensive resource allocation data including talent workloads, project distribution, and workload alerts. Can be filtered by project ID."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved resource allocation data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ResourceAllocationDTO.class)
            )
        )
    })
    @GetMapping("/allocation")
    public ResponseEntity<ResourceAllocationDTO> getResourceAllocation(
        @Parameter(description = "Filter by project ID", required = false)
        @RequestParam(required = false) Long projectId
    ) {
        ResourceAllocationDTO allocation = resourceAllocationService.getResourceAllocation(projectId);
        return ResponseEntity.ok(allocation);
    }

    @Operation(
        summary = "Get list of available projects",
        description = "Retrieves a list of all projects that can be used for filtering resource allocation"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved project list",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = List.class),
                examples = @ExampleObject(
                    value = "[\n" +
                            "  {\n" +
                            "    \"id\": 1,\n" +
                            "    \"name\": \"E-Commerce Platform\"\n" +
                            "  },\n" +
                            "  {\n" +
                            "    \"id\": 2,\n" +
                            "    \"name\": \"Mobile App\"\n" +
                            "  }\n" +
                            "]"
                )
            )
        )
    })
    @GetMapping("/projects")
    public ResponseEntity<List<Map<String, Object>>> getAvailableProjects() {
        List<Map<String, Object>> projects = resourceAllocationService.getAvailableProjects();
        return ResponseEntity.ok(projects);
    }
} 