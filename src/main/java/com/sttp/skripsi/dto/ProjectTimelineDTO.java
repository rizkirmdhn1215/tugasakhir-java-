package com.sttp.skripsi.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.time.LocalDateTime;

@Data
@Builder
public class ProjectTimelineDTO {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private List<ProjectTimeline> projectTimelines;

    @Data
    @Builder
    public static class ProjectTimeline {
        private String projectName;
        private Double progress;
        private String status;  // Added this field
    }
}