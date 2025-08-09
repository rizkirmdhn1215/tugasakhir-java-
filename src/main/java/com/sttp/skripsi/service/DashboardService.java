package com.sttp.skripsi.service;

import com.sttp.skripsi.dto.DashboardSummaryDTO;
import com.sttp.skripsi.dto.ProjectTimelineDTO;
import com.sttp.skripsi.dto.ResourceAllocationDTO;
import com.sttp.skripsi.model.*;
import com.sttp.skripsi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private static final int MAX_WORKLOAD = 100;
    private static final int UPCOMING_DEADLINE_DAYS = 14; // Show deadlines within next 14 days
    
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final DailyProgressRepository dailyProgressRepository;
    private final TalentRepository talentRepository;
    private final ResourceAllocationService resourceAllocationService;
    
    @Transactional(readOnly = true)
    public DashboardSummaryDTO getDashboardSummary() {
        List<Project> projects = projectRepository.findAll();
        List<Task> tasks = taskRepository.findAll();
        List<DailyProgress> progress = dailyProgressRepository.findAll();
        List<Talent> talents = talentRepository.findAll();

        // Calculate project statuses
        Map<String, Integer> statusCounts = calculateProjectStatuses(projects, tasks);
        
        // Calculate project workload
        Map<String, Map<String, Object>> projectWorkload = calculateProjectWorkload(projects, progress);
        
        // Calculate projects with workload (projects that have any effort spent)
        int projectsWithWorkload = (int) projectWorkload.values().stream()
            .filter(workload -> (Double) workload.get("totalEffortSpent") > 0)
            .count();

        // Get top 5 overloaded talents
        List<DashboardSummaryDTO.TopOverloadedTalent> topOverloadedTalents = getTopOverloadedTalents(talents, progress);
        
        // Get top 5 talents with delayed tasks
        List<DashboardSummaryDTO.TopDelayedTalent> topDelayedTalents = getTopDelayedTalents(talents, tasks, progress);

        return DashboardSummaryDTO.builder()
            .totalProjects(projects.size())
            .onSchedule(statusCounts.get("onSchedule"))
            .percentageOnSchedule(calculatePercentage(statusCounts.get("onSchedule"), projects.size()))
            .delayed(statusCounts.get("delayed"))
            .percentageDelayed(calculatePercentage(statusCounts.get("delayed"), projects.size()))
            .aheadOfSchedule(statusCounts.get("ahead"))
            .percentageAhead(calculatePercentage(statusCounts.get("ahead"), projects.size()))
            .totalProjectWorkload(calculateTotalProjectWorkload(projectWorkload))
            .projectsWithWorkload(projectsWithWorkload)
            .projectWorkload(projectWorkload)
            .topOverloadedTalents(topOverloadedTalents)
            .topDelayedTalents(topDelayedTalents)
            .build();
    }
    
    private Map<String, Integer> calculateProjectStatuses(List<Project> projects, List<Task> tasks) {
        Map<String, Integer> statusCounts = new HashMap<>();
        statusCounts.put("onSchedule", 0);
        statusCounts.put("delayed", 0);
        statusCounts.put("ahead", 0);

        for (Project project : projects) {
            List<Task> projectTasks = tasks.stream()
                .filter(task -> task.getProject().getId().equals(project.getId()))
                .collect(Collectors.toList());

            if (projectTasks.isEmpty()) {
                continue;
            }

            int tasksAhead = 0;
            int tasksOnTrack = 0;
            int tasksDelayed = 0;

            for (Task task : projectTasks) {
                Optional<DailyProgress> latestProgressOpt = dailyProgressRepository.findByTask(task).stream()
                    .max(Comparator.comparing(DailyProgress::getDate));

                if (latestProgressOpt.isPresent()) {
                    DailyProgress latestProgress = latestProgressOpt.get();
                    String scheduleStatus = latestProgress.getScheduleStatus();

                    if (scheduleStatus != null) {
                        if (scheduleStatus.equalsIgnoreCase("ahead")) {
                            tasksAhead++;
                        } else if (scheduleStatus.equalsIgnoreCase("DELAYED")) {
                            tasksDelayed++;
                        } else {
                            tasksOnTrack++;
                        }
                    } else {
                        tasksOnTrack++;
                    }
                } else {
                    tasksOnTrack++;
                }
            }

            // Determine project status based on majority of task statuses
            if (tasksDelayed > tasksAhead && tasksDelayed > tasksOnTrack) {
                statusCounts.merge("delayed", 1, Integer::sum);
            } else if (tasksAhead > tasksDelayed && tasksAhead > tasksOnTrack) {
                statusCounts.merge("ahead", 1, Integer::sum);
            } else {
                statusCounts.merge("onSchedule", 1, Integer::sum);
            }
        }

        return statusCounts;
    }
    
    private Map<String, Map<String, Object>> calculateProjectWorkload(List<Project> projects, List<DailyProgress> progress) {
        // List untuk menampung hasil sementara
        List<Map.Entry<String, Map<String, Object>>> workloadList = new ArrayList<>();
    
        for (Project project : projects) {
            List<DailyProgress> projectProgress = progress.stream()
                .filter(p -> p.getTask().getProject().getId().equals(project.getId()))
                .collect(Collectors.toList());
    
            double totalEffortSpent = projectProgress.stream()
                .mapToDouble(p -> p.getTotalEffortSpent() != null ? p.getTotalEffortSpent() : 0.0)
                .sum();
    
            int uniqueDays = (int) projectProgress.stream()
                .map(DailyProgress::getDate)
                .distinct()
                .count();
    
            double avgDailyEffort = uniqueDays > 0 ? totalEffortSpent / uniqueDays : 0.0;
    
            // Calculate Start Project (earliest progress date)
            LocalDate startProject = projectProgress.stream()
                .map(DailyProgress::getDate)
                .min(LocalDate::compareTo)
                .orElse(null);
    
            // Calculate End Project (latest date when task is completed - progress = 100)
            LocalDate endProject = projectProgress.stream()
                .filter(p -> p.getProgress() != null && p.getProgress() >= 100)
                .map(DailyProgress::getDate)
                .max(LocalDate::compareTo)
                .orElse(null);
    
            Map<String, Object> workloadDetails = new HashMap<>();
            workloadDetails.put("id", project.getId());
            workloadDetails.put("totalEffortSpent", totalEffortSpent);
            workloadDetails.put("avgDailyEffort", avgDailyEffort);
            workloadDetails.put("uniqueDays", uniqueDays);
            workloadDetails.put("startProject", startProject);
            workloadDetails.put("endProject", endProject);
    
            workloadList.add(new AbstractMap.SimpleEntry<>(project.getName(), workloadDetails));
        }
    
        // Sort workloadList berdasarkan ID DESCENDING (5 project terbaru)
        workloadList = workloadList.stream()
            .sorted((a, b) -> {
                Long idA = (Long) a.getValue().get("id");
                Long idB = (Long) b.getValue().get("id");
                return idB.compareTo(idA); // DESCENDING - ID terbesar dulu
            })
            .limit(5)
            .collect(Collectors.toList());
    
        // Masukkan ke LinkedHashMap agar urutan tetap
        Map<String, Map<String, Object>> projectWorkload = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : workloadList) {
            projectWorkload.put(entry.getKey(), entry.getValue());
        }
    
        return projectWorkload;
    }
    
    private double calculatePercentage(Integer value, int total) {
    if (total == 0) return 0.0;
    double result = (double) value / total * 100;
    return Math.round(result * 100.0) / 100.0; // 2 angka di belakang koma
    }
    
    private int calculateTotalProjectWorkload(Map<String, Map<String, Object>> projectWorkload) {
        return projectWorkload.values().stream()
            .mapToInt(details -> ((Double) details.get("totalEffortSpent")).intValue())
            .sum();
    }
    
    private List<DashboardSummaryDTO.TopOverloadedTalent> getTopOverloadedTalents(List<Talent> talents, List<DailyProgress> progress) {
        return talents.stream()
            .map(talent -> {
                // Group progress by project for this talent
                Map<Project, List<DailyProgress>> projectProgressMap = progress.stream()
                    .filter(p -> p.getTalent().getId().equals(talent.getId()))
                    .collect(Collectors.groupingBy(p -> p.getTask().getProject()));

                // Find the project with the highest average daily effort
                Project maxProject = null;
                double maxAvgEffort = 0.0;
                int maxTotalEffort = 0;
                int maxTotalTasks = 0;
                for (Map.Entry<Project, List<DailyProgress>> entry : projectProgressMap.entrySet()) {
                    List<DailyProgress> projectProgress = entry.getValue();
                    double totalEffort = projectProgress.stream()
                        .mapToDouble(p -> p.getTotalEffortSpent() != null ? p.getTotalEffortSpent() : 0.0)
                        .sum();
                    long uniqueDays = projectProgress.stream()
                        .map(DailyProgress::getDate)
                        .distinct()
                        .count();
                    double avgDailyEffort = uniqueDays > 0 ? totalEffort / uniqueDays : 0.0;
                    int totalTasks = (int) projectProgress.stream()
                        .map(p -> p.getTask().getId())
                        .distinct()
                        .count();
                    if (avgDailyEffort > maxAvgEffort) {
                        maxAvgEffort = avgDailyEffort;
                        maxProject = entry.getKey();
                        maxTotalEffort = (int) totalEffort;
                        maxTotalTasks = totalTasks;
                    }
                }
                if (maxProject == null) {
                    return null; // No project for this talent
                }
                int availableCapacity = (int) (40 - maxAvgEffort); // 40 hours per week
                double workloadPercentage = (maxAvgEffort / 8.0) * 100; // Based on 8-hour workday
                return DashboardSummaryDTO.TopOverloadedTalent.builder()
                    .talentId(talent.getId())
                    .talentName(talent.getName() + " (" + maxProject.getName() + ")")
                    .totalTasks(maxTotalTasks)
                    .totalEffortSpent(maxTotalEffort)
                    .availableCapacity(availableCapacity)
                    .workloadPercentage(workloadPercentage)
                    .build();
            })
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(DashboardSummaryDTO.TopOverloadedTalent::getWorkloadPercentage).reversed())
            .limit(5)
            .collect(Collectors.toList());
    }
    
    private List<DashboardSummaryDTO.TopDelayedTalent> getTopDelayedTalents(List<Talent> talents, List<Task> tasks, List<DailyProgress> progress) {
        return talents.stream()
            .map(talent -> {
                // Find all tasks for this talent that have any progress with schedule_status='DELAYED'
                Set<Long> delayedTaskIds = progress.stream()
                    .filter(p -> p.getTalent().getId().equals(talent.getId()) && "DELAYED".equalsIgnoreCase(p.getScheduleStatus()))
                    .map(p -> p.getTask().getId())
                    .collect(Collectors.toSet());

                // Group delayed tasks by project name
                Map<String, Integer> delayedTasksByProject = tasks.stream()
                    .filter(task -> delayedTaskIds.contains(task.getId()))
                    .collect(Collectors.groupingBy(
                        task -> task.getProject().getName(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                    ));

                int totalDelayedTasks = delayedTasksByProject.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();

                return DashboardSummaryDTO.TopDelayedTalent.builder()
                    .talentId(talent.getId())
                    .talentName(talent.getName())
                    .totalDelayedTasks(totalDelayedTasks)
                    .delayedTasksByProject(delayedTasksByProject)
                    .build();
            })
            .sorted(Comparator.comparing(DashboardSummaryDTO.TopDelayedTalent::getTotalDelayedTasks).reversed())
            .limit(5)
            .collect(Collectors.toList());
    }
    
    private double calculateExpectedProgress(Task task) {
        if (task.getDeadline() == null) {
            return 0.0;
        }

        // Get the first progress entry date as start date
        LocalDate startDate = dailyProgressRepository.findByTask(task).stream()
            .map(DailyProgress::getDate)
            .min(LocalDate::compareTo)
            .orElse(task.getDeadline().minusDays(1)); // Default to 1 day before deadline if no progress

        // Calculate expected progress based on time elapsed
        long totalDays = ChronoUnit.DAYS.between(startDate, task.getDeadline());
        if (totalDays <= 0) {
            return 100.0; // If deadline is today or in the past, expect 100% completion
        }

        long daysElapsed = ChronoUnit.DAYS.between(startDate, LocalDate.now());
        return Math.min(100.0, (double) daysElapsed / totalDays * 100);
    }
    
    @Transactional(readOnly = true)
    public ProjectTimelineDTO getNewestProjects() {
        // Get all projects sorted by creation date (descending)
        List<Project> newestProjects = projectRepository.findAll().stream()
            .sorted(Comparator.comparing(Project::getCreatedAt).reversed())
            .limit(5)
            .collect(Collectors.toList());
        
        // Calculate progress for each project
        List<ProjectTimelineDTO.ProjectTimeline> projectTimelines = newestProjects.stream()
            .map(project -> {
                // Calculate overall project progress
                List<Task> tasks = taskRepository.findByProject(project);
                double progress = 0.0;
                String status = "on_track"; // Default status
                
                if (!tasks.isEmpty()) {
                    double totalProgress = 0.0;
                    int taskCount = 0;
                    
                    // Count schedule statuses
                    int aheadCount = 0;
                    int delayedCount = 0;
                    int onTrackCount = 0;
                    
                    for (Task task : tasks) {
                        List<DailyProgress> progressList = dailyProgressRepository.findByTask(task);
                        if (!progressList.isEmpty()) {
                            // Get the latest progress for this task
                            DailyProgress latestProgress = progressList.stream()
                                .max(Comparator.comparing(DailyProgress::getDate))
                                .orElse(null);
                            
                            if (latestProgress != null) {
                                // Add to progress calculation
                                if (latestProgress.getProgress() != null) {
                                    totalProgress += latestProgress.getProgress();
                                    taskCount++;
                                }
                                
                                // Count schedule statuses
                                String scheduleStatus = latestProgress.getScheduleStatus();
                                if (scheduleStatus != null) {
                                    if (scheduleStatus.equalsIgnoreCase("ahead")) {
                                        aheadCount++;
                                    } else if (scheduleStatus.equalsIgnoreCase("DELAYED")) {
                                        delayedCount++;
                                    } else {
                                        onTrackCount++;
                                    }
                                } else {
                                    onTrackCount++; // Default to on track if null
                                }
                            }
                        }
                    }
                    
                    // Calculate progress
                    if (taskCount > 0) {
                        progress = totalProgress / taskCount;
                        progress = Math.round(progress * 100.0) / 100.0;
                    }
                    
                    // Determine status based on majority
                    if (delayedCount > aheadCount && delayedCount > onTrackCount) {
                        status = "DELAYED";
                    } else if (aheadCount > delayedCount && aheadCount > onTrackCount) {
                        status = "ahead";
                    } else {
                        status = "on_track";
                    }
                }
                
                return ProjectTimelineDTO.ProjectTimeline.builder()
                    .projectName(project.getName())
                    .progress(progress)
                    .status(status)
                    .build();
            })
            .collect(Collectors.toList());
        
        return ProjectTimelineDTO.builder()
            .projectTimelines(projectTimelines)
            .build();
    }
}