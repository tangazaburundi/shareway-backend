package com.shareway.infrastructure.scheduler;

import com.shareway.application.usecase.RideUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RideAutoCancelScheduler {

    private final RideUseCase rideUseCase;

    /** Auto-annule les courses SEARCHING sans chauffeur après 3 min */
    @Scheduled(fixedRate = 60000)
    public void autoCancelExpiredSearchingRides() {
        int cancelled = rideUseCase.autoCancelExpiredSearchingRides();
        if (cancelled > 0) {
            log.info("Auto-cancelled {} expired SEARCHING ride(s)", cancelled);
        }
    }

    /** Auto-annule les courses DRIVER_FOUND non acceptées après 3 min */
    @Scheduled(fixedRate = 60000)
    public void autoCancelExpiredRides() {
        int cancelled = rideUseCase.autoCancelExpiredDriverFoundRides();
        if (cancelled > 0) {
            log.info("Auto-cancelled {} expired DRIVER_FOUND ride(s)", cancelled);
        }
    }
}
