package com.sttp.skripsi.service;

import com.sttp.skripsi.constant.ErrorMessage;
import com.sttp.skripsi.exception.AppException;
import com.sttp.skripsi.dto.ProjectComparisonDTO;
import com.sttp.skripsi.model.*;
import com.sttp.skripsi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectComparisonService {
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final DailyProgressRepository dailyProgressRepository;
    private final TalentRepository talentRepository;

    @Transactional(readOnly = true)
    public ProjectComparisonDTO compareProjects(List<Long> projectIds) {
        List<Project> projects = projectRepository.findAllById(projectIds);
        if (projects.isEmpty()) {
            throw AppException.notFound(ErrorMessage.RESOURCE_NOT_FOUND);
        }

        List<ProjectComparisonDTO.ProjectMetrics> projectMetricsList = new ArrayList<>();
        Map<String, Double> averageMetrics = new HashMap<>();
        double totalProgress = 0;
        double totalEffort = 0;
        int totalTasks = 0;
        int totalTalents = 0;
        int totalDelayedTasks = 0;
        int totalCompletedTasks = 0;
        double totalProgressChange = 0;

        for (Project project : projects) {
            List<Task> tasks = taskRepository.findByProject(project);
            Set<Talent> talents = new HashSet<>();
            Map<String, Integer> tasksByCategory = new HashMap<>();
            Map<String, Integer> tasksByStatus = new HashMap<>();
            double projectProgress = 0;
            double projectEffort = 0;
            int projectDelayedTasks = 0;
            int projectCompletedTasks = 0;
            double projectProgressChange = 0;
            int progressChangeCount = 0;

            for (Task task : tasks) {
                List<DailyProgress> progressList = dailyProgressRepository.findByTask(task);
                if (!progressList.isEmpty()) {
                    DailyProgress latestProgress = progressList.stream()
                        .max(Comparator.comparing(DailyProgress::getDate))
                        .get();

                    talents.add(latestProgress.getTalent());
                    projectProgress += latestProgress.getProgress();
                    projectEffort += latestProgress.getTotalEffortSpent();

                    tasksByCategory.merge(task.getTaskCategory(), 1, Integer::sum);
                    tasksByStatus.merge(latestProgress.getScheduleStatus(), 1, Integer::sum);

                    if (latestProgress.getProgress() >= 100) {
                        projectCompletedTasks++;
                    } else if ("DELAYED".equals(latestProgress.getScheduleStatus())) {
                        projectDelayedTasks++;
                    }

                    if (latestProgress.getProgressFrom() != null && latestProgress.getProgressTo() != null) {
                        projectProgressChange += (latestProgress.getProgressTo() - latestProgress.getProgressFrom());
                        progressChangeCount++;
                    }
                }
            }

            double averageProgress = tasks.isEmpty() ? 0 : projectProgress / tasks.size();
            double averageEffort = tasks.isEmpty() ? 0 : projectEffort / tasks.size();
            double progressChangeRate = progressChangeCount == 0 ? 0 : projectProgressChange / progressChangeCount;

            projectMetricsList.add(ProjectComparisonDTO.ProjectMetrics.builder()
                .projectName(project.getName())
                .totalTasks(tasks.size())
                .totalTalents(talents.size())
                .averageProgress(averageProgress)
                .delayedTasks(projectDelayedTasks)
                .completedTasks(projectCompletedTasks)
                .averageEffortSpent(averageEffort)
                .progressChangeRate(progressChangeRate)
                .tasksByCategory(tasksByCategory)
                .tasksByStatus(tasksByStatus)
                .build());

            totalProgress += averageProgress;
            totalEffort += averageEffort;
            totalTasks += tasks.size();
            totalTalents += talents.size();
            totalDelayedTasks += projectDelayedTasks;
            totalCompletedTasks += projectCompletedTasks;
            totalProgressChange += progressChangeRate;
        }

        int projectCount = projects.size();
        averageMetrics.put("averageProgress", totalProgress / projectCount);
        averageMetrics.put("averageEffort", totalEffort / projectCount);
        averageMetrics.put("averageTasks", (double) totalTasks / projectCount);
        averageMetrics.put("averageTalents", (double) totalTalents / projectCount);
        averageMetrics.put("averageDelayedTasks", (double) totalDelayedTasks / projectCount);
        averageMetrics.put("averageCompletedTasks", (double) totalCompletedTasks / projectCount);
        averageMetrics.put("averageProgressChange", totalProgressChange / projectCount);

        return ProjectComparisonDTO.builder()
            .projectMetrics(projectMetricsList)
            .averageMetrics(averageMetrics)
            .build();
    }
} 