package com.shareway.infrastructure.adapter.audit.domain.repository;

import com.shareway.infrastructure.adapter.audit.domain.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {

    boolean existsByTripIdAndAuthorIdAndTargetUserId(String tripId, String authorId, String targetUserId);

    /**
     * Avis approuvés reçus par un utilisateur (sa page profil)
     */
    @Query("SELECT r FROM Review r WHERE r.targetUser.id = :userId AND r.deletedAt IS NULL " +
            "AND r.approved = true ORDER BY r.createdAt DESC")
    Page<Review> findApprovedByTarget(@Param("userId") String userId, Pageable pageable);

    /**
     * Historique des avis laissés par l'auteur
     */
    @Query("SELECT r FROM Review r WHERE r.author.id = :authorId AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    List<Review> findByAuthorIdAndDeletedAtIsNull(@Param("authorId") String authorId);

    /**
     * Avis signalés – modération admin
     */
    @Query("SELECT r FROM Review r WHERE r.flagged = true AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    Page<Review> findFlagged(Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.targetUser.id = :userId " +
            "AND r.deletedAt IS NULL AND r.approved = true")
    Double getAverageRating(@Param("userId") String userId);
}
