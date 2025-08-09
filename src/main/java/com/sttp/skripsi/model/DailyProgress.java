package com.sttp.skripsi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_progress")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "talent_id", nullable = false)
    private Talent talent;

    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "previous_effort")
    private Integer previousEffort;

    @Column(name = "additional_effort")
    private Integer additionalEffort;

    @Column(name = "total_effort_spent")
    private Integer totalEffortSpent;

    @Column(name = "base_estimate")
    private Integer baseEstimate;

    @Column(name = "effort_adjustment")
    private Integer effortAdjustment;

    @Column(name = "final_estimate")
    private Integer finalEstimate;

    private Integer progress;

    @Column(name = "schedule_status")
    private String scheduleStatus;

    @Column(name = "delay_reason")
    private String delayReason;

    @Column(name = "estimated_completion_date")
    private LocalDate estimatedCompletionDate;

    @Column(name = "progress_from")
    private Integer progressFrom;

    @Column(name = "progress_to")
    private Integer progressTo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 