package com.shareway.application.usecase;

import com.shareway.application.dto.request.BookTripRequest;
import com.shareway.application.dto.request.CreateTripRequest;
import com.shareway.application.dto.request.RespondBookingRequest;
import com.shareway.application.dto.request.TripSearchRequest;
import com.shareway.application.dto.request.UpdateTripRequest;
import com.shareway.application.dto.response.BookingResponse;
import com.shareway.application.dto.response.TripResponse;
import com.shareway.application.port.out.AuditPort;
import com.shareway.application.port.out.EmailPort;
import com.shareway.application.port.out.NotificationPort;
import com.shareway.domain.exception.InsufficientSeatsException;
import com.shareway.domain.exception.InvalidOperationException;
import com.shareway.domain.exception.NotAuthorizedException;
import com.shareway.domain.exception.TripNotFoundException;
import com.shareway.domain.exception.UserNotFoundException;
import com.shareway.domain.model.Booking;
import com.shareway.domain.model.Trip;
import com.shareway.domain.model.User;
import com.shareway.domain.repository.BookingRepository;
import com.shareway.domain.repository.TripEditHistoryRepository;
import com.shareway.domain.repository.TripRepository;
import com.shareway.domain.repository.UserRepository;
import com.shareway.domain.service.PenaltyService;
import com.shareway.domain.service.TripDomainService;
import com.shareway.application.mapper.TripHistoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripUseCaseTest {

    @Mock private TripRepository tripRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private TripDomainService tripDomainService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AuditPort auditPort;
    @Mock private NotificationPort notificationPort;
    @Mock private EmailPort emailPort;
    @Mock private TripEditHistoryRepository tripEditHistoryRepository;
    @Mock private TripHistoryMapper tripHistoryMapper;
    @Mock private StripeUseCase stripeUseCase;
    @Mock private PenaltyService penaltyService;

    @InjectMocks
    private TripUseCase tripUseCase;

    private User driver;
    private User passenger;
    private Trip trip;

    @BeforeEach
    void setUp() {
        driver = User.builder()
                .id("driver-1").firstName("Jean").lastName("Dupont")
                .email("jean@test.com").phone("+25779000000")
                .role(User.UserRole.DRIVER).build();
        driver.setPhoneVisible(true);

        passenger = User.builder()
                .id("passenger-1").firstName("Marie").lastName("Niyonzima")
                .email("marie@test.com").role(User.UserRole.PASSENGER).build();

        trip = Trip.builder()
                .id("trip-1").driver(driver)
                .departureCity("Bujumbura").arrivalCity("Gitega")
                .departureTime(LocalDateTime.now().plusDays(2))
                .totalSeats(4).availableSeats(3)
                .pricePerSeat(new BigDecimal("5000"))
                .currency(Trip.Currency.FBU)
                .status(Trip.TripStatus.OPEN)
                .shareToken("abc123")
                .build();
    }

    // ── CREATE ─────────────────────────────────────────────────────────

    @Test
    void create_trip_shouldSucceed() {
        CreateTripRequest req = new CreateTripRequest();
        req.setDepartureCity("Bujumbura");
        req.setArrivalCity("Gitega");
        req.setDepartureTime(LocalDateTime.now().plusDays(2));
        req.setTotalSeats(4);
        req.setPricePerSeat(new BigDecimal("5000"));
        req.setCurrency("FBU");

        when(userRepository.findByIdAndDeletedAtIsNull("driver-1")).thenReturn(Optional.of(driver));
        when(tripDomainService.generateShareToken()).thenReturn("abc123");
        when(tripRepository.save(any(Trip.class))).thenAnswer(i -> i.getArgument(0));

        TripResponse response = tripUseCase.create(req, "driver-1");

        assertNotNull(response);
        assertEquals("Bujumbura", response.getDepartureCity());
        assertEquals("Gitega", response.getArrivalCity());
        verify(auditPort).log(eq("TRIP_CREATED"), eq("Trip"), any(), isNull(), isNull(), eq("driver-1"));
    }

    @Test
    void create_trip_passenger_shouldThrow() {
        when(userRepository.findByIdAndDeletedAtIsNull("passenger-1")).thenReturn(Optional.of(passenger));

        CreateTripRequest req = new CreateTripRequest();
        req.setDepartureCity("Bujumbura");

        assertThrows(InvalidOperationException.class,
                () -> tripUseCase.create(req, "passenger-1"));
    }

    // ── UPDATE ─────────────────────────────────────────────────────────

    @Test
    void update_trip_shouldChangePrice() {
        when(tripRepository.findByIdAndDeletedAtIsNull("trip-1")).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(i -> i.getArgument(0));

        UpdateTripRequest req = new UpdateTripRequest();
        req.setPricePerSeat(new BigDecimal("7000"));

        TripResponse response = tripUseCase.update("trip-1", req, "driver-1");

        assertEquals(new BigDecimal("7000"), response.getPricePerSeat());
        verify(notificationPort, never()).notifyWithLink(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void update_trip_wrong_driver_shouldThrow() {
        when(tripRepository.findByIdAndDeletedAtIsNull("trip-1")).thenReturn(Optional.of(trip));

        UpdateTripRequest req = new UpdateTripRequest();
        req.setPricePerSeat(new BigDecimal("7000"));

        assertThrows(NotAuthorizedException.class,
                () -> tripUseCase.update("trip-1", req, "other-driver"));
    }

    @Test
    void update_completed_trip_shouldThrow() {
        trip.setStatus(Trip.TripStatus.COMPLETED);
        when(tripRepository.findByIdAndDeletedAtIsNull("trip-1")).thenReturn(Optional.of(trip));

        UpdateTripRequest req = new UpdateTripRequest();
        req.setPricePerSeat(new BigDecimal("7000"));

        assertThrows(InvalidOperationException.class,
                () -> tripUseCase.update("trip-1", req, "driver-1"));
    }

    // ── BOOK ───────────────────────────────────────────────────────────

    @Test
    void book_trip_shouldCreateBooking() {
        when(tripRepository.findByIdAndDeletedAtIsNull("trip-1")).thenReturn(Optional.of(trip));
        when(userRepository.findByIdAndDeletedAtIsNull("passenger-1")).thenReturn(Optional.of(passenger));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
        when(tripRepository.save(any(Trip.class))).thenAnswer(i -> i.getArgument(0));

        BookTripRequest req = new BookTripRequest();
        req.setSeats(1);
        req.setCurrency("FBU");

        BookingResponse response = tripUseCase.book("trip-1", req, "passenger-1");

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        assertEquals(new BigDecimal("5000"), response.getAmountPaid());
        verify(notificationPort).notifyWithLink(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(auditPort).log(eq("BOOKING_CREATED"), eq("Booking"), any(), isNull(), isNull(), eq("passenger-1"));
    }

    @Test
    void book_trip_not_found_shouldThrow() {
        when(tripRepository.findByIdAndDeletedAtIsNull("unknown")).thenReturn(Optional.empty());

        BookTripRequest req = new BookTripRequest();
        req.setSeats(1);

        assertThrows(TripNotFoundException.class,
                () -> tripUseCase.book("unknown", req, "passenger-1"));
    }

    // ── CANCEL ─────────────────────────────────────────────────────────

    @Test
    void cancel_trip_shouldNotifyPassengers() {
        Booking booking = Booking.builder()
                .id("booking-1").trip(trip).passenger(passenger)
                .seatsBooked(1).status(Booking.BookingStatus.CONFIRMED)
                .amountPaid(new BigDecimal("5000")).currency(Trip.Currency.FBU)
                .build();
        trip.getBookings().add(booking);

        when(tripRepository.findByIdAndDeletedAtIsNull("trip-1")).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(i -> i.getArgument(0));

        tripUseCase.cancel("trip-1", "Raison test", "driver-1");

        assertEquals(Trip.TripStatus.CANCELLED, trip.getStatus());
        verify(emailPort).sendTripCancellation(anyString(), anyString(), anyString(), eq("Raison test"));
        verify(auditPort).log(eq("TRIP_CANCELLED"), eq("Trip"), eq("trip-1"), isNull(), isNull(), eq("driver-1"));
    }

    @Test
    void cancel_trip_wrong_driver_shouldThrow() {
        when(tripRepository.findByIdAndDeletedAtIsNull("trip-1")).thenReturn(Optional.of(trip));

        assertThrows(NotAuthorizedException.class,
                () -> tripUseCase.cancel("trip-1", "reason", "other-driver"));
    }

    // ── COMPLETE ───────────────────────────────────────────────────────

    @Test
    void complete_trip_shouldWork() {
        when(tripRepository.findByIdAndDeletedAtIsNull("trip-1")).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(i -> i.getArgument(0));

        tripUseCase.complete("trip-1", "driver-1");

        assertEquals(Trip.TripStatus.COMPLETED, trip.getStatus());
        verify(auditPort).log(eq("TRIP_COMPLETED"), eq("Trip"), eq("trip-1"), isNull(), isNull(), eq("driver-1"));
    }

    // ── GET BY ID ──────────────────────────────────────────────────────

    @Test
    void getById_found() {
        when(tripRepository.findByIdAndDeletedAtIsNull("trip-1")).thenReturn(Optional.of(trip));
        TripResponse response = tripUseCase.getById("trip-1");
        assertEquals("trip-1", response.getId());
    }

    @Test
    void getById_not_found_shouldThrow() {
        when(tripRepository.findByIdAndDeletedAtIsNull("unknown")).thenReturn(Optional.empty());
        assertThrows(TripNotFoundException.class, () -> tripUseCase.getById("unknown"));
    }

    // ── SEARCH ─────────────────────────────────────────────────────────

    @Test
    void search_shouldReturnResults() {
        TripSearchRequest req = new TripSearchRequest();
        req.setDepartureCity("Bujumbura");

        org.springframework.data.domain.Page<Trip> page =
                new org.springframework.data.domain.PageImpl<>(List.of(trip));
        when(tripRepository.search(anyString(), isNull(), any(LocalDateTime.class), isNull(),
                eq(1), isNull(), isNull(), isNull(), isNull(), isNull(), any())).thenReturn(page);

        var result = tripUseCase.search(req, 0, 20);
        assertNotNull(result);
    }
}
