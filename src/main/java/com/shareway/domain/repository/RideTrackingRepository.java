package com.shareway.domain.repository;

import com.shareway.domain.model.RideTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RideTrackingRepository extends JpaRepository<RideTracking, String> {

    List<RideTracking> findByRideRequestIdOrderByRecordedAtAsc(String rideRequestId);

    List<RideTracking> findTop1ByRideRequestIdOrderByRecordedAtDesc(String rideRequestId);
}
