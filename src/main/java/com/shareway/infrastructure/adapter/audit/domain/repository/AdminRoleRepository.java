package com.shareway.infrastructure.adapter.audit.domain.repository;

import com.shareway.infrastructure.adapter.audit.domain.model.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRoleRepository extends JpaRepository<AdminRole, String> {
    Optional<AdminRole> findByUserId(String userId);
}