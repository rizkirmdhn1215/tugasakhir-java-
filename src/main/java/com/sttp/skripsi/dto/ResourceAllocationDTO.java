package com.sttp.skripsi.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ResourceAllocationDTO {
    private List<TalentAllocation> talentAllocations;
    private Map<String, Integer> projectWorkload;
    private Map<String, Integer> categoryWorkload;
    private List<WorkloadAlert> workloadAlerts;
    
    @Data
    @Builder
    public static class TalentAllocation {
        private Long talentId;
        private String talentName;
        private int totalProjects;
        private int totalTasks;
        private int totalEffortSpent;
        private int availableCapacity;
        private List<ProjectWorkload> projectWorkloads;
    }
    
    @Data
    @Builder
    public static class ProjectWorkload {
        private Long projectId;
        private String projectName;
        private int assignedTasks;
        private int totalEffortSpent;
        private double progress;
        private int delayedTasks;
        private double avgDailyEffort;
        private double avgTasksPerDay;
        private long projectDuration;
    }
    
    @Data
    @Builder
    public static class WorkloadAlert {
        private String talentName;
        private String alertType;
        private String message;
        private int currentWorkload;
        private int recommendedWorkload;
    }
} 