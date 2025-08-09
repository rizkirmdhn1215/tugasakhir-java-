package com.sttp.skripsi.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class TalentDetailDTO {
    private Long id;
    private String fullName;
    private boolean overloaded;
    private String role;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private List<String> projects;
    private List<Long> projectIds;
    
    private PerformanceOverview performanceOverview;
    private List<TaskHistory> taskHistory;
    private List<ProjectPerformance> projectPerformance;
    private List<WorkloadAlert> workloadAlerts;
    
    @Data
    @Builder
    public static class PerformanceOverview {
        private int taskEarly;
        private double taskEarlyPercentage;
        private int taskOntime;
        private double taskOntimePercentage;
        private int taskDelayed;
        private double taskDelayedPercentage;
    }
    
    @Data
    @Builder
    public static class MonthlyPerformance {
        private double taskEarlyPercentage;
        private double taskOntimePercentage;
        private double taskDelayedPercentage;
    }
    
    @Data
    @Builder
    public static class TaskHistory {
        private Long idTask;  // Add this line
        private String taskCode;
        private String taskName;
        private String project;
        private String taskCategory;
        private LocalDate date;
        private String status;
        private String timeline;
    }

    @Data
    @Builder
    public static class ProjectPerformance {
        private Long projectId;
        private String projectName;
        private int totalTasks;
        private int completedTasks;
        private int inProgressTasks;
        private int notStartedTasks;
        private double completionPercentage;
    }

    @Data
    @Builder
    public static class WorkloadAlert {
        private String alertType;
        private String message;
        private int currentWorkload;
        private int recommendedWorkload;
    }
}