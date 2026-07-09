package com.shareway.domain.service;

import com.shareway.domain.model.Booking;
import com.shareway.domain.model.Penalty;
import com.shareway.domain.model.Trip;
import com.shareway.domain.model.User;
import com.shareway.domain.repository.PenaltyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PenaltyService {

    private final PenaltyRepository penaltyRepository;

    private static final BigDecimal LATE_CANCELLATION_RATE = new BigDecimal("0.50");

    public Penalty assessLateCancellation(Booking booking, User user) {
        Trip trip = booking.getTrip();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime departure = trip.getDepartureTime();

        Duration duration = Duration.between(now, departure);
        if (duration.isNegative() || duration.toHours() > 2) {
            return null;
        }

        BigDecimal penaltyAmount = booking.getAmountPaid().multiply(LATE_CANCELLATION_RATE);
        if (penaltyAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        Penalty penalty = Penalty.builder()
                .user(user)
                .bookingId(booking.getId())
                .tripId(trip.getId())
                .type(Penalty.PenaltyType.LATE_CANCELLATION)
                .amount(penaltyAmount)
                .currency(booking.getCurrency().name())
                .reason("Annulation moins de 2h avant le départ. Pénalité de 50% du montant.")
                .paid(false)
                .build();

        return penaltyRepository.save(penalty);
    }

    public Penalty assessNoShow(Trip trip, Booking booking) {
        BigDecimal penaltyAmount = booking.getAmountPaid();
        if (penaltyAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        Penalty penalty = Penalty.builder()
                .user(booking.getPassenger())
                .bookingId(booking.getId())
                .tripId(trip.getId())
                .type(Penalty.PenaltyType.NO_SHOW)
                .amount(penaltyAmount)
                .currency(booking.getCurrency().name())
                .reason("Absence non justifiée. Pénalité de 100% du montant.")
                .paid(false)
                .build();

        return penaltyRepository.save(penalty);
    }
}
