package com.shareway.domain.repository;

import com.shareway.domain.model.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, String> {

    List<EmergencyContact> findByUserIdAndActiveTrueOrderByCreatedAtDesc(String userId);

    List<EmergencyContact> findAllByUserIdOrderByCreatedAtDesc(String userId);

    long countByUserIdAndActiveTrue(String userId);
}
