package com.shareway.application.usecase;
/*
import com.shareway.application.dto.request.BookTripRequest;
import com.shareway.application.dto.request.CreateTripRequest;
import com.shareway.application.dto.request.TripSearchRequest;
import com.shareway.application.dto.response.PageResponse;
import com.shareway.application.dto.response.PassengerResponse;
import com.shareway.application.dto.response.StopPointResponse;
import com.shareway.application.dto.response.TripPreferencesResponse;
import com.shareway.application.dto.response.TripResponse;
import com.shareway.application.dto.response.TripUserResponse;
import com.shareway.application.port.out.AuditPort;
import com.shareway.application.port.out.NotificationPort;
import com.shareway.domain.event.TripBookedEvent;
import com.shareway.domain.event.TripCancelledEvent;
import com.shareway.domain.exception.BookingNotFoundException;
import com.shareway.domain.exception.CancelBookingException;
import com.shareway.domain.exception.InsufficientSeatsException;
import com.shareway.domain.exception.InvalidOperationException;
import com.shareway.domain.exception.NotAuthorizedException;
import com.shareway.domain.exception.TripNotFoundException;
import com.shareway.domain.exception.UserNotFoundException;
import com.shareway.domain.model.Booking;
import com.shareway.domain.model.StopPoint;
import com.shareway.domain.model.Trip;
import com.shareway.domain.model.TripPreferences;
import com.shareway.domain.model.User;
import com.shareway.domain.repository.BookingRepository;
import com.shareway.domain.repository.TripRepository;
import com.shareway.domain.repository.UserRepository;
import com.shareway.domain.service.TripDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TripUseCase {

    public static final String TRIP_NOT_FOUND = "Trip not found";
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TripDomainService tripDomainService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditPort auditPort;
    private final NotificationPort notificationPort;

    // ─────────────────────────────────────────────────────────────────────
    // Créer un trajet
    // ─────────────────────────────────────────────────────────────────────
    public TripResponse create(CreateTripRequest req, String driverId) {
        User driver = userRepository.findByIdAndDeletedAtIsNull(driverId)
                .orElseThrow(() -> new UserNotFoundException("Driver not found"));

        if (!driver.isDriver())
            throw new InvalidOperationException("Only drivers can create trips");

        Trip trip = Trip.builder()
                .driver(driver)
                .departureCity(req.getDepartureCity())
                .arrivalCity(req.getArrivalCity())
                .departureAddress(req.getDepartureAddress())
                .arrivalAddress(req.getArrivalAddress())
                .departureLat(req.getDepartureLat())
                .departureLng(req.getDepartureLng())
                .arrivalLat(req.getArrivalLat())
                .arrivalLng(req.getArrivalLng())
                .departureTime(req.getDepartureTime())
                .arrivalTime(req.getArrivalTime())
                .totalSeats(req.getTotalSeats())
                .availableSeats(req.getTotalSeats())
                .pricePerSeat(req.getPricePerSeat())
                .currency(Trip.Currency.valueOf(req.getCurrency()))
                .description(req.getDescription())
                .recurring(req.isRecurring())
                .frequency(req.getFrequency() != null ? Trip.TripFrequency.valueOf(req.getFrequency()) : null)
                .recurringDays(req.getRecurringDays())
                .recurringEndDate(req.getRecurringEndDate())
                .shareToken(tripDomainService.generateShareToken())
                .build();

        if (req.getStopPoints() != null) {
            req.getStopPoints().forEach(sp -> {
                StopPoint stop = StopPoint.builder()
                        .trip(trip).city(sp.getCity()).address(sp.getAddress())
                        .lat(sp.getLat()).lng(sp.getLng())
                        .stopOrder(sp.getOrder()).arrivalTime(sp.getArrivalTime()).build();
                trip.getStopPoints().add(stop);
            });
        }

        if (req.getPreferences() != null) {
            var p = req.getPreferences();
            TripPreferences prefs = TripPreferences.builder()
                    .trip(trip)
                    .music(p.isMusic())
                    .smoking(p.isSmoking())
                    .pets(p.isPets())
                    .talking(p.isTalking())
                    .airConditioning(p.isAirConditioning())
                    .smallLuggage(p.isSmallLuggage())
                    .largeLuggage(p.isLargeLuggage())
                    .build();
            trip.setPreferences(prefs);
        }

        tripRepository.save(trip);
        auditPort.log("TRIP_CREATED", "Trip", trip.getId(), null, null, driverId);
        return toResponse(trip);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Recherche — seul departureCity est requis
    // ─────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PageResponse<TripResponse> search(TripSearchRequest req, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("departureTime").ascending());

        // Construire les filtres dynamiquement
        LocalDateTime from = null;
        LocalDateTime to = null;

        if (req.getDate() != null && !req.getDate().isBlank()) {
            LocalDate date = LocalDate.parse(req.getDate());
            from = date.atStartOfDay();
            to = date.atTime(LocalTime.MAX);
        } else {
            // Par défaut : trajets futurs uniquement
            from = LocalDateTime.now();
        }

        // Appel repository avec tous les filtres optionnels
        Page<Trip> trips = tripRepository.search(
                req.getDepartureCity(),
                req.getArrivalCity(),
                from, to,
                req.getSeats() != null ? req.getSeats() : 1,
                req.getMaxPrice(),
                req.getMinRating(),
                req.getSmallLuggage(),
                req.getLargeLuggage(),
                req.getPets(),
                pageable
        );

        return PageResponse.from(trips.map(this::toResponse));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Détail d'un trajet
    // ─────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public TripResponse getById(String tripId) {
        return toResponse(tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new TripNotFoundException("Trip not found: " + tripId)));
    }

    @Transactional(readOnly = true)
    public TripResponse getByShareToken(String token) {
        return toResponse(tripRepository.findByShareTokenAndDeletedAtIsNull(token)
                .orElseThrow(() -> new TripNotFoundException("Trip not found for token: " + token)));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Réserver un trajet
    // ─────────────────────────────────────────────────────────────────────
    public void book(String tripId, BookTripRequest req, String passengerId) {
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new TripNotFoundException(TRIP_NOT_FOUND));
        User passenger = userRepository.findByIdAndDeletedAtIsNull(passengerId)
                .orElseThrow(() -> new UserNotFoundException("Passenger not found"));

        tripDomainService.validateBooking(trip, passenger);

        if (!trip.canJoin(req.getSeats()))
            throw new InsufficientSeatsException("Not enough available seats");

        trip.bookSeats(req.getSeats());

        Booking booking = Booking.builder()
                .trip(trip).passenger(passenger)
                .seatsBooked(req.getSeats())
                .currency(req.getCurrency() != null
                        ? Trip.Currency.valueOf(req.getCurrency()) : trip.getCurrency())
                .amountPaid(trip.getPricePerSeat()
                        .multiply(BigDecimal.valueOf(req.getSeats())))
                .build();

        bookingRepository.save(booking);
        tripRepository.save(trip);

        eventPublisher.publishEvent(
                new TripBookedEvent(tripId, passengerId, trip.getDriver().getId(), req.getSeats()));
        notificationPort.notify(trip.getDriver().getId(), "BOOKING",
                "Nouveau passager",
                passenger.getFullName() + " a rejoint votre trajet vers " + trip.getArrivalCity());
        auditPort.log("TRIP_BOOKED", "Booking", booking.getId(), null, null, passengerId);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Annuler un trajet (passager)
    // ─────────────────────────────────────────────────────────────────────
    public Void leaveTrip(String tripId, String reason, String passagerId) {
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new TripNotFoundException(TRIP_NOT_FOUND));

        if (trip.getDriver().getId().equals(passagerId))
            throw new CancelBookingException("Only the passenger can cancel this trip");

        Booking booking = bookingRepository.findByTripIdAndPassengerId(tripId, passagerId).orElse(null);

        if (booking != null) {
            if (Booking.BookingStatus.CANCELLED.equals(booking.getStatus())) {
                throw new CancelBookingException("Réservation déjà annulée");
            }
            if (Booking.BookingStatus.COMPLETED.equals(booking.getStatus())) {
                throw new CancelBookingException("Vous ne pouvez pas annuler un trajet terminé");
            }

            booking.cancel(reason);
            eventPublisher.publishEvent(
                    new TripCancelledEvent(tripId, passagerId, reason, 1));
            auditPort.log("BOOKING_CANCELLED", "Trip", tripId, null, null, passagerId);
            bookingRepository.save(booking);
        } else {
            throw new BookingNotFoundException("La réservation n'existe pas");
        }
        return null;
    }


    // ─────────────────────────────────────────────────────────────────────
    // Annuler un trajet (conducteur)
    // ─────────────────────────────────────────────────────────────────────
    public void cancel(String tripId, String reason, String driverId) {
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new TripNotFoundException(TRIP_NOT_FOUND));

        if (!trip.getDriver().getId().equals(driverId))
            throw new NotAuthorizedException("Only the driver can cancel this trip");

        List<Booking> confirmed = trip.getBookings().stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED)
                .toList();

        confirmed.forEach(b -> {
            b.cancel(reason);
            notificationPort.notify(b.getPassenger().getId(), "CANCELLATION",
                    "Trajet annulé",
                    "Votre trajet vers " + trip.getArrivalCity() + " a été annulé");
        });

        trip.cancel(driverId);
        tripRepository.save(trip);
        eventPublisher.publishEvent(
                new TripCancelledEvent(tripId, driverId, reason, confirmed.size()));
        auditPort.log("TRIP_CANCELLED", "Trip", tripId, null, null, driverId);
    }


    // ─────────────────────────────────────────────────────────────────────
    // Compléter un trajet (conducteur)
    // ─────────────────────────────────────────────────────────────────────
    public void complete(String tripId, String driverId) {
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new TripNotFoundException(TRIP_NOT_FOUND));

        if (!trip.getDriver().getId().equals(driverId))
            throw new NotAuthorizedException("Only the driver can complete this trip");

        trip.complete();
        trip.getBookings().stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED)
                .forEach(b -> {
                    b.complete();
                    // Inviter le passager à laisser un avis
                    notificationPort.notifyWithLink(
                            b.getPassenger().getId(), "REVIEW",
                            "Trajet terminé — laissez un avis",
                            "Comment s'est passé votre trajet avec " + trip.getDriver().getFirstName() + " ?",
                            "/reviews/new?tripId=" + tripId + "&targetId=" + driverId
                    );
                });

        tripRepository.save(trip);
        auditPort.log("TRIP_COMPLETED", "Trip", tripId, null, null, driverId);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> getMyTrips(String driverId) {
        return tripRepository.findByDriverId(driverId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TripResponse> getMyBookings(String passengerId) {
        return tripRepository.findByPassengerId(passengerId).stream()
                .map(this::toResponse).toList();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Mapper Trip → TripResponse (complet)
    // ─────────────────────────────────────────────────────────────────────
    private TripResponse toResponse(Trip t) {
        var prefs = t.getPreferences();
        return TripResponse.builder()
                .id(t.getId())
                .driver(TripUserResponse.builder()
                        .id(t.getDriver().getId())
                        .firstName(t.getDriver().getFirstName())
                        .lastName(t.getDriver().getLastName())
                        .avatarUrl(t.getDriver().getAvatarUrl())
                        .rating(t.getDriver().getRating())
                        .verified(t.getDriver().isIdentityVerified())
                        .phone(t.getDriver().isPhoneVisible() ? t.getDriver().getPhone() : null)
                        .build())
                .departureCity(t.getDepartureCity())
                .arrivalCity(t.getArrivalCity())
                .departureAddress(t.getDepartureAddress())
                .arrivalAddress(t.getArrivalAddress())
                .departureTime(t.getDepartureTime())
                .arrivalTime(t.getArrivalTime())
                .availableSeats(t.getAvailableSeats())
                .totalSeats(t.getTotalSeats())
                .pricePerSeat(t.getPricePerSeat())
                .currency(t.getCurrency().name())
                .description(t.getDescription())
                .status(t.getStatus().name())
                .shareToken(t.getShareToken())
                .recurring(t.isRecurring())
                .frequency(t.getFrequency() != null ? t.getFrequency().name() : null)
                .stopPoints(t.getStopPoints().stream()
                        .sorted(Comparator.comparingInt(StopPoint::getStopOrder))
                        .map(sp -> StopPointResponse.builder()
                                .id(sp.getId()).city(sp.getCity()).address(sp.getAddress())
                                .order(sp.getStopOrder()).arrivalTime(sp.getArrivalTime()).build())
                        .toList())
                .passengers(t.getBookings().stream()
                        .filter(b -> b.getStatus() != Booking.BookingStatus.CANCELLED)
                        .map(b -> PassengerResponse.builder()
                                .id(b.getPassenger().getId())
                                .firstName(b.getPassenger().getFirstName())
                                .lastName(b.getPassenger().getLastName())
                                .avatarUrl(b.getPassenger().getAvatarUrl())
                                .bookingId(b.getId())
                                .bookingStatus(b.getStatus().name())
                                .bookedAt(b.getCreatedAt()).build())
                        .toList())
                .preferences(prefs != null ? TripPreferencesResponse.builder()
                        .music(prefs.isMusic()).smoking(prefs.isSmoking())
                        .pets(prefs.isPets()).talking(prefs.isTalking())
                        .airConditioning(prefs.isAirConditioning())
                        .smallLuggage(prefs.isSmallLuggage())
                        .largeLuggage(prefs.isLargeLuggage())
                        .build() : null)
                .createdAt(t.getCreatedAt())
                .build();
    }
}
*/


import com.shareway.application.dto.request.BookTripRequest;
import com.shareway.application.dto.request.CreateTripRequest;
import com.shareway.application.dto.request.RespondBookingRequest;
import com.shareway.application.dto.request.StopPointRequest;
import com.shareway.application.dto.request.TripPreferencesRequest;
import com.shareway.application.dto.request.TripSearchRequest;
import com.shareway.application.dto.request.UpdateTripRequest;
import com.shareway.application.dto.response.BookingResponse;
import com.shareway.application.dto.response.PageResponse;
import com.shareway.application.dto.response.PassengerPublicResponse;
import com.shareway.application.dto.response.PassengerResponse;
import com.shareway.application.dto.response.StopPointResponse;
import com.shareway.application.dto.response.TripEditHistoryResponse;
import com.shareway.application.dto.response.TripPreferencesResponse;
import com.shareway.application.dto.response.TripResponse;
import com.shareway.application.dto.response.TripUserResponse;
import com.shareway.application.mapper.TripHistoryMapper;
import com.shareway.application.port.out.AuditPort;
import com.shareway.application.port.out.NotificationPort;
import com.shareway.infrastructure.adapter.audit.domain.event.TripCancelledEvent;
import com.shareway.infrastructure.adapter.audit.domain.exception.BookingNotFoundException;
import com.shareway.infrastructure.adapter.audit.domain.exception.InsufficientSeatsException;
import com.shareway.infrastructure.adapter.audit.domain.exception.InvalidOperationException;
import com.shareway.infrastructure.adapter.audit.domain.exception.NotAuthorizedException;
import com.shareway.infrastructure.adapter.audit.domain.exception.TripNotFoundException;
import com.shareway.infrastructure.adapter.audit.domain.exception.UserNotFoundException;
import com.shareway.infrastructure.adapter.audit.domain.model.Booking;
import com.shareway.infrastructure.adapter.audit.domain.model.StopPoint;
import com.shareway.infrastructure.adapter.audit.domain.model.Trip;
import com.shareway.infrastructure.adapter.audit.domain.model.TripPreferences;
import com.shareway.infrastructure.adapter.audit.domain.model.User;
import com.shareway.infrastructure.adapter.audit.domain.repository.BookingRepository;
import com.shareway.infrastructure.adapter.audit.domain.repository.TripEditHistoryRepository;
import com.shareway.infrastructure.adapter.audit.domain.repository.TripRepository;
import com.shareway.infrastructure.adapter.audit.domain.repository.UserRepository;
import com.shareway.infrastructure.adapter.audit.domain.service.TripDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TripUseCase {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TripDomainService tripDomainService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditPort auditPort;
    private final NotificationPort notificationPort;
    private final TripEditHistoryRepository tripEditHistoryRepository;
    private final TripHistoryMapper tripHistoryMapper;

    // ── Créer un trajet ───────────────────────────────────────────────────
    public TripResponse create(CreateTripRequest req, String driverId) {
        User driver = userRepository.findByIdAndDeletedAtIsNull(driverId)
                .orElseThrow(() -> new UserNotFoundException("Conducteur introuvable"));
        if (!driver.isDriver())
            throw new InvalidOperationException("Seuls les conducteurs peuvent créer des trajets");

        Trip trip = Trip.builder()
                .driver(driver)
                .departureCity(req.getDepartureCity()).arrivalCity(req.getArrivalCity())
                .departureAddress(req.getDepartureAddress()).arrivalAddress(req.getArrivalAddress())
                .departureLat(req.getDepartureLat()).departureLng(req.getDepartureLng())
                .arrivalLat(req.getArrivalLat()).arrivalLng(req.getArrivalLng())
                .departureTime(req.getDepartureTime()).arrivalTime(req.getArrivalTime())
                .totalSeats(req.getTotalSeats()).availableSeats(req.getTotalSeats())
                .pricePerSeat(req.getPricePerSeat())
                .currency(Trip.Currency.valueOf(req.getCurrency()))
                .description(req.getDescription())
                .recurring(req.isRecurring())
                .frequency(req.getFrequency() != null ? Trip.TripFrequency.valueOf(req.getFrequency()) : null)
                .recurringDays(req.getRecurringDays()).recurringEndDate(req.getRecurringEndDate())
                .shareToken(tripDomainService.generateShareToken())
                .build();

        buildStopPoints(trip, req.getStopPoints());
        buildPreferences(trip, req.getPreferences());
        tripRepository.save(trip);
        auditPort.log("TRIP_CREATED", "Trip", trip.getId(), null, null, driverId);
        return toResponse(trip);
    }

    // ── Modifier un trajet (conducteur) ────────────────────────────────────
    public TripResponse update(String tripId, UpdateTripRequest req, String driverId) {
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new TripNotFoundException("Trajet introuvable"));

        if (!trip.getDriver().getId().equals(driverId))
            throw new NotAuthorizedException("Seul le conducteur peut modifier ce trajet");

        if (trip.getStatus() == Trip.TripStatus.COMPLETED || trip.getStatus() == Trip.TripStatus.CANCELLED)
            throw new InvalidOperationException("Impossible de modifier un trajet " + trip.getStatus());

        // Collecter les changements pour notifier les passagers
        List<String> changes = new ArrayList<>();

        if (req.getDepartureCity() != null && !req.getDepartureCity().equals(trip.getDepartureCity())) {
            changes.add("Ville de départ : " + trip.getDepartureCity() + " → " + req.getDepartureCity());
            trip.setDepartureCity(req.getDepartureCity());
        }
        if (req.getArrivalCity() != null && !req.getArrivalCity().equals(trip.getArrivalCity())) {
            changes.add("Ville d'arrivée : " + trip.getArrivalCity() + " → " + req.getArrivalCity());
            trip.setArrivalCity(req.getArrivalCity());
        }
        if (req.getDepartureAddress() != null) trip.setDepartureAddress(req.getDepartureAddress());
        if (req.getArrivalAddress() != null) trip.setArrivalAddress(req.getArrivalAddress());
        if (req.getDepartureLat() != null) trip.setDepartureLat(req.getDepartureLat());
        if (req.getDepartureLng() != null) trip.setDepartureLng(req.getDepartureLng());
        if (req.getArrivalLat() != null) trip.setArrivalLat(req.getArrivalLat());
        if (req.getArrivalLng() != null) trip.setArrivalLng(req.getArrivalLng());

        if (req.getDepartureTime() != null && !req.getDepartureTime().equals(trip.getDepartureTime())) {
            changes.add("Heure de départ modifiée");
            trip.setDepartureTime(req.getDepartureTime());
        }
        if (req.getArrivalTime() != null) trip.setArrivalTime(req.getArrivalTime());
        if (req.getDescription() != null) trip.setDescription(req.getDescription());
        if (req.getCurrency() != null) trip.setCurrency(Trip.Currency.valueOf(req.getCurrency()));

        if (req.getPricePerSeat() != null && req.getPricePerSeat().compareTo(trip.getPricePerSeat()) != 0) {
            changes.add("Prix par place : " + trip.getPricePerSeat() + " → " + req.getPricePerSeat());
            trip.setPricePerSeat(req.getPricePerSeat());
        }

        // Modification des places : uniquement si pas de réservations confirmées
        if (req.getTotalSeats() != null) {
            long confirmedBookings = trip.getBookings().stream()
                    .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED
                            || b.getStatus() == Booking.BookingStatus.PENDING).count();
            if (req.getTotalSeats() < confirmedBookings)
                throw new InvalidOperationException(
                        "Impossible de réduire à " + req.getTotalSeats() +
                                " places, " + confirmedBookings + " réservation(s) active(s)");
            int diff = req.getTotalSeats() - trip.getTotalSeats();
            trip.setTotalSeats(req.getTotalSeats());
            trip.setAvailableSeats(Math.max(0, trip.getAvailableSeats() + diff));
            if (trip.getAvailableSeats() > 0 && trip.getStatus() == Trip.TripStatus.FULL)
                trip.setStatus(Trip.TripStatus.OPEN);
        }

        if (req.getPreferences() != null) buildPreferences(trip, req.getPreferences());
        if (req.getStopPoints() != null) {
            trip.getStopPoints().clear();
            buildStopPoints(trip, req.getStopPoints());
        }

        tripRepository.save(trip);

        // Notifier les passagers confirmés si des champs importants ont changé
        if (!changes.isEmpty()) {
            String msg = req.getNotificationMessage() != null
                    ? req.getNotificationMessage()
                    : "Votre trajet a été modifié : " + String.join(", ", changes);

            trip.getBookings().stream()
                    .filter(Booking::isActive)
                    .forEach(b -> notificationPort.notifyWithLink(
                            b.getPassenger().getId(), "TRIP_UPDATE",
                            "Trajet modifié",
                            msg,
                            "/trips/" + tripId));
        }

        auditPort.log("TRIP_UPDATED", "Trip", tripId, null, String.join("; ", changes), driverId);
        return toResponse(trip);
    }

    // ── Recherche (departureCity seul suffit) ─────────────────────────────
    @Transactional(readOnly = true)
    public PageResponse<TripResponse> search(TripSearchRequest req, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("departureTime").ascending());
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = null;

        if (req.getDate() != null && !req.getDate().isBlank()) {
            java.time.LocalDate date = java.time.LocalDate.parse(req.getDate());
            from = date.atStartOfDay();
            to = date.atTime(LocalTime.MAX);
        }

        Page<Trip> trips = tripRepository.search(
                req.getDepartureCity() != null ? req.getDepartureCity() : "",
                req.getArrivalCity(),
                from, to,
                req.getSeats() != null ? req.getSeats() : 1,
                req.getMaxPrice(),
                req.getMinRating(),
                req.getSmallLuggage(),
                req.getLargeLuggage(),
                req.getPets(),
                pageable
        );
        return PageResponse.from(trips.map(this::toResponse));
    }

    // ── Détails ───────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public TripResponse getById(String tripId) {
        return toResponse(tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new TripNotFoundException("Trajet introuvable: " + tripId)));
    }

    @Transactional(readOnly = true)
    public TripResponse getByShareToken(String token) {
        return toResponse(tripRepository.findByShareTokenAndDeletedAtIsNull(token)
                .orElseThrow(() -> new TripNotFoundException("Trajet introuvable")));
    }

    // ── Réserver (passager) ──────────────────────────────────────────────
    public BookingResponse book(String tripId, BookTripRequest req, String passengerId) {
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new TripNotFoundException("Trajet introuvable"));
        User passenger = userRepository.findByIdAndDeletedAtIsNull(passengerId)
                .orElseThrow(() -> new UserNotFoundException("Passager introuvable"));

        tripDomainService.validateBooking(trip, passenger);
        if (!trip.canJoin(req.getSeats()))
            throw new InsufficientSeatsException("Pas assez de places disponibles");

        // Bloquer les places immédiatement (status PENDING)
        trip.bookSeats(req.getSeats());

        Booking booking = Booking.builder()
                .trip(trip).passenger(passenger)
                .seatsBooked(req.getSeats())
                .currency(req.getCurrency() != null ? Trip.Currency.valueOf(req.getCurrency()) : trip.getCurrency())
                .amountPaid(trip.getPricePerSeat().multiply(BigDecimal.valueOf(req.getSeats())))
                .status(Booking.BookingStatus.PENDING)  // ← conducteur doit valider
                .build();

        bookingRepository.save(booking);
        tripRepository.save(trip);

        // Notifier le conducteur
        notificationPort.notifyWithLink(
                trip.getDriver().getId(), "BOOKING",
                "Nouvelle demande de réservation",
                passenger.getFullName() + " souhaite rejoindre votre trajet vers " + trip.getArrivalCity(),
                "/trips/" + tripId + "/bookings"
        );
        auditPort.log("BOOKING_CREATED", "Booking", booking.getId(), null, null, passengerId);
        return toBookingResponse(booking);
    }

    // ── Conducteur valide ou refuse une réservation ───────────────────────
    public BookingResponse respondToBooking(String bookingId,
                                            RespondBookingRequest req, String driverId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Réservation introuvable"));
        String tripId = booking.getTrip().getId();
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new TripNotFoundException("Trajet introuvable"));
        if (!trip.getDriver().getId().equals(driverId))
            throw new NotAuthorizedException("Seul le conducteur peut répondre aux réservations");


        if (!booking.getTrip().getId().equals(tripId))
            throw new InvalidOperationException("Cette réservation n'appartient pas à ce trajet");
        if (!booking.isPending())
            throw new InvalidOperationException("Cette réservation n'est plus en attente (statut: " + booking.getStatus() + ")");

        if ("ACCEPTED".equals(req.getAction())) {
            booking.confirm();
            notificationPort.notifyWithLink(
                    booking.getPassenger().getId(), "BOOKING",
                    "Réservation acceptée ! 🎉",
                    "Le conducteur " + trip.getDriver().getFirstName() + " a accepté votre réservation pour " + trip.getArrivalCity(),
                    "/my-bookings"
            );
        } else {
            if (req.getReason() == null || req.getReason().isBlank())
                throw new InvalidOperationException("Une raison est requise pour refuser une réservation");
            booking.reject(req.getReason());
            tripRepository.save(trip); // places remises à jour
            notificationPort.notifyWithLink(
                    booking.getPassenger().getId(), "CANCELLATION",
                    "Réservation refusée",
                    "Le conducteur a refusé votre réservation. Raison : " + req.getReason(),
                    "/trips"
            );
        }

        bookingRepository.save(booking);
        auditPort.log("BOOKING_RESPONDED", "Booking", bookingId, null, req.getAction(), driverId);
        return toBookingResponse(booking);
    }

    // ── Passager quitte un trajet (PENDING ou CONFIRMED) ─────────────────
    public void leaveTrip(String tripId, String passengerId, String reason) {
        Booking booking = bookingRepository
                .findByTripIdAndPassengerId(tripId, passengerId)
                .orElseThrow(() -> new BookingNotFoundException("Réservation introuvable"));

        if (booking.getStatus() == Booking.BookingStatus.COMPLETED)
            throw new InvalidOperationException("Impossible d'annuler une réservation terminée");
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED)
            throw new InvalidOperationException("Cette réservation est déjà annulée");

        booking.cancelByPassenger(reason != null ? reason : "Annulé par le passager");
        bookingRepository.save(booking);

        Trip trip = booking.getTrip();
        tripRepository.save(trip);

        notificationPort.notify(
                trip.getDriver().getId(), "CANCELLATION",
                "Un passager a annulé",
                booking.getPassenger().getFullName() + " a annulé sa réservation pour " + trip.getArrivalCity()
        );
        auditPort.log("BOOKING_CANCELLED_PASSENGER", "Booking", booking.getId(), null, reason, passengerId);
    }

    // ── Annuler un trajet (conducteur) ────────────────────────────────────
    public void cancel(String tripId, String reason, String driverId) {
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new TripNotFoundException("Trajet introuvable"));
        if (!trip.getDriver().getId().equals(driverId))
            throw new NotAuthorizedException("Seul le conducteur peut annuler ce trajet");

        List<Booking> actives = trip.getBookings().stream()
                .filter(Booking::isActive).toList();
        actives.forEach(b -> {
            b.cancelByPassenger("Trajet annulé par le conducteur");
            notificationPort.notify(b.getPassenger().getId(), "CANCELLATION",
                    "Trajet annulé",
                    "Votre trajet vers " + trip.getArrivalCity() + " a été annulé par le conducteur");
        });

        trip.cancel(driverId);
        tripRepository.save(trip);
        eventPublisher.publishEvent(new TripCancelledEvent(tripId, driverId, reason, actives.size()));
        auditPort.log("TRIP_CANCELLED", "Trip", tripId, null, null, driverId);
    }

    // ── Terminer un trajet ────────────────────────────────────────────────
    public void complete(String tripId, String driverId) {
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new TripNotFoundException("Trajet introuvable"));
        if (!trip.getDriver().getId().equals(driverId))
            throw new NotAuthorizedException("Seul le conducteur peut terminer ce trajet");

        trip.complete();
        trip.getBookings().stream().filter(Booking::isConfirmed).forEach(b -> {
            b.complete();
            notificationPort.notifyWithLink(
                    b.getPassenger().getId(), "REVIEW",
                    "Trajet terminé — donnez votre avis",
                    "Comment s'est passé votre trajet avec " + trip.getDriver().getFirstName() + " ?",
                    "/reviews/new?tripId=" + tripId + "&targetId=" + trip.getDriver().getId());
        });
        tripRepository.save(trip);
        auditPort.log("TRIP_COMPLETED", "Trip", tripId, null, null, driverId);
    }

    // ── Mes trajets (conducteur) ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<TripResponse> getMyTrips(String driverId) {
        return tripRepository.findByDriverId(driverId).stream()
                .map(this::toResponse).toList();
    }

    // ── Mes réservations (passager) ────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String passengerId) {
        return bookingRepository.findByPassengerIdAndDeletedAtIsNull(passengerId).stream()
                .map(this::toBookingResponse).toList();
    }

    // ── Réservations d'un trajet (conducteur) ──────────────────────────────
    @Transactional(readOnly = true)
    public List<BookingResponse> getTripBookings(String tripId, String driverId) {
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new TripNotFoundException("Trajet introuvable"));
        if (!trip.getDriver().getId().equals(driverId))
            throw new NotAuthorizedException("Accès non autorisé");
        return trip.getBookings().stream()
                .filter(b -> b.getDeletedAt() == null)
                .map(this::toBookingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getTripBookingsByIdDriver(String driverId) {
        List<Trip> trips = tripRepository.findByDriverId(driverId);
        List<BookingResponse> bookingResponses = new ArrayList<>();

       /* trips.forEach(trip -> {
            if (!Objects.equals(trip.getDriver().getId(), driverId)) {
                throw new NotAuthorizedException("Seul le conducteur peut voir les reservation de son trajet.");
            }

            List<BookingResponse> responses = bookingRepository.findByTripId(trip.getId()).stream().map(this::toBookingResponse).toList();
            bookingResponses.addAll(responses);
        });
*/
        trips.forEach(trip -> {
            if (!Objects.equals(trip.getDriver().getId(), driverId)) {
                throw new NotAuthorizedException("Seul le conducteur peut voir les reservation de son trajet.");
            }

            List<BookingResponse> responses = trip.getBookings().stream().map(this::toBookingResponse).toList();
            bookingResponses.addAll(responses);
        });

        return bookingResponses;
    }
/*
    @Transactional(readOnly = true)
    public List<BookingResponse> getTripBookingsByIdDriver(String driverId) {

        return bookingRepository.findByTripDriverIdAndDeletedAtIsNull(driverId)
                .stream()
                .map(this::toBookingResponse)
                .toList();
    }
*/

    // ── Profil passager (visible par le conducteur du trajet) ─────────────
    @Transactional(readOnly = true)
    public PassengerPublicResponse getPassengerProfile(String tripId, String passengerId, String driverId) {
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new TripNotFoundException("Trajet introuvable"));
        if (!trip.getDriver().getId().equals(driverId))
            throw new NotAuthorizedException("Seul le conducteur peut voir les profils des passagers");

        boolean hasBooking = trip.getBookings().stream()
                .anyMatch(b -> b.getPassenger().getId().equals(passengerId) && b.getDeletedAt() == null);
        if (!hasBooking)
            throw new InvalidOperationException("Ce passager n'a pas de réservation sur ce trajet");

        User passenger = userRepository.findByIdAndDeletedAtIsNull(passengerId)
                .orElseThrow(() -> new UserNotFoundException("Passager introuvable"));

        return PassengerPublicResponse.builder()
                .id(passenger.getId())
                .firstName(passenger.getFirstName())
                .lastName(passenger.getLastName())
                .avatarUrl(passenger.getAvatarUrl())
                .rating(passenger.getRating())
                .reviewCount(passenger.getReviewCount())
                .identityVerified(passenger.isIdentityVerified())
                .phone(passenger.isPhoneVisible() ? passenger.getPhone() : null)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private void buildStopPoints(Trip trip, List<StopPointRequest> list) {
        if (list == null) return;
        list.forEach(sp -> trip.getStopPoints().add(StopPoint.builder()
                .trip(trip).city(sp.getCity()).address(sp.getAddress())
                .lat(sp.getLat()).lng(sp.getLng())
                .stopOrder(sp.getOrder()).arrivalTime(sp.getArrivalTime()).build()));
    }

    private void buildPreferences(Trip trip, TripPreferencesRequest p) {
        if (p == null) return;
        TripPreferences prefs = trip.getPreferences() != null
                ? trip.getPreferences()
                : TripPreferences.builder().trip(trip).build();
        prefs.setMusic(p.isMusic());
        prefs.setSmoking(p.isSmoking());
        prefs.setPets(p.isPets());
        prefs.setTalking(p.isTalking());
        prefs.setAirConditioning(p.isAirConditioning());
        prefs.setSmallLuggage(p.isSmallLuggage());
        prefs.setLargeLuggage(p.isLargeLuggage());
        trip.setPreferences(prefs);
    }

    private BookingResponse toBookingResponse(Booking b) {
        return BookingResponse.builder()
                .id(b.getId()).tripId(b.getTrip().getId())
                .status(b.getStatus().name())
                .tripStatus(b.getTrip().getStatus().name())
                .driverId(b.getTrip().getDriver().getId())
                .seatsBooked(b.getSeatsBooked())
                .amountPaid(b.getAmountPaid())
                .currency(b.getCurrency().name())
                .cancelReason(b.getCancelReason())
                .driverRejectReason(b.getDriverRejectReason())
                .createdAt(b.getCreatedAt())
                .driverResponseAt(b.getDriverResponseAt())
                .departureCity(b.getTrip().getDepartureCity())
                .arrivalCity(b.getTrip().getArrivalCity())
                .departureTime(b.getTrip().getDepartureTime())
                .pricePerSeat(b.getTrip().getPricePerSeat())
                .passenger(PassengerPublicResponse.builder()
                        .id(b.getPassenger().getId())
                        .firstName(b.getPassenger().getFirstName())
                        .lastName(b.getPassenger().getLastName())
                        .avatarUrl(b.getPassenger().getAvatarUrl())
                        .rating(b.getPassenger().getRating())
                        .reviewCount(b.getPassenger().getReviewCount())
                        .identityVerified(b.getPassenger().isIdentityVerified())
                        .build())
                .build();
    }

    public TripResponse toResponse(Trip t) {
        var prefs = t.getPreferences();
        return TripResponse.builder()
                .id(t.getId())
                .driver(TripUserResponse.builder()
                        .id(t.getDriver().getId()).firstName(t.getDriver().getFirstName())
                        .lastName(t.getDriver().getLastName()).avatarUrl(t.getDriver().getAvatarUrl())
                        .rating(t.getDriver().getRating()).verified(t.getDriver().isIdentityVerified())
                        .phone(t.getDriver().isPhoneVisible() ? t.getDriver().getPhone() : null).build())
                .departureCity(t.getDepartureCity()).arrivalCity(t.getArrivalCity())
                .departureAddress(t.getDepartureAddress()).arrivalAddress(t.getArrivalAddress())
                .departureTime(t.getDepartureTime()).arrivalTime(t.getArrivalTime())
                .availableSeats(t.getAvailableSeats()).totalSeats(t.getTotalSeats())
                .pricePerSeat(t.getPricePerSeat()).currency(t.getCurrency().name())
                .description(t.getDescription()).status(t.getStatus().name())
                .shareToken(t.getShareToken()).recurring(t.isRecurring())
                .frequency(t.getFrequency() != null ? t.getFrequency().name() : null)
                .stopPoints(t.getStopPoints().stream()
                        .sorted(Comparator.comparingInt(StopPoint::getStopOrder))
                        .map(sp -> StopPointResponse.builder()
                                .id(sp.getId()).city(sp.getCity()).address(sp.getAddress())
                                .order(sp.getStopOrder()).arrivalTime(sp.getArrivalTime()).build())
                        .toList())
                .passengers(t.getBookings().stream()
                        .filter(Booking::isActive)
                        .map(b -> PassengerResponse.builder()
                                .id(b.getPassenger().getId()).firstName(b.getPassenger().getFirstName())
                                .lastName(b.getPassenger().getLastName()).avatarUrl(b.getPassenger().getAvatarUrl())
                                .bookingId(b.getId()).bookingStatus(b.getStatus().name())
                                .bookedAt(b.getCreatedAt()).build())
                        .toList())
                .preferences(prefs != null ? TripPreferencesResponse.builder()
                        .music(prefs.isMusic()).smoking(prefs.isSmoking()).pets(prefs.isPets())
                        .talking(prefs.isTalking()).airConditioning(prefs.isAirConditioning())
                        .smallLuggage(prefs.isSmallLuggage()).largeLuggage(prefs.isLargeLuggage()).build() : null)
                .createdAt(t.getCreatedAt()).build();
    }

    public List<TripEditHistoryResponse> getHistory(String tripId, String driverId) {

        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new TripNotFoundException("Trajet introuvable"));
        if (!trip.getDriver().getId().equals(driverId))
            throw new NotAuthorizedException("Seul le conducteur peut consulter l'historique de son trajet");

        return tripEditHistoryRepository.findByTripIdOrderByCreatedAtDesc(tripId).stream()
                .map(tripHistoryMapper::toResponse).toList();
    }
}
