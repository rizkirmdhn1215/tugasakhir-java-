package com.sttp.skripsi.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProjectProgressDTO {
    private String projectName;
    private String sheetId;
    
    // Overall project metrics
    private int totalTasks;
    private double overallProjectProgress;
    private Map<String, Integer> delayedTasksCount;
    
    // Talent-specific metrics
    private List<TalentProgress> talentProgresses;
    
    @Data
    @Builder
    public static class TalentProgress {
        private String talentName;
        private int assignedTasks;
        private double averageProgress;
        private int completedTasks;
        private int delayedTasks;
        private int onTrackTasks;
        private List<TaskProgress> taskDetails;
    }
    
    @Data
    @Builder
    public static class TaskProgress {
        private String taskCode;
        private String taskName;
        private int currentProgress;
        private LocalDate deadline;
        private LocalDate estimatedCompletionDate;
        private String scheduleStatus;
        private String delayReason;
        private int baseEstimate;
        private int totalEffortSpent;
    }
} 