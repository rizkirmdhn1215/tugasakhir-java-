package com.sttp.skripsi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDetailResponse {
    private Long projectId;
    private String projectName;
    private String taskCode;
    private String taskName;
    private String taskCategory;
    private LocalDate date;
    private String scheduleStatus;
    private String assignedTo;
    private Integer progress;
    private Integer progressFrom;
    private Integer progressTo;
    private Integer baseEstimate;
    private Integer finalEstimate;
    private Integer adjustment;
    private Integer previousEffort;
    private Integer additionalEffort;
    private Integer totalEffortSpent;
    private String delayReason;  // Add this field
}