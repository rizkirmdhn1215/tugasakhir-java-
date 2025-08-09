package com.sttp.skripsi.service;

import com.sttp.skripsi.constant.ErrorMessage;
import com.sttp.skripsi.dto.*;
import com.sttp.skripsi.exception.AppException;
import com.sttp.skripsi.model.*;
import com.sttp.skripsi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class ProjectProgressService {
    private static final int MAX_WORKLOAD = 100;
    
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final DailyProgressRepository dailyProgressRepository;
    private final TalentRepository talentRepository;

    @Transactional(readOnly = true)
    public ProjectProgressDTO getProjectProgress(Long projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> AppException.notFound(ErrorMessage.PROJECT_NOT_FOUND));

        List<Task> projectTasks = taskRepository.findByProject(project);
        
        // Get all talents who have progress records for this project's tasks
        Set<Talent> talents = projectTasks.stream()
            .flatMap(task -> dailyProgressRepository.findByTask(task).stream())
            .map(DailyProgress::getTalent)
            .collect(Collectors.toSet());

        // Calculate overall project metrics
        Map<Task, DailyProgress> latestProgressByTask = new HashMap<>();
        for (Task task : projectTasks) {
            dailyProgressRepository.findByTask(task).stream()
                .max(Comparator.comparing(DailyProgress::getDate))
                .ifPresent(dp -> latestProgressByTask.put(task, dp));
        }

        double overallProgress = latestProgressByTask.values().stream()
            .mapToInt(DailyProgress::getProgress)
            .average()
            .orElse(0.0);

        // Count delayed tasks
        Map<String, Integer> delayedTasksCount = latestProgressByTask.entrySet().stream()
            .filter(entry -> "DELAYED".equals(entry.getValue().getScheduleStatus()))
            .collect(Collectors.groupingBy(
                entry -> entry.getKey().getTaskCategory(),
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));

        // Calculate talent-specific metrics
        List<ProjectProgressDTO.TalentProgress> talentProgresses = new ArrayList<>();
        
        for (Talent talent : talents) {
            Map<Task, DailyProgress> talentLatestProgress = new HashMap<>();
            
            for (Task task : projectTasks) {
                dailyProgressRepository.findByTalentAndTask(talent, task).stream()
                    .max(Comparator.comparing(DailyProgress::getDate))
                    .ifPresent(dp -> talentLatestProgress.put(task, dp));
            }

            List<ProjectProgressDTO.TaskProgress> taskProgresses = talentLatestProgress.values().stream()
                .map(dp -> ProjectProgressDTO.TaskProgress.builder()
                    .taskCode(dp.getTask().getTaskCode())
                    .taskName(dp.getTask().getTaskName())
                    .currentProgress(dp.getProgress())
                    .deadline(dp.getTask().getDeadline())
                    .estimatedCompletionDate(dp.getEstimatedCompletionDate())
                    .scheduleStatus(dp.getScheduleStatus())
                    .delayReason(dp.getDelayReason())
                    .baseEstimate(dp.getBaseEstimate())
                    .totalEffortSpent(dp.getTotalEffortSpent())
                    .build())
                .collect(Collectors.toList());

            int completedTasks = (int) talentLatestProgress.values().stream()
                .filter(dp -> dp.getProgress() >= 100)
                .count();

            int delayedTasks = (int) talentLatestProgress.values().stream()
                .filter(dp -> "DELAYED".equals(dp.getScheduleStatus()))
                .count();

            double averageProgress = talentLatestProgress.values().stream()
                .mapToInt(DailyProgress::getProgress)
                .average()
                .orElse(0.0);

            talentProgresses.add(ProjectProgressDTO.TalentProgress.builder()
                .talentName(talent.getName())
                .assignedTasks(talentLatestProgress.size())
                .averageProgress(averageProgress)
                .completedTasks(completedTasks)
                .delayedTasks(delayedTasks)
                .onTrackTasks(talentLatestProgress.size() - completedTasks - delayedTasks)
                .taskDetails(taskProgresses)
                .build());
        }

        return ProjectProgressDTO.builder()
            .projectName(project.getName())
            .sheetId(project.getSheetId())
            .totalTasks(projectTasks.size())
            .overallProjectProgress(overallProgress)
            .delayedTasksCount(delayedTasksCount)
            .talentProgresses(talentProgresses)
            .build();
    }
    @Transactional(readOnly = true)
    public ProjectSummaryDTO getAllProjectsSummary() {
        List<Project> allProjects = projectRepository.findAll();
        
        if (allProjects.isEmpty()) {
            return ProjectSummaryDTO.builder()
                    .totalProjects(0)
                    .onScheduleProjects(0)
                    .aheadScheduleProjects(0)
                    .delayedProjects(0)
                    .projectDetails(Collections.emptyList())
                    .build();
        }

        List<ProjectSummaryDTO.ProjectDetail> projectDetails = new ArrayList<>();
        int onScheduleCount = 0;
        int aheadScheduleCount = 0;
        int delayedCount = 0;

        for (Project project : allProjects) {
            List<Task> projectTasks = taskRepository.findByProject(project);
            if (projectTasks.isEmpty()) {
                continue;
            }
            int totalTasks = projectTasks.size();
            int tasksDone = 0;
            int tasksAhead = 0;
            int tasksOnTrack = 0;
            int tasksDelayed = 0;
            LocalDate dueDate = null;
            for (Task task : projectTasks) {
                List<DailyProgress> taskProgressList = dailyProgressRepository.findByTask(task);
                // Cek apakah task pernah ahead
                boolean everAhead = taskProgressList.stream()
                    .anyMatch(dp -> "ahead".equalsIgnoreCase(dp.getScheduleStatus()));
                if (everAhead) {
                    tasksAhead++;
                }
                // Find earliest date for due date calculation
                if (!taskProgressList.isEmpty()) {
                    LocalDate taskEarliestDate = taskProgressList.stream()
                        .map(DailyProgress::getDate)
                        .min(LocalDate::compareTo)
                        .orElse(null);
                    if (taskEarliestDate != null && (dueDate == null || taskEarliestDate.isBefore(dueDate))) {
                        dueDate = taskEarliestDate;
                    }
                    // Get latest progress for task status
                    Optional<DailyProgress> latestProgressOpt = taskProgressList.stream()
                        .max(Comparator.comparing(DailyProgress::getDate));
                    if (latestProgressOpt.isPresent()) {
                        DailyProgress latestProgress = latestProgressOpt.get();
                        if (latestProgress.getProgress() != null && latestProgress.getProgress() >= 100) {
                            tasksDone++;
                        }
                        String scheduleStatus = latestProgress.getScheduleStatus();
                        if (scheduleStatus != null) {
                            if (scheduleStatus.equalsIgnoreCase("ON_TRACK")) {
                                tasksOnTrack++;
                            } else if (scheduleStatus.equalsIgnoreCase("DELAYED")) {
                                tasksDelayed++;
                            }
                        } else {
                            tasksOnTrack++;
                        }
                    }
                } else {
                    // Task tanpa progress dianggap on track
                    tasksOnTrack++;
                }
            }
            if (dueDate == null) {
                dueDate = LocalDate.now();
            }
            double progress = totalTasks > 0 ? (double) tasksDone / totalTasks * 100 : 0;
            // Format progress to 2 decimal places
            progress = Math.round(progress * 100.0) / 100.0;

            String projectStatus;
            if (tasksDelayed > tasksAhead && tasksDelayed > tasksOnTrack) {
                projectStatus = "DELAYED";
                delayedCount++;
            } else if (tasksAhead > tasksDelayed && tasksAhead > tasksOnTrack) {
                projectStatus = "AHEAD";
                aheadScheduleCount++;
            } else {
                projectStatus = "ON_TRACK";
                onScheduleCount++;
            }

            ProjectSummaryDTO.ScheduleStatus scheduleStatus = ProjectSummaryDTO.ScheduleStatus.builder()
                    .tasksAhead(tasksAhead)
                    .tasksOnTrack(tasksOnTrack)
                    .tasksDelayed(tasksDelayed)
                    .build();

            ProjectSummaryDTO.ProjectDetail detail = ProjectSummaryDTO.ProjectDetail.builder()
                    .id(project.getId())
                    .projectName(project.getName())
                    .progress(progress)
                    .tasksDone(tasksDone)
                    .totalTasks(totalTasks)
                    .scheduleStatus(scheduleStatus)
                    .dueDate(dueDate)
                    .projectStatus(projectStatus)  // Added this line
                    .build();
            projectDetails.add(detail);
        }

        return ProjectSummaryDTO.builder()
                .totalProjects(allProjects.size())
                .onScheduleProjects(onScheduleCount)
                .aheadScheduleProjects(aheadScheduleCount)
                .delayedProjects(delayedCount)
                .projectDetails(projectDetails)
                .build();
    }

    @Transactional(readOnly = true)
    public ProjectSummaryDTO getDelayedProjects() {
        ProjectSummaryDTO allProjects = getAllProjectsSummary();
        List<ProjectSummaryDTO.ProjectDetail> delayedProjects = allProjects.getProjectDetails().stream()
                .filter(project -> "DELAYED".equals(project.getProjectStatus()))
                .collect(Collectors.toList());
        
        return ProjectSummaryDTO.builder()
                .totalProjects(delayedProjects.size())
                .delayedProjects(delayedProjects.size())
                .projectDetails(delayedProjects)
                .build();
    }

    @Transactional(readOnly = true)
    public ProjectSummaryDTO getOnScheduleProjects() {
        ProjectSummaryDTO allProjects = getAllProjectsSummary();
        List<ProjectSummaryDTO.ProjectDetail> onScheduleProjects = allProjects.getProjectDetails().stream()
                .filter(project -> "ON_TRACK".equals(project.getProjectStatus()))
                .collect(Collectors.toList());
        
        return ProjectSummaryDTO.builder()
                .totalProjects(onScheduleProjects.size())
                .onScheduleProjects(onScheduleProjects.size())
                .projectDetails(onScheduleProjects)
                .build();
    }

    @Transactional(readOnly = true)
    public ProjectSummaryDTO getAheadProjects() {
        ProjectSummaryDTO allProjects = getAllProjectsSummary();
        List<ProjectSummaryDTO.ProjectDetail> aheadProjects = allProjects.getProjectDetails().stream()
                .filter(project -> "AHEAD".equals(project.getProjectStatus()))
                .collect(Collectors.toList());
        
        return ProjectSummaryDTO.builder()
                .totalProjects(aheadProjects.size())
                .aheadScheduleProjects(aheadProjects.size())
                .projectDetails(aheadProjects)
                .build();
    }

    @Transactional(readOnly = true)
    public List<String> getAllTaskCategories() {
        return taskRepository.findDistinctTaskCategories();
    }

    @Transactional(readOnly = true)
    public ProjectDetailDTO getProjectDetail(Long projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> AppException.notFound(ErrorMessage.PROJECT_NOT_FOUND));
    
        List<Task> projectTasks = taskRepository.findByProject(project);
        List<DailyProgress> allProgress = dailyProgressRepository.findByTaskIn(projectTasks);
    
        // Get earliest date from all progress records
        LocalDate createdDate = allProgress.stream()
            .map(DailyProgress::getDate)
            .min(LocalDate::compareTo)
            .orElse(LocalDate.now());
    
        // Calculate schedule status counts
        Map<Task, DailyProgress> latestProgressByTask = new HashMap<>();
        for (Task task : projectTasks) {
            dailyProgressRepository.findByTask(task).stream()
                .max(Comparator.comparing(DailyProgress::getDate))
                .ifPresent(dp -> latestProgressByTask.put(task, dp));
        }
    
        int tasksAhead = 0;
        int tasksOnTrack = 0;
        int tasksDelayed = 0;
        int completedTasks = 0;
        int inProgressTasks = 0;
        int notStartedTasks = 0;
    
        for (DailyProgress progress : latestProgressByTask.values()) {
            String status = progress.getScheduleStatus();
            if ("AHEAD".equalsIgnoreCase(status)) {
                tasksAhead++;
            } else if ("DELAYED".equalsIgnoreCase(status)) {
                tasksDelayed++;
            } else {
                tasksOnTrack++;
            }
    
            if (progress.getProgress() == null || progress.getProgress() == 0) {
                notStartedTasks++;
            } else if (progress.getProgress() >= 100) {
                completedTasks++;
            } else {
                inProgressTasks++;
            }
        }
    
        // Determine project status based on majority of task statuses
        String projectStatus;
        if (tasksDelayed > tasksAhead && tasksDelayed > tasksOnTrack) {
            projectStatus = "DELAYED";
        } else if (tasksAhead > tasksDelayed && tasksAhead > tasksOnTrack) {
            projectStatus = "AHEAD";
        } else {
            projectStatus = "ON_TRACK";
        }
    
        // Calculate team members statistics
        Set<Talent> talents = projectTasks.stream()
            .flatMap(task -> dailyProgressRepository.findByTask(task).stream())
            .map(DailyProgress::getTalent)
            .collect(Collectors.toSet());
    
        List<ProjectDetailDTO.TeamMember> teamMembers = talents.stream()
            .map(talent -> {
                // Create final references inside the lambda
                final Map<Task, DailyProgress> talentProgress = new HashMap<>();
                final AtomicReference<LocalDate> joiningDate = new AtomicReference<>();
                final AtomicInteger totalEffortSpent = new AtomicInteger(0);
                
                // Ubah logika perhitungan totalEffortSpent
                // Ambil semua progress record untuk talent ini
                List<DailyProgress> allTalentProgress = new ArrayList<>();
                for (Task task : projectTasks) {
                    List<DailyProgress> progressList = dailyProgressRepository.findByTalentAndTask(talent, task);
                    allTalentProgress.addAll(progressList);
                    
                    // Tetap ambil progress terbaru untuk keperluan lain
                    progressList.stream()
                        .max(Comparator.comparing(DailyProgress::getDate))
                        .ifPresent(latestDp -> {
                            talentProgress.put(task, latestDp);
                            // Update joining date if earlier
                            if (joiningDate.get() == null || latestDp.getDate().isBefore(joiningDate.get())) {
                                joiningDate.set(latestDp.getDate());
                            }
                        });
                }
                
                // Hitung total effort spent dari semua progress record
                totalEffortSpent.set(allTalentProgress.stream()
                    .filter(progressItem -> progressItem.getTotalEffortSpent() != null)
                    .mapToInt(DailyProgress::getTotalEffortSpent)
                    .sum());
                
                int ahead = 0;
                int onTrack = 0;
                int delayed = 0;
                int completed = 0;
    
                for (DailyProgress progress : talentProgress.values()) {
                    String status = progress.getScheduleStatus();
                    if ("AHEAD".equalsIgnoreCase(status)) {
                        ahead++;
                    } else if ("DELAYED".equalsIgnoreCase(status)) {
                        delayed++;
                    } else {
                        onTrack++;
                    }
    
                    if (progress.getProgress() != null && progress.getProgress() >= 100) {
                        completed++;
                    }
                }
    
                // Determine timeline
                String timeline;
                if (ahead > onTrack && ahead > delayed) {
                    timeline = "ahead";
                } else if (delayed > ahead && delayed > onTrack) {
                    timeline = "delayed"; 
                } else {
                    timeline = "on_track";
                }
    
                return ProjectDetailDTO.TeamMember.builder()
                    .name(talent.getName())
                    .talentId(talent.getId())  // Add talentId
                    .joiningDate(joiningDate.get())
                    .assignedTasks(talentProgress.size())
                    .completedTasks(completed)
                    .aheadTasks(ahead)
                    .onTrackTasks(onTrack)
                    .delayedTasks(delayed)
                    .totalEffortSpent(totalEffortSpent.get())
                    .timeline(timeline)
                    .build();
            })
            .collect(Collectors.toList());
    
        // Calculate effort tracking
        // Calculate effort tracking
        int totalEstimatedEffort = latestProgressByTask.values().stream()
            .mapToInt(dp -> dp.getFinalEstimate() != null ? dp.getFinalEstimate() : 0)
            .sum();
    
        int totalSpentEffort = latestProgressByTask.values().stream()
            .mapToInt(dp -> dp.getTotalEffortSpent() != null ? dp.getTotalEffortSpent() : 0)
            .sum();
    
        // Build task list
        List<ProjectDetailDTO.TaskDetail> taskList = latestProgressByTask.entrySet().stream()
            .map(entry -> {
                Task task = entry.getKey();
                DailyProgress progress = entry.getValue();
                
                String status;
                if (progress.getProgress() == null || progress.getProgress() == 0) {
                    status = "notstarted";
                } else if (progress.getProgress() >= 100) {
                    status = "completed";
                } else {
                    status = "inprogress";
                }
                Double formattedProgress = progress.getProgress() != null ? 
                    Math.round(progress.getProgress() * 100.0) / 100.0 : 0.0;
                
                return ProjectDetailDTO.TaskDetail.builder()
                    .taskName(task.getTaskName())
                    .taskId(task.getId())  // Add taskId
                    .category(task.getTaskCategory())
                    .assignee(task.getPicName())
                    .progress(formattedProgress)
                    .status(status)
                    .build();
            })
            .collect(Collectors.toList());
    
        // Calculate overall progress
        double progress = latestProgressByTask.values().stream()
            .mapToInt(dp -> dp.getProgress() != null ? dp.getProgress() : 0)
            .average()
            .orElse(0.0);
    
        progress = Math.round(progress * 100.0) / 100.0;

        return ProjectDetailDTO.builder()
            .projectName(project.getName())
            .projectStatus(projectStatus)
            .projectId(project.getId())
            .createdDate(createdDate)
            .totalTasks(projectTasks.size())
            .progress(progress)
            .scheduleStatus(ProjectDetailDTO.ScheduleStatus.builder()
                .tasksAhead(tasksAhead)
                .tasksOnTrack(tasksOnTrack)
                .tasksDelayed(tasksDelayed)
                .build())
            .taskStatus(ProjectDetailDTO.TaskStatus.builder()
                .completed(completedTasks)
                .inProgress(inProgressTasks)
                .notStarted(notStartedTasks)
                .build())
            .effortTracking(ProjectDetailDTO.EffortTracking.builder()
                .totalEstimatedEffort(totalEstimatedEffort)
                .totalSpentEffort(totalSpentEffort)
                .remainingEffort(totalEstimatedEffort - totalSpentEffort)
                .build())
            .currentWorkload(ProjectDetailDTO.CurrentWorkload.builder()
                .totalTeamMembers(talents.size())
                .averageTasksPerMember(Math.round((projectTasks.size() / (double) talents.size()) * 100.0) / 100.0)
                .build())
            .teamMembers(ProjectDetailDTO.TeamMembers.builder()
                .totalTeam(talents.size())
                .team(teamMembers)
                .build())
            .taskList(taskList)
            .build();
    }
}