package com.shareway.domain.repository;

import com.shareway.domain.model.RideRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RideRatingRepository extends JpaRepository<RideRating, String> {

    List<RideRating> findByToUserIdOrderByCreatedAtDesc(String toUserId);

    Optional<RideRating> findByRideRequestIdAndFromUserId(String rideRequestId, String fromUserId);

    @Query("SELECT AVG(r.rating) FROM RideRating r WHERE r.toUser.id = :userId")
    Optional<Double> avgRatingByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(r) FROM RideRating r WHERE r.toUser.id = :userId")
    long countByUserId(@Param("userId") String userId);
}
