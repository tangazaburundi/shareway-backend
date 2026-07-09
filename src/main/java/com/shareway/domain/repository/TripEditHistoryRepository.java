package com.shareway.domain.repository;

import com.shareway.domain.model.TripEditHistory;
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