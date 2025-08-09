package com.sttp.skripsi.dto;

import lombok.Data;
import lombok.Builder;
import java.util.Map;

@Data
@Builder
public class ProjectDashboardDTO {
    private String projectName;
    private int totalTasks;
    private double overallProgress;
    private Map<String, Integer> delayedTasksByCategory;
    private int totalTalents;
    private int completedTasks;
    private int inProgressTasks;
    private int delayedTasks;
    private double averageEffortSpent;
    private double averageProgressChange;
} 