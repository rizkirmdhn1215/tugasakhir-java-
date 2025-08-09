package com.sttp.skripsi.service;

import com.sttp.skripsi.constant.ErrorMessage;
import com.sttp.skripsi.dto.ProjectDashboardDTO;
import com.sttp.skripsi.exception.AppException;
import com.sttp.skripsi.model.*;
import com.sttp.skripsi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectDashboardService {
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final DailyProgressRepository dailyProgressRepository;
    private final TalentRepository talentRepository;

    @Transactional(readOnly = true)
    public ProjectDashboardDTO getProjectDashboard(Long projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> AppException.notFound(ErrorMessage.PROJECT_NOT_FOUND));

        List<Task> tasks = taskRepository.findByProject(project);
        Set<Talent> talents = new HashSet<>();
        Map<String, Integer> delayedTasksByCategory = new HashMap<>();
        int completedTasks = 0;
        int inProgressTasks = 0;
        int delayedTasks = 0;
        double totalEffortSpent = 0;
        double totalProgressChange = 0;
        int progressChangeCount = 0;

        for (Task task : tasks) {
            List<DailyProgress> progressList = dailyProgressRepository.findByTask(task);
            if (!progressList.isEmpty()) {
                DailyProgress latestProgress = progressList.stream()
                    .max(Comparator.comparing(DailyProgress::getDate))
                    .get();

                talents.add(latestProgress.getTalent());
                
                if (latestProgress.getProgress() >= 100) {
                    completedTasks++;
                } else if ("DELAYED".equals(latestProgress.getScheduleStatus())) {
                    delayedTasks++;
                    delayedTasksByCategory.merge(task.getTaskCategory(), 1, Integer::sum);
                } else {
                    inProgressTasks++;
                }

                totalEffortSpent += latestProgress.getTotalEffortSpent();
                
                if (latestProgress.getProgressFrom() != null && latestProgress.getProgressTo() != null) {
                    totalProgressChange += (latestProgress.getProgressTo() - latestProgress.getProgressFrom());
                    progressChangeCount++;
                }
            }
        }

        double averageProgress = tasks.stream()
            .flatMap(task -> dailyProgressRepository.findByTask(task).stream())
            .collect(Collectors.groupingBy(
                DailyProgress::getTask,
                Collectors.maxBy(Comparator.comparing(DailyProgress::getDate))
            ))
            .values()
            .stream()
            .filter(Optional::isPresent)
            .mapToInt(dp -> dp.get().getProgress())
            .average()
            .orElse(0.0);

        return ProjectDashboardDTO.builder()
            .projectName(project.getName())
            .totalTasks(tasks.size())
            .overallProgress(averageProgress)
            .delayedTasksByCategory(delayedTasksByCategory)
            .totalTalents(talents.size())
            .completedTasks(completedTasks)
            .inProgressTasks(inProgressTasks)
            .delayedTasks(delayedTasks)
            .averageEffortSpent(tasks.isEmpty() ? 0 : totalEffortSpent / tasks.size())
            .averageProgressChange(progressChangeCount == 0 ? 0 : totalProgressChange / progressChangeCount)
            .build();
    }
} 