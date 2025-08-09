package com.sttp.skripsi.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProjectComparisonDTO {
    private List<ProjectMetrics> projectMetrics;
    private Map<String, Double> averageMetrics;
    
    @Data
    @Builder
    public static class ProjectMetrics {
        private String projectName;
        private int totalTasks;
        private int totalTalents;
        private double averageProgress;
        private int delayedTasks;
        private int completedTasks;
        private double averageEffortSpent;
        private double progressChangeRate;
        private Map<String, Integer> tasksByCategory;
        private Map<String, Integer> tasksByStatus;
    }
} 