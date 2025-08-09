package com.sttp.skripsi.repository;

import com.sttp.skripsi.model.Task;
import com.sttp.skripsi.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProject(Project project);
    Optional<Task> findByProjectAndTaskCode(Project project, String taskCode);
    boolean existsByProjectAndTaskCode(Project project, String taskCode);
    List<Task> findByPicName(String picName);

    @Query("SELECT DISTINCT t.taskCategory FROM Task t WHERE t.taskCategory IS NOT NULL")
    List<String> findDistinctTaskCategories();
}