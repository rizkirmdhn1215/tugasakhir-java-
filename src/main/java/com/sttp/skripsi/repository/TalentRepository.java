package com.sttp.skripsi.repository;

import com.sttp.skripsi.model.Talent;
import com.sttp.skripsi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TalentRepository extends JpaRepository<Talent, Long> {
    Optional<Talent> findByName(String name);
    boolean existsByName(String name);
    Optional<Talent> findByUser(User user);
    Optional<Talent> findByUserId(Long userId);
} 