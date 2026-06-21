package com.shareway.infrastructure.adapter.audit.domain.repository;

import com.shareway.infrastructure.adapter.audit.domain.model.TripEditHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripEditHistoryRepository extends JpaRepository<TripEditHistory, String> {

    /**
     * Toutes les modifications d'un trajet, ordre chronologique inverse
     */
    List<TripEditHistory> findByTripIdOrderByCreatedAtDesc(String tripId);
}