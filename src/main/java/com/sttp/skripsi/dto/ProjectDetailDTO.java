package com.sttp.skripsi.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProjectDetailDTO {
    private String projectName;
    private String projectStatus;
    private Long projectId;
    private LocalDate createdDate;
    private Integer totalTasks;
    private Double progress;
    private ScheduleStatus scheduleStatus;
    private TaskStatus taskStatus;
    private EffortTracking effortTracking;
    private CurrentWorkload currentWorkload;
    private TeamMembers teamMembers;
    private List<TaskDetail> taskList;

    @Data
    @Builder
    public static class ScheduleStatus {
        private Integer tasksAhead;
        private Integer tasksOnTrack;
        private Integer tasksDelayed;
    }

    @Data
    @Builder
    public static class TaskStatus {
        private Integer completed;
        private Integer inProgress;
        private Integer notStarted;
    }

    @Data
    @Builder
    public static class EffortTracking {
        private Integer totalEstimatedEffort;
        private Integer totalSpentEffort;
        private Integer remainingEffort;
    }

    @Data
    @Builder
    public static class CurrentWorkload {
        private Integer totalTeamMembers;
        private Double averageTasksPerMember;
    }

    @Data
    @Builder
    public static class TeamMembers {
        private Integer totalTeam;
        private List<TeamMember> team;
    }

    // Add talentId to TeamMember class
    @Data
    @Builder
    public static class TeamMember {
        private String name;
        private Long talentId;  // Add this field
        private LocalDate joiningDate;
        private Integer assignedTasks;
        private Integer completedTasks;
        private Integer aheadTasks;
        private Integer onTrackTasks;
        private Integer delayedTasks;
        private Integer totalEffortSpent;
        private String timeline;
    }

    // Add taskId to TaskDetail class
    @Data
    @Builder
    public static class TaskDetail {
        private String taskName;
        private Long taskId;  // Add this field
        private String category;
        private String assignee;
        private Double progress;
        private String status;
    }
}