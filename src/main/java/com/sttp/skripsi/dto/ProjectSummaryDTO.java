package com.sttp.skripsi.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ProjectSummaryDTO {
    private Integer totalProjects;
    private Integer onScheduleProjects;
    private Integer aheadScheduleProjects;
    private Integer delayedProjects;
    private List<ProjectDetail> projectDetails;

    @Data
    @Builder
    public static class ProjectDetail {
        private Long id;
        private String projectName;
        private Double progress;
        private Integer tasksDone;
        private Integer totalTasks;
        private ScheduleStatus scheduleStatus;
        private LocalDate dueDate;
        private String projectStatus;
    }

    @Data
    @Builder
    public static class ScheduleStatus {
        private Integer tasksAhead;
        private Integer tasksOnTrack;
        private Integer tasksDelayed;
    }
}