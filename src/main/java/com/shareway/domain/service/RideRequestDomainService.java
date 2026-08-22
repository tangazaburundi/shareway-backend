package com.shareway.domain.service;

import com.shareway.domain.exception.InvalidRideStateException;
import com.shareway.domain.model.DriverAvailability;
import com.shareway.domain.model.RideRequest;
import com.shareway.domain.repository.DriverAvailabilityRepository;
import com.shareway.domain.repository.RideRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideRequestDomainService {

    private final RideRequestRepository rideRequestRepository;
    private final DriverAvailabilityRepository driverAvailabilityRepository;

    /**
     * Valide qu'une transition de statut est autorisée.
     */
    public void validateTransition(RideRequest ride, RideRequest.RideStatus targetStatus) {
        boolean valid = switch (targetStatus) {
            case DRIVER_FOUND -> ride.getStatus() == RideRequest.RideStatus.SEARCHING;
            case ACCEPTED -> ride.getStatus() == RideRequest.RideStatus.DRIVER_FOUND
                    || ride.getStatus() == RideRequest.RideStatus.SEARCHING;
            case DRIVER_EN_ROUTE -> ride.getStatus() == RideRequest.RideStatus.ACCEPTED;
            case ARRIVED -> ride.getStatus() == RideRequest.RideStatus.DRIVER_EN_ROUTE;
            case IN_PROGRESS -> ride.getStatus() == RideRequest.RideStatus.ARRIVED;
            case COMPLETED -> ride.getStatus() == RideRequest.RideStatus.IN_PROGRESS;
            case CANCELLED -> ride.isActive();
            case EXPIRED -> ride.getStatus() == RideRequest.RideStatus.SEARCHING;
            default -> false;
        };

        if (!valid) {
            throw new InvalidRideStateException(
                    "Transition de " + ride.getStatus() + " vers " + targetStatus + " non autorisée");
        }
    }

    /**
     * Attribue un chauffeur à une demande.
     */
    @Transactional
    public void assignDriver(RideRequest ride, DriverAvailability driver) {
        ride.setDriver(driver.getUser());
        ride.setStatus(RideRequest.RideStatus.DRIVER_FOUND);
        ride.setDriverNotifiedAt(java.time.LocalDateTime.now());
        driver.setBusy();
        driverAvailabilityRepository.save(driver);
        rideRequestRepository.save(ride);
    }

    /**
     * Met à jour la disponibilité du chauffeur après annulation.
     */
    @Transactional
    public void releaseDriver(String driverId) {
        driverAvailabilityRepository.findByDriverId(driverId).ifPresent(driver -> {
            driver.setAvailable();
            driverAvailabilityRepository.save(driver);
        });
    }
}
