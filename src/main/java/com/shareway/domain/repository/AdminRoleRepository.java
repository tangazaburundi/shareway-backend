package com.shareway.domain.repository;

import com.shareway.domain.model.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRoleRepository extends JpaRepository<AdminRole, String> {
    Optional<AdminRole> findByUserId(String userId);

    @Query("SELECT a.userId FROM AdminRole a")
    List<String> findAllUserIds();
}