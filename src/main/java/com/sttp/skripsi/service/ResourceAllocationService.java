package com.sttp.skripsi.service;

import com.sttp.skripsi.constant.ErrorMessage;
import com.sttp.skripsi.dto.ResourceAllocationDTO;
import com.sttp.skripsi.exception.AppException;
import com.sttp.skripsi.model.*;
import com.sttp.skripsi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ResourceAllocationService {
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final DailyProgressRepository dailyProgressRepository;
    private final TalentRepository talentRepository;

    private static final int MAX_DAILY_EFFORT = 8; // Standard workday hours
    private static final int MAX_WEEKLY_EFFORT = 40; // Standard workweek hours
    private static final int MAX_TASKS_PER_DAY = 3; // Maximum recommended tasks per day
    private static final int WARNING_THRESHOLD = 75; // Warning threshold percentage
    private static final int CRITICAL_THRESHOLD = 90; // Critical threshold percentage

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAvailableProjects() {
        return projectRepository.findAll().stream()
            .map(project -> {
                Map<String, Object> projectInfo = new HashMap<>();
                projectInfo.put("id", project.getId());
                projectInfo.put("name", project.getName());
                return projectInfo;
            })
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResourceAllocationDTO getResourceAllocation(Long projectId) {
        List<Talent> talents = talentRepository.findAll();
        List<Project> projects = projectId != null ? 
            Collections.singletonList(projectRepository.findById(projectId)
                .orElseThrow(() -> AppException.notFound(ErrorMessage.PROJECT_NOT_FOUND))) :
            projectRepository.findAll();
        
        Map<String, Integer> projectWorkload = new HashMap<>();
        Map<String, Integer> categoryWorkload = new HashMap<>();
        List<ResourceAllocationDTO.TalentAllocation> talentAllocations = new ArrayList<>();
        List<ResourceAllocationDTO.WorkloadAlert> workloadAlerts = new ArrayList<>();

        // Calculate workload for each project
        for (Project project : projects) {
            List<Task> tasks = taskRepository.findByProject(project);
            int totalEffort = tasks.stream()
                .flatMap(task -> dailyProgressRepository.findByTask(task).stream())
                .mapToInt(DailyProgress::getTotalEffortSpent)
                .sum();
            projectWorkload.put(project.getId().toString(), totalEffort);
        }

        // Calculate workload for each talent
        for (Talent talent : talents) {
            List<DailyProgress> allProgress = dailyProgressRepository.findByTalent(talent);
            
            // Filter progress by project if projectId is specified
            if (projectId != null) {
                allProgress = allProgress.stream()
                    .filter(dp -> dp.getTask().getProject().getId().equals(projectId))
                    .collect(Collectors.toList());
                
                // Skip talents with no progress for the specified project
                if (allProgress.isEmpty()) {
                    continue;
                }
            }

            Map<Project, List<DailyProgress>> progressByProject = allProgress.stream()
                .collect(Collectors.groupingBy(dp -> dp.getTask().getProject()));

            // Create a single allocation for the talent with all their projects
            List<ResourceAllocationDTO.ProjectWorkload> projectWorkloads = new ArrayList<>();
            int totalTasks = 0;
            int totalEffortSpent = 0;
            int totalProjects = progressByProject.size();

            // Process each project for this talent
            for (Map.Entry<Project, List<DailyProgress>> projectEntry : progressByProject.entrySet()) {
                Project project = projectEntry.getKey();
                List<DailyProgress> projectProgress = projectEntry.getValue();
                
                int projectEffort = projectProgress.stream()
                    .mapToInt(DailyProgress::getTotalEffortSpent)
                    .sum();
                
                double projectProgressPercentage = projectProgress.stream()
                    .mapToInt(DailyProgress::getProgress)
                    .average()
                    .orElse(0.0);
                
                int delayedTasks = (int) projectProgress.stream()
                    .filter(dp -> "DELAYED".equals(dp.getScheduleStatus()))
                    .count();

                // Calculate project-specific workload
                Map<LocalDate, List<DailyProgress>> progressByDate = projectProgress.stream()
                    .collect(Collectors.groupingBy(DailyProgress::getDate));

                // Calculate average daily effort for this project
                double avgDailyEffort = progressByDate.entrySet().stream()
                    .mapToDouble(dateEntry -> dateEntry.getValue().stream()
                        .mapToInt(DailyProgress::getAdditionalEffort)
                        .sum())
                    .average()
                    .orElse(0.0);

                // Calculate average tasks per day for this project
                double avgTasksPerDay = progressByDate.entrySet().stream()
                    .mapToDouble(dateEntry -> dateEntry.getValue().size())
                    .average()
                    .orElse(0.0);

                // Calculate workload percentages
                double dailyEffortPercentage = Math.min(
                    (avgDailyEffort * 100.0) / MAX_DAILY_EFFORT,
                    100.0
                );

                double taskLoadPercentage = Math.min(
                    (avgTasksPerDay * 100.0) / MAX_TASKS_PER_DAY,
                    100.0
                );

                // Combined workload (60% effort, 40% task load)
                int workloadPercentage = (int) (
                    (dailyEffortPercentage * 0.6) + 
                    (taskLoadPercentage * 0.4)
                );

                // Calculate project duration in days
                long projectDuration = progressByDate.keySet().stream()
                    .min(LocalDate::compareTo)
                    .map(startDate -> 
                        ChronoUnit.DAYS.between(
                            startDate,
                            progressByDate.keySet().stream()
                                .max(LocalDate::compareTo)
                                .orElse(startDate)
                        ) + 1
                    )
                    .orElse(1L);

                // Generate alerts if workload is high
                if (workloadPercentage >= CRITICAL_THRESHOLD) {
                    workloadAlerts.add(ResourceAllocationDTO.WorkloadAlert.builder()
                        .talentName(talent.getName())
                        .alertType("CRITICAL")
                        .message(String.format(
                            "High workload in %s: %.1f hours daily effort with %.1f tasks per day over %d days", 
                            project.getName(),
                            avgDailyEffort,
                            avgTasksPerDay,
                            projectDuration
                        ))
                        .currentWorkload(workloadPercentage)
                        .recommendedWorkload(WARNING_THRESHOLD)
                        .build());
                } else if (workloadPercentage >= WARNING_THRESHOLD) {
                    workloadAlerts.add(ResourceAllocationDTO.WorkloadAlert.builder()
                        .talentName(talent.getName())
                        .alertType("WARNING")
                        .message(String.format(
                            "Approaching high workload in %s: %.1f hours daily effort with %.1f tasks per day over %d days",
                            project.getName(),
                            avgDailyEffort,
                            avgTasksPerDay,
                            projectDuration
                        ))
                        .currentWorkload(workloadPercentage)
                        .recommendedWorkload(WARNING_THRESHOLD)
                        .build());
                }

                // Add project workload to the list
                projectWorkloads.add(ResourceAllocationDTO.ProjectWorkload.builder()
                    .projectId(project.getId())
                    .projectName(project.getName())
                    .assignedTasks(projectProgress.size())
                    .totalEffortSpent(projectEffort)
                    .progress(projectProgressPercentage)
                    .delayedTasks(delayedTasks)
                    .avgDailyEffort(avgDailyEffort)
                    .avgTasksPerDay(avgTasksPerDay)
                    .projectDuration(projectDuration)
                    .build());

                totalTasks += projectProgress.size();
                totalEffortSpent += projectEffort;
            }

            // Calculate total available capacity across all projects
            double totalAvgDailyEffort = projectWorkloads.stream()
                .mapToDouble(ResourceAllocationDTO.ProjectWorkload::getAvgDailyEffort)
                .sum();
            int weeklyProjectEffort = (int)(totalAvgDailyEffort * 5); // 5 working days
            int availableCapacity = MAX_WEEKLY_EFFORT - weeklyProjectEffort;

            // Create a single talent allocation with all projects
            talentAllocations.add(ResourceAllocationDTO.TalentAllocation.builder()
                .talentId(talent.getId())
                .talentName(talent.getName())
                .totalProjects(totalProjects)
                .totalTasks(totalTasks)
                .totalEffortSpent(totalEffortSpent)
                .availableCapacity(availableCapacity)
                .projectWorkloads(projectWorkloads)
                .build());

            // Update category workload
            allProgress.stream()
                .map(dp -> dp.getTask().getTaskCategory())
                .forEach(category -> categoryWorkload.merge(category, 1, Integer::sum));
        }

        // Sort talent allocations by talent name
        talentAllocations.sort((a, b) -> a.getTalentName().compareTo(b.getTalentName()));

        return ResourceAllocationDTO.builder()
            .talentAllocations(talentAllocations)
            .projectWorkload(projectWorkload)
            .categoryWorkload(categoryWorkload)
            .workloadAlerts(workloadAlerts)
            .build();
    }
} 