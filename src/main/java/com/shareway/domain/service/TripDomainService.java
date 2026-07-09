package com.shareway.domain.service;

import com.shareway.domain.exception.AccountBlockedException;
import com.shareway.domain.exception.InvalidOperationException;
import com.shareway.domain.exception.ResourceAlreadyExistsException;
import com.shareway.domain.model.Trip;
import com.shareway.domain.model.User;
import com.shareway.domain.repository.BookingRepository;
import com.shareway.domain.repository.TripRepository;
import com.shareway.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service domaine pour les règles métier des trajets.
 * Contient uniquement la logique de domaine pure.
 */
@Service
@RequiredArgsConstructor
public class TripDomainService {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(TripDomainService.class);

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    /**
     * Règle métier : un conducteur ne peut rejoindre son propre trajet
     */
    public void validateBooking(Trip trip, User passenger) {
        if (trip.getDriver().getId().equals(passenger.getId())) {
            LOGGER.error("Un conducteur ne peut pas réserver son propre voyage.");
            throw new InvalidOperationException("Un conducteur ne peut pas réserver son propre voyage.");
        }
        if (passenger.isBlocked()) {

            LOGGER.error("Votre compte a été bloqué: {}", passenger.getBlockReason());
            throw new AccountBlockedException("Votre compte a été bloqué: " + passenger.getBlockReason());
        }

        if (!passenger.isEmailVerified()) {
            LOGGER.error("Veuillez vérifier votre adresse e-mail avant de réserver.");
            throw new InvalidOperationException("Veuillez vérifier votre adresse e-mail avant de réserver.");
        }

        if (!trip.isOpen()) {
            LOGGER.error("Le voyage n'est pas disponible à la réservation. Status: {}", trip.getStatus());
            throw new InvalidOperationException("Le voyage n'est pas disponible à la réservation. Status: " + trip.getStatus());
        }

        if (bookingRepository.existsByTripIdAndPassengerIdAndDeletedAtIsNull(trip.getId(), passenger.getId())) {
            LOGGER.error("You already have a booking for this trip");
            throw new ResourceAlreadyExistsException("You already have a booking for this trip");
        }
    }

    /**
     * Règle métier : vérifier qu'un avis peut être laissé
     */
    public void validateReview(Trip trip, User author, User target) {
        if (trip.getStatus() != Trip.TripStatus.COMPLETED)
            throw new InvalidOperationException("You can only leave a review for completed trips");

        if (author.getId().equals(target.getId()))
            throw new InvalidOperationException("You cannot review yourself");

        // Vérifier la fenêtre de temps (7 jours)
        if (trip.getDepartureTime().isBefore(LocalDateTime.now().minusDays(7)))
            throw new InvalidOperationException("Review window has expired (7 days after trip)");
    }

    /**
     * Générer un token de partage unique
     */
    public String generateShareToken() {
        String token;
        do {
            token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        } while (tripRepository.findByShareTokenAndDeletedAtIsNull(token).isPresent());
        return token;
    }

    /**
     * Calculer le taux de remplissage moyen pour un conducteur
     */
    public double calculateAverageFillRate(String driverId) {
        List<Trip> trips = tripRepository.findByDriverId(driverId);
        if (trips.isEmpty()) return 0.0;
        return trips.stream()
                .filter(t -> t.getStatus() == Trip.TripStatus.COMPLETED)
                .mapToInt(Trip::getFillRate)
                .average()
                .orElse(0.0);
    }
}
