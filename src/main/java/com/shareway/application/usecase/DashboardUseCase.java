package com.shareway.application.usecase;

import com.shareway.domain.model.Booking;
import com.shareway.domain.model.Trip;
import com.shareway.domain.repository.BookingRepository;
import com.shareway.domain.repository.ReviewRepository;
import com.shareway.domain.repository.TripRepository;
import com.shareway.domain.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
        long completed = bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.COMPLETED).count();
        long cancelled = bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.CANCELLED).count();
        long pending = bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.PENDING).count();
        long active = bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED).count();

        BigDecimal totalSpent = bookings.stream()
                .filter(b -> b.getAmountPaid() != null && b.getStatus() == Booking.BookingStatus.COMPLETED)
                .map(Booking::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> spentByCurrency = bookings.stream()
                .filter(b -> b.getAmountPaid() != null && b.getStatus() == Booking.BookingStatus.COMPLETED)
                .collect(Collectors.groupingBy(
                        b -> b.getCurrency() != null ? b.getCurrency().name() : "FBU",
                        Collectors.reducing(BigDecimal.ZERO, Booking::getAmountPaid, BigDecimal::add)
                ));

        List<RecentBooking> recent = bookings.stream()
                .sorted(Comparator.comparing(Booking::getCreatedAt).reversed())
                .limit(10)
                .map(b -> {
                    Trip t = b.getTrip();
                    return RecentBooking.builder()
                            .id(b.getId())
                            .tripId(t != null ? t.getId() : null)
                            .departureCity(t != null ? t.getDepartureCity() : null)
                            .arrivalCity(t != null ? t.getArrivalCity() : null)
                            .departureTime(t != null ? t.getDepartureTime() : null)
                            .status(b.getStatus().name())
                            .amountPaid(b.getAmountPaid())
                            .currency(b.getCurrency() != null ? b.getCurrency().name() : "FBU")
                            .createdAt(b.getCreatedAt())
                            .build();
                })
                .toList();

        return PassengerDashboard.builder()
                .totalBookings(total).completedTrips(completed).cancelledTrips(cancelled)
                .pendingBookings(pending).activeBookings(active)
                .totalSpent(totalSpent).totalSpentByCurrency(spentByCurrency)
                .recentBookings(recent)
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
        private long totalBookings, completedTrips, cancelledTrips, pendingBookings, activeBookings;
        private BigDecimal totalSpent;
        private Map<String, BigDecimal> totalSpentByCurrency;
        private List<RecentBooking> recentBookings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentBooking {
        private String id, tripId, departureCity, arrivalCity, status, currency;
        private LocalDateTime departureTime, createdAt;
        private BigDecimal amountPaid;
    }
}
