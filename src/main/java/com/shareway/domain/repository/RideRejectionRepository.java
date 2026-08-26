package com.shareway.domain.repository;

import com.shareway.domain.model.RideRejection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RideRejectionRepository extends JpaRepository<RideRejection, String> {
    List<RideRejection> findByDriverIdOrderByCreatedAtDesc(String driverId);
}
