package com.sttp.skripsi.service;

import com.sttp.skripsi.constant.ErrorMessage;
import com.sttp.skripsi.exception.AppException;
import com.sttp.skripsi.dto.TalentDetailDTO;
import com.sttp.skripsi.model.Talent;
import com.sttp.skripsi.model.User;
import com.sttp.skripsi.model.DailyProgress;
import com.sttp.skripsi.model.Task;
import com.sttp.skripsi.model.Project;
import com.sttp.skripsi.repository.TalentRepository;
import com.sttp.skripsi.repository.DailyProgressRepository;
import com.sttp.skripsi.repository.TaskRepository;
import com.sttp.skripsi.repository.ProjectRepository;
import com.sttp.skripsi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;
import java.time.temporal.WeekFields;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class TalentService {
    private final TalentRepository talentRepository;
    private final DailyProgressRepository dailyProgressRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    private static final int MAX_DAILY_EFFORT = 8; // Standard workday hours
    private static final int MAX_TASKS_PER_DAY = 3; // Maximum recommended tasks per day
    private static final int WARNING_THRESHOLD = 75; // Warning threshold percentage
    private static final int CRITICAL_THRESHOLD = 90; // Critical threshold percentage

    public List<Talent> getAllTalents() {
        return talentRepository.findAll();
    }

    public Optional<Talent> getTalentById(Long id) {
        return talentRepository.findById(id);
    }

    public Optional<Talent> getTalentByUserId(Long userId) {
        return talentRepository.findByUserId(userId);
    }

    @Transactional
    public Talent createTalent(String name, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound(ErrorMessage.USER_NOT_FOUND));

        Talent talent = Talent.builder()
                .name(name)
                .user(user)
                .isActive(true)
                .build();

        return talentRepository.save(talent);
    }

    @Transactional
    public Talent updateTalent(Long id, String name, boolean isActive) {
        Talent talent = talentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound(ErrorMessage.TALENT_NOT_FOUND));

        talent.setName(name);
        talent.setIsActive(isActive);

        return talentRepository.save(talent);
    }

    @Transactional
    public void deleteTalent(Long id) {
        talentRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public TalentDetailDTO getTalentDetail(Long id, String category, String status) {
        Talent talent = talentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound(ErrorMessage.TALENT_NOT_FOUND));
        
        User user = talent.getUser();
        List<DailyProgress> allProgress = dailyProgressRepository.findByTalent(talent);
        
        // Apply filters if provided
        if (category != null || status != null) {
            allProgress = allProgress.stream()
                .filter(dp -> {
                    boolean matchesCategory = category == null || 
                        (dp.getTask() != null && category.equalsIgnoreCase(dp.getTask().getTaskCategory()));
                    boolean matchesStatus = status == null || 
                        status.equalsIgnoreCase(mapProgressToStatus(dp.getProgress()));
                    return matchesCategory && matchesStatus;
                })
                .collect(Collectors.toList());
        }
        
        // Calculate workload alerts
        List<TalentDetailDTO.WorkloadAlert> workloadAlerts = calculateWorkloadAlerts(allProgress);
        
        // Calculate performance overview
        int totalTasks = allProgress.size();
        int taskEarly = (int) allProgress.stream()
                .filter(dp -> "ahead".equalsIgnoreCase(dp.getScheduleStatus()))
                .count();
        int taskOntime = (int) allProgress.stream()
                .filter(dp -> "on_track".equalsIgnoreCase(dp.getScheduleStatus()))
                .count();
        int taskDelayed = (int) allProgress.stream()
                .filter(dp -> "delayed".equalsIgnoreCase(dp.getScheduleStatus()))
                .count();

        TalentDetailDTO.PerformanceOverview performanceOverview = TalentDetailDTO.PerformanceOverview.builder()
                .taskEarly(taskEarly)
                .taskEarlyPercentage(totalTasks > 0 ? (taskEarly * 100.0 / totalTasks) : 0)
                .taskOntime(taskOntime)
                .taskOntimePercentage(totalTasks > 0 ? (taskOntime * 100.0 / totalTasks) : 0)
                .taskDelayed(taskDelayed)
                .taskDelayedPercentage(totalTasks > 0 ? (taskDelayed * 100.0 / totalTasks) : 0)
                .build();

        // Get task history from DailyProgress
        List<TalentDetailDTO.TaskHistory> taskHistory = allProgress.stream()
                .map(dp -> {
                    Task task = dp.getTask();
                    if (task == null) return null;
                    
                    String taskStatus = mapProgressToStatus(dp.getProgress());
                    String timeline = mapScheduleStatusToTimeline(dp.getScheduleStatus());
                    
                    return TalentDetailDTO.TaskHistory.builder()
                            .idTask(task.getId())
                            .taskCode(task.getTaskCode())
                            .taskName(task.getTaskName())
                            .project(task.getProject() != null ? task.getProject().getName() : "Unknown Project")
                            .taskCategory(task.getTaskCategory())
                            .date(dp.getDate())
                            .status(taskStatus)
                            .timeline(timeline)
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Calculate project performance
        List<TalentDetailDTO.ProjectPerformance> projectPerformance = allProgress.stream()
                .filter(dp -> dp.getTask() != null && dp.getTask().getProject() != null)
                .collect(Collectors.groupingBy(dp -> dp.getTask().getProject()))
                .entrySet().stream()
                .map(entry -> {
                    Project project = entry.getKey();
                    List<DailyProgress> projectProgress = entry.getValue();

                    int totalProjectTasks = projectProgress.size();
                    int completedTasks = (int) projectProgress.stream()
                            .filter(dp -> mapProgressToStatus(dp.getProgress()).equals("completed"))
                            .count();
                    int inProgressTasks = (int) projectProgress.stream()
                            .filter(dp -> mapProgressToStatus(dp.getProgress()).equals("inprogress"))
                            .count();
                    int notStartedTasks = (int) projectProgress.stream()
                            .filter(dp -> mapProgressToStatus(dp.getProgress()).equals("notstarted"))
                            .count();

                    double completionPercentage = totalProjectTasks > 0 ?
                            (completedTasks * 100.0 / totalProjectTasks) : 0;

                    return TalentDetailDTO.ProjectPerformance.builder()
                            .projectId(project.getId())
                            .projectName(project.getName())
                            .totalTasks(totalProjectTasks)
                            .completedTasks(completedTasks)
                            .inProgressTasks(inProgressTasks)
                            .notStartedTasks(notStartedTasks)
                            .completionPercentage(Math.round(completionPercentage * 100) / 100.0)
                            .build();
                })
                .collect(Collectors.toList());

        // Get projects from DailyProgress
        List<String> projects = allProgress.stream()
                .filter(dp -> dp.getTask() != null && dp.getTask().getProject() != null)
                .map(dp -> dp.getTask().getProject().getName())
                .distinct()
                .collect(Collectors.toList());

        // Get project IDs from DailyProgress
        List<Long> projectIds = allProgress.stream()
                .filter(dp -> dp.getTask() != null && dp.getTask().getProject() != null)
                .map(dp -> dp.getTask().getProject().getId())
                .distinct()
                .collect(Collectors.toList());

        String role = "USER";
        String email = null;
        LocalDateTime lastLogin = null;

        if (user != null) {
            email = user.getEmail();
            lastLogin = user.getLastLogin();
        }

        return TalentDetailDTO.builder()
                .id(talent.getId())
                .fullName(talent.getName())
                .overloaded(calculateOverloadedStatus(allProgress))
                .role(role)
                .email(email)
                .createdAt(talent.getCreatedAt())
                .lastLogin(lastLogin)
                .projects(projects)
                .projectIds(projectIds)
                .performanceOverview(performanceOverview)
                .taskHistory(taskHistory)
                .projectPerformance(projectPerformance)
                .workloadAlerts(workloadAlerts)
                .build();
    }

    private String mapProgressToStatus(Integer progress) {
        if (progress == null) return "notstarted";
        if (progress >= 100) return "completed";
        if (progress > 0) return "inprogress";
        return "notstarted";
    }

    private boolean calculateOverloadedStatus(List<DailyProgress> progress) {
        // Implementasi logika untuk menentukan apakah talent overloaded
        // Contoh sederhana: jika total effort spent > 80% dari kapasitas
        int totalEffort = progress.stream()
                .mapToInt(DailyProgress::getTotalEffortSpent)
                .sum();
        return totalEffort > 80;
    }

    @Transactional
    public void clearDatabase() {
        // Hapus data dari tabel-tabel yang saling terhubung
        // Urutan penghapusan penting karena foreign key constraints
        dailyProgressRepository.deleteAll();
        taskRepository.deleteAll();
        talentRepository.deleteAll();
        projectRepository.deleteAll();
    }

    @Transactional(readOnly = true)
    public Map<String, TalentDetailDTO.MonthlyPerformance> getPerformanceTrends(Long talentId, String period) {
        Talent talent = talentRepository.findById(talentId)
                .orElseThrow(() -> AppException.notFound(ErrorMessage.TALENT_NOT_FOUND));
        
        List<DailyProgress> allProgress = dailyProgressRepository.findByTalent(talent);
        Map<String, TalentDetailDTO.MonthlyPerformance> trends = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();
        
        if ("this_week".equalsIgnoreCase(period)) {
            // Filter for current month's data only
            List<DailyProgress> currentMonthProgress = allProgress.stream()
                .filter(dp -> dp.getDate().getMonth() == now.getMonth() && 
                             dp.getDate().getYear() == now.getYear())
                .collect(Collectors.toList());
                
            // Group by week number for current month only
            currentMonthProgress.stream()
                .collect(Collectors.groupingBy(dp -> 
                    "Week" + dp.getDate().get(WeekFields.ISO.weekOfMonth())))
                .forEach((week, weeklyProgress) -> {
                    int total = weeklyProgress.size();
                    int early = (int) weeklyProgress.stream()
                            .filter(dp -> "ahead".equalsIgnoreCase(dp.getScheduleStatus()))
                            .count();
                    int ontime = (int) weeklyProgress.stream()
                            .filter(dp -> "on_track".equalsIgnoreCase(dp.getScheduleStatus()))
                            .count();
                    int delayed = (int) weeklyProgress.stream()
                            .filter(dp -> "delayed".equalsIgnoreCase(dp.getScheduleStatus()))
                            .count();
                    
                    trends.put(week, TalentDetailDTO.MonthlyPerformance.builder()
                            .taskEarlyPercentage(total > 0 ? (early * 100.0 / total) : 0)
                            .taskOntimePercentage(total > 0 ? (ontime * 100.0 / total) : 0)
                            .taskDelayedPercentage(total > 0 ? (delayed * 100.0 / total) : 0)
                            .build());
                });
        } else if ("this_month".equalsIgnoreCase(period)) {
            // Group by month
            allProgress.stream()
                .collect(Collectors.groupingBy(dp -> dp.getDate().getMonth().toString()))
                .forEach((month, monthlyProgress) -> {
                    int total = monthlyProgress.size();
                    int early = (int) monthlyProgress.stream()
                            .filter(dp -> "ahead".equalsIgnoreCase(dp.getScheduleStatus()))
                            .count();
                    int ontime = (int) monthlyProgress.stream()
                            .filter(dp -> "on_track".equalsIgnoreCase(dp.getScheduleStatus()))
                            .count();
                    int delayed = (int) monthlyProgress.stream()
                            .filter(dp -> "delayed".equalsIgnoreCase(dp.getScheduleStatus()))
                            .count();
                    
                    // Format to 2 decimal places
                    double earlyPercentage = total > 0 ? (early * 100.0 / total) : 0;
                    double ontimePercentage = total > 0 ? (ontime * 100.0 / total) : 0;
                    double delayedPercentage = total > 0 ? (delayed * 100.0 / total) : 0;
                    
                    trends.put(month, TalentDetailDTO.MonthlyPerformance.builder()
                            .taskEarlyPercentage(Math.round(earlyPercentage * 100) / 100.0)
                            .taskOntimePercentage(Math.round(ontimePercentage * 100) / 100.0)
                            .taskDelayedPercentage(Math.round(delayedPercentage * 100) / 100.0)
                            .build());
                });
        }
        
        return trends;
    }

    private String mapScheduleStatusToTimeline(String scheduleStatus) {
        if (scheduleStatus == null) return "Unknown";
        
        switch (scheduleStatus.toLowerCase()) {
            case "ahead":
                return "Ahead of Schedule";
            case "on_track":
                return "On Track";
            case "delayed":
                return "Delayed";
            default:
                return "Unknown";
        }
    }

    // Add a new method to handle the original call without filters
    @Transactional(readOnly = true)
    public TalentDetailDTO getTalentDetail(Long id) {
        return getTalentDetail(id, null, null);
    }

    private List<TalentDetailDTO.WorkloadAlert> calculateWorkloadAlerts(List<DailyProgress> progress) {
        List<TalentDetailDTO.WorkloadAlert> alerts = new ArrayList<>();
        
        // Group progress by project
        Map<Project, List<DailyProgress>> progressByProject = progress.stream()
            .filter(dp -> dp.getTask() != null && dp.getTask().getProject() != null)
            .collect(Collectors.groupingBy(dp -> dp.getTask().getProject()));

        for (Map.Entry<Project, List<DailyProgress>> entry : progressByProject.entrySet()) {
            Project project = entry.getKey();
            List<DailyProgress> projectProgress = entry.getValue();

            // Calculate average daily effort and tasks per day
            Map<LocalDate, List<DailyProgress>> progressByDate = projectProgress.stream()
                .collect(Collectors.groupingBy(DailyProgress::getDate));

            double avgDailyEffort = progressByDate.entrySet().stream()
                .mapToDouble(dateEntry -> dateEntry.getValue().stream()
                    .mapToInt(DailyProgress::getAdditionalEffort)
                    .sum())
                .average()
                .orElse(0.0);

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
                alerts.add(TalentDetailDTO.WorkloadAlert.builder()
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
                alerts.add(TalentDetailDTO.WorkloadAlert.builder()
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
        }

        return alerts;
    }
}