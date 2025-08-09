package com.sttp.skripsi.controller;

import com.sttp.skripsi.dto.TalentDetailDTO;
import com.sttp.skripsi.dto.TalentRequest;
import com.sttp.skripsi.model.Talent;
import com.sttp.skripsi.model.User;
import com.sttp.skripsi.service.TalentService;
import com.sttp.skripsi.service.UserService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@RestController
@RequestMapping("/api/talents")
@RequiredArgsConstructor
@Tag(name = "Talent Management", description = "APIs for managing talents and their progress")
public class TalentController {
    private final TalentService talentService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get all talents", description = "Retrieves a list of all talents in the system")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved all talents",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Talent.class),
                    examples = @ExampleObject(value = """
                            [
                                {
                                    "id": 1,
                                    "name": "John Doe",
                                    "isActive": true,
                                    "createdAt": "2024-01-01T00:00:00"
                                }
                            ]
                            """)))
    public ResponseEntity<List<Talent>> getAllTalents() {
        return ResponseEntity.ok(talentService.getAllTalents());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get talent by ID", description = "Retrieves detailed information about a specific talent including their performance metrics and task history. Can be filtered by task category and status.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the talent details",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = TalentDetailDTO.class)))
    @ApiResponse(responseCode = "404", description = "Talent not found")
    public ResponseEntity<TalentDetailDTO> getTalentById(
            @Parameter(description = "ID of the talent") @PathVariable Long id,
            @Parameter(description = "Filter tasks by category (e.g., 'Coding', 'Testing')") 
            @RequestParam(required = false) String category,
            @Parameter(description = "Filter tasks by status (completed, inprogress, notstarted)") 
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(talentService.getTalentDetail(id, category, status));
    }

    @GetMapping("/user")
    @Operation(summary = "Get current user's talent profile", description = "Retrieves the talent profile of the currently authenticated user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the talent profile",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Talent.class)))
    @ApiResponse(responseCode = "404", description = "Talent profile not found")
    public ResponseEntity<Talent> getTalentByUser(@AuthenticationPrincipal User user) {
        return talentService.getTalentByUserId(user.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new talent", description = "Creates a new talent profile for a user")
    @ApiResponse(responseCode = "200", description = "Successfully created the talent",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Talent.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input")
    public ResponseEntity<Talent> createTalent(@RequestBody TalentRequest request, @AuthenticationPrincipal User user) {
        Talent talent = talentService.createTalent(request.getName(), user.getId());
        return ResponseEntity.ok(talent);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a talent", description = "Updates an existing talent's information")
    @ApiResponse(responseCode = "200", description = "Successfully updated the talent",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Talent.class)))
    @ApiResponse(responseCode = "404", description = "Talent not found")
    public ResponseEntity<Talent> updateTalent(
            @PathVariable Long id,
            @RequestBody UpdateTalentRequest request) {
        Talent talent = talentService.updateTalent(id, request.getName(), request.getIsActive());
        return ResponseEntity.ok(talent);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a talent", description = "Deletes a talent profile")
    @ApiResponse(responseCode = "200", description = "Successfully deleted the talent")
    @ApiResponse(responseCode = "404", description = "Talent not found")
    public ResponseEntity<Void> deleteTalent(@PathVariable Long id) {
        talentService.deleteTalent(id);
        return ResponseEntity.ok().build();
    }

    // Request DTOs
    public static class CreateTalentRequest {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class UpdateTalentRequest {
        private String name;
        private Boolean isActive;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Boolean getIsActive() {
            return isActive;
        }

        public void setIsActive(Boolean isActive) {
            this.isActive = isActive;
        }
    }

    @GetMapping("/{id}/performancetrends")
    @Operation(summary = "Get performance trends", 
               description = "Retrieves performance trends for a talent by week or month")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved performance trends",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Weekly trends",
                        value = """
                            {
                                "performanceTrends": {
                                    "Week1": {
                                        "taskEarlyPercentage": 27.50,
                                        "taskOntimePercentage": 45.00,
                                        "taskDelayedPercentage": 17.50
                                    }
                                }
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Monthly trends",
                        value = """
                            {
                                "performanceTrends": {
                                    "OCTOBER": {
                                        "taskEarlyPercentage": 27.50,
                                        "taskOntimePercentage": 45.00,
                                        "taskDelayedPercentage": 17.50
                                    },
                                    "SEPTEMBER": {
                                        "taskEarlyPercentage": 0.00,
                                        "taskOntimePercentage": 100.00,
                                        "taskDelayedPercentage": 0.00
                                    },
                                    "NOVEMBER": {
                                        "taskEarlyPercentage": 52.94,
                                        "taskOntimePercentage": 20.59,
                                        "taskDelayedPercentage": 26.47
                                    }
                                }
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid period parameter",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Talent not found",
            content = @Content
        )
    })
    public ResponseEntity<Map<String, Object>> getPerformanceTrends(
            @Parameter(description = "ID of the talent") @PathVariable Long id,
            @Parameter(description = "Period to filter by (this week or this month)", 
                      required = true) @RequestParam String period) {
        
        if (!"this_week".equalsIgnoreCase(period) && !"this_month".equalsIgnoreCase(period)) {
            return ResponseEntity.badRequest().build();
        }
        
        Map<String, TalentDetailDTO.MonthlyPerformance> trends = 
            talentService.getPerformanceTrends(id, period);
        
        Map<String, Object> response = new HashMap<>();
        response.put("performanceTrends", trends);
        
        return ResponseEntity.ok(response);
    }
}