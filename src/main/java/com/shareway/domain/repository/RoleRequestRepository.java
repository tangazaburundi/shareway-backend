package com.shareway.domain.repository;

import com.shareway.domain.model.RoleRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleRequestRepository extends JpaRepository<RoleRequest, String> {

    List<RoleRequest> findByUserIdOrderByCreatedAtDesc(String userId);

    List<RoleRequest> findByStatus(RoleRequest.Status status);

    Page<RoleRequest> findByStatusOrderByCreatedAtDesc(RoleRequest.Status status, Pageable pageable);

    Page<RoleRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
