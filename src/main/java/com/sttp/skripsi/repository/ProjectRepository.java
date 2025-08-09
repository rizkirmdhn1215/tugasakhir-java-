package com.sttp.skripsi.repository;

import com.sttp.skripsi.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByName(String name);
    Optional<Project> findBySheetId(String sheetId);
    boolean existsByName(String name);
    boolean existsBySheetId(String sheetId);
} 