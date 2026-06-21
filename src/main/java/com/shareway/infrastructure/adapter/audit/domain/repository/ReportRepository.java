package com.shareway.infrastructure.adapter.audit.domain.repository;

import com.shareway.infrastructure.adapter.audit.domain.model.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, String> {
    Page<Report> findByStatusOrderByCreatedAtDesc(Report.ReportStatus status, Pageable pageable);

    Page<Report> findByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.status = 'PENDING'")
    long countPending();
}
