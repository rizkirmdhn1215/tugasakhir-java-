package com.sttp.skripsi.service;

import com.sttp.skripsi.constant.ErrorMessage;
import com.sttp.skripsi.dto.TaskDetailResponse;
import com.sttp.skripsi.exception.AppException;
import com.sttp.skripsi.model.DailyProgress;
import com.sttp.skripsi.model.Task;
import com.sttp.skripsi.repository.DailyProgressRepository;
import com.sttp.skripsi.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskDetailService {
    private final TaskRepository taskRepository;
    private final DailyProgressRepository dailyProgressRepository;

    public TaskDetailResponse getTaskDetail(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> AppException.notFound(ErrorMessage.TASK_NOT_FOUND));

        DailyProgress latestProgress = dailyProgressRepository.findFirstByTaskIdOrderByDateDesc(taskId)
                .orElseThrow(() -> AppException.notFound(ErrorMessage.TASK_PROGRESS_NOT_FOUND));

        return TaskDetailResponse.builder()
                .projectId(task.getProject().getId())
                .projectName(task.getProject().getName())
                .taskCode(task.getTaskCode())
                .taskName(task.getTaskName())
                .taskCategory(task.getTaskCategory())
                .date(latestProgress.getDate())
                .scheduleStatus(latestProgress.getScheduleStatus())
                .assignedTo(task.getPicName())
                .progress(latestProgress.getProgress())
                .progressFrom(latestProgress.getProgressFrom())
                .progressTo(latestProgress.getProgressTo())
                .baseEstimate(latestProgress.getBaseEstimate())
                .finalEstimate(latestProgress.getFinalEstimate())
                .adjustment(latestProgress.getEffortAdjustment())
                .previousEffort(latestProgress.getPreviousEffort())
                .additionalEffort(latestProgress.getAdditionalEffort())
                .totalEffortSpent(latestProgress.getTotalEffortSpent())
                .delayReason(latestProgress.getDelayReason())  // Add this line
                .build();
    }
}