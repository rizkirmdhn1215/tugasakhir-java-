package com.sttp.skripsi.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;  // Add this import

@Data
@Builder
public class DashboardSummaryDTO {
    private Integer totalProjects;
    private Integer onSchedule;
    private Double percentageOnSchedule;
    private Integer delayed;
    private Double percentageDelayed;
    private Integer aheadOfSchedule;
    private Double percentageAhead;
    private Integer totalProjectWorkload;
    private Integer projectsWithWorkload;
    
    private Map<String, Map<String, Object>> projectWorkload;
    private List<TopOverloadedTalent> topOverloadedTalents;
    private List<TopDelayedTalent> topDelayedTalents;
    
    @Data
    @Builder
    public static class ProjectWorkload {
        private Long talentId;
        private String talentName;
        private Integer totalProjects;
        private Integer totalTasks;
        private Integer totalEffortSpent;
        private Integer availableCapacity;
        private List<ProjectDetail> projectWorkloads;
        
        @Data
        @Builder
        public static class ProjectDetail {
            private String projectName;
            private Integer assignedTasks;
            private Integer totalEffortSpent;
            private Double progress;
            private Integer delayedTasks;
        }
    }
    
    @Data
    @Builder
    public static class DelayedTask {
        private Long talentId;
        private String talentName;
        private String projectName;
        private Integer totalProjectDelayed;
        private String role;
    }
    
    @Data
    @Builder
    public static class UpcomingDeadline {
        private Long taskId;
        private String taskName;
        private String projectName;
        private LocalDate deadline;
        private Long talentId;
        private String talentName;
        private Integer daysRemaining;
    }
    
    @Data
    @Builder
    public static class TopPerformer {
        private Long talentId;
        private String talentName;
        private String projectName;
        private Double percentageOnTrack;
        private Integer totalTaskOnTrack;
    }
    
    @Data
    @Builder
    public static class TopOverloadedTalent {
        private Long talentId;
        private String talentName;
        private Integer totalTasks;
        private Integer totalEffortSpent;
        private Integer availableCapacity;
        private Double workloadPercentage;
    }
    
    @Data
    @Builder
    public static class TopDelayedTalent {
        private Long talentId;
        private String talentName;
        private Integer totalDelayedTasks;
        private Map<String, Integer> delayedTasksByProject;
    }
}