package com.sttp.skripsi.repository;

import com.sttp.skripsi.model.DailyProgress;
import com.sttp.skripsi.model.Task;
import com.sttp.skripsi.model.Talent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyProgressRepository extends JpaRepository<DailyProgress, Long> {
    List<DailyProgress> findByTalent(Talent talent);
    List<DailyProgress> findByTask(Task task);
    List<DailyProgress> findByDate(LocalDate date);
    List<DailyProgress> findByTalentAndDateBetween(Talent talent, LocalDate startDate, LocalDate endDate);
    Optional<DailyProgress> findByTalentAndTaskAndDate(Talent talent, Task task, LocalDate date);
    boolean existsByTalentAndTaskAndDate(Talent talent, Task task, LocalDate date);
    List<DailyProgress> findByTalentAndTask(Talent talent, Task task);
    List<DailyProgress> findByTaskIn(List<Task> tasks);
    Optional<DailyProgress> findFirstByTaskIdOrderByDateDesc(Long taskId);
} 