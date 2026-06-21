package com.shareway.application.usecase;

import com.shareway.infrastructure.adapter.audit.domain.model.Booking;
import com.shareway.infrastructure.adapter.audit.domain.repository.BookingRepository;
import com.shareway.infrastructure.adapter.audit.domain.repository.ReviewRepository;
import com.shareway.infrastructure.adapter.audit.domain.repository.TripRepository;
import com.shareway.infrastructure.adapter.audit.domain.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardUseCase {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    /**
     * Dashboard conducteur
     */
    public DriverDashboard getDriverDashboard(String driverId) {
        var trips = tripRepository.findByDriverId(driverId);
        long total = trips.size();
        long completed = trips.stream().filter(t -> t.getStatus().name().equals("COMPLETED")).count();
        long cancelled = trips.stream().filter(t -> t.getStatus().name().equals("CANCELLED")).count();
        long totalPassengers = trips.stream().mapToLong(t -> t.getTotalSeats() - t.getAvailableSeats()).sum();
        double avgFill = trips.stream()
                .filter(t -> t.getStatus().name().equals("COMPLETED"))
                .mapToInt(t -> t.getTotalSeats() > 0 ? (t.getTotalSeats() - t.getAvailableSeats()) * 100 / t.getTotalSeats() : 0)
                .average().orElse(0);
        Double avgRating = reviewRepository.getAverageRating(driverId);

        LocalDate now = LocalDate.now();
        BigDecimal monthlyEarnings = bookingRepository.sumEarningsByDriver(
                driverId, now.getMonthValue(), now.getYear());

        return DriverDashboard.builder()
                .totalTrips(total).completedTrips(completed).cancelledTrips(cancelled)
                .totalPassengers(totalPassengers).averageFillRate(avgFill)
                .averageRating(avgRating != null ? avgRating : 0.0)
                .monthlyEarnings(monthlyEarnings != null ? monthlyEarnings : BigDecimal.ZERO)
                .build();
    }

    /**
     * Dashboard passager
     */
    public PassengerDashboard getPassengerDashboard(String passengerId) {
        var bookings = bookingRepository.findByPassengerIdAndDeletedAtIsNull(passengerId);
        long total = bookings.size();
        long completed = bookings.stream().filter(b -> b.getStatus().name().equals("COMPLETED")).count();
        long cancelled = bookings.stream().filter(b -> b.getStatus().name().equals("CANCELLED")).count();
        BigDecimal totalSpent = bookings.stream()
                .filter(b -> b.getAmountPaid() != null)
                .map(Booking::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PassengerDashboard.builder()
                .totalBookings(total).completedTrips(completed).cancelledTrips(cancelled)
                .totalSpent(totalSpent)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverDashboard {
        private long totalTrips, completedTrips, cancelledTrips, totalPassengers;
        private double averageFillRate, averageRating;
        private BigDecimal monthlyEarnings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerDashboard {
        private long totalBookings, completedTrips, cancelledTrips;
        private BigDecimal totalSpent;
    }
}
