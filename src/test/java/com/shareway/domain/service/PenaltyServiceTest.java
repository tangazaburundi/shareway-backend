package com.shareway.domain.service;

import com.shareway.domain.model.Booking;
import com.shareway.domain.model.Penalty;
import com.shareway.domain.model.Trip;
import com.shareway.domain.model.User;
import com.shareway.domain.repository.PenaltyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PenaltyServiceTest {

    @Mock private PenaltyRepository penaltyRepository;
    @Captor private ArgumentCaptor<Penalty> penaltyCaptor;

    private PenaltyService penaltyService;

    @BeforeEach
    void setUp() {
        penaltyService = new PenaltyService(penaltyRepository);
    }

    @Test
    void assessLateCancellation_within2Hours_shouldCreatePenalty() {
        User user = User.builder().id("user-1").build();
        Trip trip = Trip.builder()
                .id("trip-1")
                .departureTime(LocalDateTime.now().plusHours(1))
                .build();
        Booking booking = Booking.builder()
                .id("booking-1")
                .trip(trip)
                .passenger(user)
                .amountPaid(new BigDecimal("20.00"))
                .currency(Trip.Currency.EUR)
                .build();

        when(penaltyRepository.save(any(Penalty.class))).thenAnswer(i -> i.getArgument(0));

        Penalty result = penaltyService.assessLateCancellation(booking, user);

        assertNotNull(result);
        assertEquals(0, new BigDecimal("10.00").compareTo(result.getAmount())); // 50% of 20
        assertEquals(Penalty.PenaltyType.LATE_CANCELLATION, result.getType());
        verify(penaltyRepository).save(any(Penalty.class));
    }

    @Test
    void assessLateCancellation_moreThan2HoursBefore_shouldReturnNull() {
        User user = User.builder().id("user-1").build();
        Trip trip = Trip.builder()
                .id("trip-1")
                .departureTime(LocalDateTime.now().plusHours(5))
                .build();
        Booking booking = Booking.builder()
                .id("booking-1")
                .trip(trip)
                .passenger(user)
                .amountPaid(new BigDecimal("20.00"))
                .build();

        Penalty result = penaltyService.assessLateCancellation(booking, user);

        assertNull(result);
        verify(penaltyRepository, never()).save(any());
    }

    @Test
    void assessLateCancellation_afterDeparture_shouldReturnNull() {
        User user = User.builder().id("user-1").build();
        Trip trip = Trip.builder()
                .id("trip-1")
                .departureTime(LocalDateTime.now().minusHours(1))
                .build();
        Booking booking = Booking.builder()
                .id("booking-1")
                .trip(trip)
                .passenger(user)
                .amountPaid(new BigDecimal("20.00"))
                .currency(Trip.Currency.EUR)
                .build();

        Penalty result = penaltyService.assessLateCancellation(booking, user);

        assertNull(result); // Past departure = no late cancellation penalty (use assessNoShow instead)
        verify(penaltyRepository, never()).save(any());
    }

    @Test
    void assessLateCancellation_withZeroAmount_shouldReturnNull() {
        User user = User.builder().id("user-1").build();
        Trip trip = Trip.builder()
                .id("trip-1")
                .departureTime(LocalDateTime.now().plusHours(1))
                .build();
        Booking booking = Booking.builder()
                .id("booking-1")
                .trip(trip)
                .passenger(user)
                .amountPaid(BigDecimal.ZERO)
                .build();

        Penalty result = penaltyService.assessLateCancellation(booking, user);

        assertNull(result);
    }

    @Test
    void assessNoShow_shouldCreateFullAmountPenalty() {
        Booking booking = Booking.builder()
                .id("booking-1")
                .passenger(User.builder().id("user-1").build())
                .amountPaid(new BigDecimal("30.00"))
                .currency(Trip.Currency.EUR)
                .build();
        Trip trip = Trip.builder().id("trip-1").build();

        when(penaltyRepository.save(any(Penalty.class))).thenAnswer(i -> i.getArgument(0));

        Penalty result = penaltyService.assessNoShow(trip, booking);

        assertNotNull(result);
        assertEquals(new BigDecimal("30.00"), result.getAmount()); // 100% of amount
        assertEquals(Penalty.PenaltyType.NO_SHOW, result.getType());
        verify(penaltyRepository).save(any(Penalty.class));
    }

    @Test
    void assessNoShow_withZeroAmount_shouldReturnNull() {
        Booking booking = Booking.builder()
                .id("booking-1")
                .passenger(User.builder().id("user-1").build())
                .amountPaid(BigDecimal.ZERO)
                .build();
        Trip trip = Trip.builder().id("trip-1").build();

        Penalty result = penaltyService.assessNoShow(trip, booking);

        assertNull(result);
    }
}
