package com.shareway.application.usecase;

import com.shareway.application.port.out.AuditPort;
import com.shareway.application.port.out.EmailPort;
import com.shareway.application.port.out.NotificationPort;
import com.shareway.domain.exception.TripNotFoundException;
import com.shareway.domain.model.Trip;
import com.shareway.domain.model.User;
import com.shareway.domain.repository.BookingRepository;
import com.shareway.domain.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUseCaseTripTest {

    @Mock private TripRepository tripRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private AuditPort auditPort;
    @Mock private NotificationPort notificationPort;
    @Mock private EmailPort emailPort;

    @InjectMocks
    private com.shareway.application.usecase.AdminUseCase adminUseCase;

    private Trip trip;
    private User driver;

    @BeforeEach
    void setUp() {
        driver = User.builder()
                .id("driver-1").firstName("Jean").lastName("Dupont")
                .email("jean@test.com").role(User.UserRole.DRIVER).build();

        trip = Trip.builder()
                .id("trip-1").driver(driver)
                .departureCity("Bujumbura").arrivalCity("Gitega")
                .departureTime(LocalDateTime.now().plusDays(2))
                .totalSeats(4).availableSeats(3)
                .pricePerSeat(new BigDecimal("5000"))
                .currency(Trip.Currency.FBU)
                .status(Trip.TripStatus.OPEN)
                .build();
    }

    // ── GET ALL TRIPS ──────────────────────────────────────────────────

    @Test
    void getAllTrips_withStatusFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(trip), pageable, 1);
        when(tripRepository.findByStatus(Trip.TripStatus.OPEN, pageable)).thenReturn(page);

        var result = adminUseCase.getAllTrips("OPEN", 0, 20);

        assertNotNull(result);
        verify(tripRepository).findByStatus(Trip.TripStatus.OPEN, pageable);
    }

    @Test
    void getAllTrips_noFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(trip), pageable, 1);
        when(tripRepository.findAll(pageable)).thenReturn(page);

        var result = adminUseCase.getAllTrips(null, 0, 20);

        assertNotNull(result);
        verify(tripRepository).findAll(pageable);
    }

    // ── APPROVE TRIP ───────────────────────────────────────────────────

    @Test
    void approveTrip_shouldSetOpen() {
        trip.setStatus(Trip.TripStatus.PENDING);
        when(tripRepository.findByIdAndDeletedAtIsNull("trip-1")).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(i -> i.getArgument(0));

        var result = adminUseCase.approveTrip("trip-1", "admin-1");

        assertEquals("OPEN", result.getStatus());
        verify(auditPort).log(eq("TRIP_APPROVED"), eq("Trip"), eq("trip-1"), isNull(), isNull(), eq("admin-1"));
    }

    @Test
    void approveTrip_notFound_shouldThrow() {
        when(tripRepository.findByIdAndDeletedAtIsNull("unknown")).thenReturn(Optional.empty());

        assertThrows(TripNotFoundException.class,
                () -> adminUseCase.approveTrip("unknown", "admin-1"));
    }

    // ── REJECT TRIP ────────────────────────────────────────────────────

    @Test
    void rejectTrip_shouldSetRejected() {
        when(tripRepository.findByIdAndDeletedAtIsNull("trip-1")).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(i -> i.getArgument(0));

        var result = adminUseCase.rejectTrip("trip-1", "admin-1", "Non conforme");

        assertEquals("REJECTED", result.getStatus());
        verify(notificationPort).notify(eq("driver-1"), eq("SYSTEM"), eq("Trajet rejeté"), anyString());
        verify(emailPort).sendGeneral(eq("jean@test.com"), anyString(), anyString());
        verify(auditPort).log(eq("TRIP_REJECTED"), eq("Trip"), eq("trip-1"), isNull(), eq("Non conforme"), eq("admin-1"));
    }

    // ── SUSPEND TRIP ───────────────────────────────────────────────────

    @Test
    void suspendTrip_shouldSetSuspended() {
        when(tripRepository.findByIdAndDeletedAtIsNull("trip-1")).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(i -> i.getArgument(0));

        var result = adminUseCase.suspendTrip("trip-1", "admin-1", "Maintenance");

        assertEquals("SUSPENDED", result.getStatus());
        verify(notificationPort).notify(eq("driver-1"), eq("SYSTEM"), eq("Trajet suspendu"), anyString());
        verify(auditPort).log(eq("TRIP_SUSPENDED"), eq("Trip"), eq("trip-1"), isNull(), eq("Maintenance"), eq("admin-1"));
    }

    // ── REACTIVATE TRIP ────────────────────────────────────────────────

    @Test
    void reactivateTrip_shouldSetOpen() {
        trip.setStatus(Trip.TripStatus.SUSPENDED);
        when(tripRepository.findByIdAndDeletedAtIsNull("trip-1")).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(i -> i.getArgument(0));

        var result = adminUseCase.reactivateTrip("trip-1", "admin-1");

        assertEquals("OPEN", result.getStatus());
        verify(notificationPort).notify(eq("driver-1"), eq("SYSTEM"), eq("Trajet réactivé"), anyString());
        verify(auditPort).log(eq("TRIP_REACTIVATED"), eq("Trip"), eq("trip-1"), isNull(), isNull(), eq("admin-1"));
    }

    // ── SOFT DELETE TRIP ───────────────────────────────────────────────

    @Test
    void softDeleteTrip_shouldCancelAndDelete() {
        when(tripRepository.findByIdAndDeletedAtIsNull("trip-1")).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(i -> i.getArgument(0));

        adminUseCase.softDeleteTrip("trip-1", "admin-1", "Contenu inapproprié");

        assertEquals(Trip.TripStatus.CANCELLED, trip.getStatus());
        assertNotNull(trip.getDeletedAt());
        assertEquals("admin-1", trip.getDeletedBy());
        verify(notificationPort).notify(eq("driver-1"), eq("SYSTEM"), eq("Trajet supprimé"), anyString());
        verify(auditPort).log(eq("TRIP_DELETED"), eq("Trip"), eq("trip-1"), isNull(), eq("Contenu inapproprié"), eq("admin-1"));
    }

    // ── GET TRIP DETAIL ────────────────────────────────────────────────

    @Test
    void getTripDetail_shouldReturnTrip() {
        when(tripRepository.findByIdAndDeletedAtIsNull("trip-1")).thenReturn(Optional.of(trip));

        var result = adminUseCase.getTripDetail("trip-1");

        assertEquals("trip-1", result.getId());
        assertEquals("Bujumbura", result.getDepartureCity());
    }

    @Test
    void getTripDetail_notFound_shouldThrow() {
        when(tripRepository.findByIdAndDeletedAtIsNull("unknown")).thenReturn(Optional.empty());

        assertThrows(TripNotFoundException.class,
                () -> adminUseCase.getTripDetail("unknown"));
    }
}
