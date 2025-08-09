package com.sttp.skripsi.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class TalentProjectsDTO {
    private String talentName;
    private List<ProjectProgress> projectProgresses;
    private int totalProjects;
    private int totalTasks;
    private double overallProgress;
    private int completedTasks;
    private int delayedTasks;
    private int onTrackTasks;
    private double averageEffortSpent;
    
    @Data
    @Builder
    public static class ProjectProgress {
        private String projectName;
        private int assignedTasks;
        private double progress;
        private int completedTasks;
        private int delayedTasks;
        private int onTrackTasks;
        private double averageEffortSpent;
        private List<TaskProgress> taskDetails;
    }
    
    @Data
    @Builder
    public static class TaskProgress {
        private String taskCode;
        private String taskName;
        private int currentProgress;
        private String scheduleStatus;
        private int totalEffortSpent;
        private int baseEstimate;
    }
} 