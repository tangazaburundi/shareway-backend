package com.shareway.application.usecase;

import com.shareway.application.dto.request.CreateRideRequest;
import com.shareway.application.dto.request.RateRideRequest;

import com.shareway.application.dto.request.UpdateDriverLocationRequest;
import com.shareway.application.dto.response.DriverAvailabilityResponse;
import com.shareway.application.dto.response.NearbyDriverResponse;
import com.shareway.application.dto.response.RideEstimateResponse;
import com.shareway.application.dto.response.RideRatingResponse;
import com.shareway.application.dto.response.RideResponse;
import com.shareway.application.port.out.AuditPort;
import com.shareway.application.port.out.NotificationPort;
import com.shareway.application.port.out.PushNotificationPort;
import com.shareway.domain.exception.DriverNotAvailableException;
import com.shareway.domain.exception.InvalidOperationException;
import com.shareway.domain.exception.InvalidRideStateException;
import com.shareway.domain.exception.NotAuthorizedException;
import com.shareway.domain.exception.RideNotFoundException;
import com.shareway.domain.exception.UserNotFoundException;
import com.shareway.domain.model.PaymentRefusal;
import com.shareway.domain.model.DriverAvailability;
import com.shareway.domain.model.RideRating;
import com.shareway.domain.model.RideRequest;
import com.shareway.domain.model.RideTracking;
import com.shareway.domain.model.Trip;
import com.shareway.domain.model.User;
import com.shareway.domain.model.Vehicle;
import com.shareway.domain.repository.DriverAvailabilityRepository;
import com.shareway.domain.repository.RideRatingRepository;
import com.shareway.domain.repository.RideRequestRepository;
import com.shareway.domain.repository.RideTrackingRepository;
import com.shareway.domain.repository.UserRepository;
import com.shareway.domain.repository.PricingConfigRepository;
import com.shareway.domain.repository.SmsConfigRepository;
import com.shareway.domain.repository.EmergencyContactRepository;
import com.shareway.domain.repository.AdminRoleRepository;
import com.shareway.domain.repository.SystemSettingRepository;
import com.shareway.domain.repository.UserDocumentRepository;
import com.shareway.domain.repository.FuelEntryRepository;
import com.shareway.domain.repository.PaymentRefusalRepository;
import com.shareway.domain.repository.RideRejectionRepository;
import com.shareway.domain.model.RideRejection;
import com.shareway.domain.model.FuelEntry;
import com.shareway.domain.model.UserDocument;
import com.shareway.domain.service.DriverMatchingService;
import com.shareway.domain.service.RidePricingService;
import com.shareway.domain.service.RideRequestDomainService;
import com.shareway.domain.model.PricingConfig;
import com.shareway.domain.model.SmsConfig;
import com.shareway.domain.model.EmergencyContact;
import com.shareway.application.dto.response.PricingConfigResponse;
import com.shareway.application.dto.response.SmsConfigResponse;
import com.shareway.application.port.out.SmsPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RideUseCase {

    private final RideRequestRepository rideRequestRepository;
    private final DriverAvailabilityRepository driverAvailabilityRepository;
    private final RideTrackingRepository rideTrackingRepository;
    private final RideRatingRepository rideRatingRepository;
    private final UserRepository userRepository;
    private final PricingConfigRepository pricingConfigRepository;
    private final SmsConfigRepository smsConfigRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final UserDocumentRepository userDocumentRepository;
    private final SmsPort smsPort;
    private final DriverMatchingService driverMatchingService;
    private final RidePricingService ridePricingService;
    private final RideRequestDomainService rideRequestDomainService;
    private final NotificationPort notificationPort;
    private final AuditPort auditPort;
    private final PushNotificationPort pushNotificationPort;
    private final SimpMessagingTemplate messaging;
    private final FuelEntryRepository fuelEntryRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final PaymentRefusalRepository paymentRefusalRepository;
    private final RideRejectionRepository rideRejectionRepository;

    // In-memory store for ride chat messages (ephemeral, lives until server restart)
    private final ConcurrentHashMap<String, List<Map<String, Object>>> rideMessages = new ConcurrentHashMap<>();

    // ════════════════════════════════════════════════════════════════
    // PASSENGER — Créer une demande de course
    // ════════════════════════════════════════════════════════════════

    public RideResponse createRideRequest(CreateRideRequest req, String passengerId) {
        User passenger = userRepository.findByIdAndDeletedAtIsNull(passengerId)
                .orElseThrow(() -> new UserNotFoundException("Passager introuvable"));

        // Vérifier qu'il n'a pas déjà une course active
        Optional<RideRequest> active = rideRequestRepository.findActiveByPassengerId(passengerId);
        if (active.isPresent()) {
            throw new InvalidOperationException("Vous avez déjà une course active");
        }

        // Calculer le prix estimé
        BigDecimal distance = driverMatchingService.calculateDistance(
                req.getPickupLat(), req.getPickupLng(),
                req.getDestinationLat(), req.getDestinationLng());
        int duration = driverMatchingService.estimateDurationMinutes(distance);

        String currency = req.getCurrency() != null ? req.getCurrency() : "FBU";
        RidePricingService.PricingResult pricing = ridePricingService.calculatePrice(distance, duration, currency);

        // Créer la demande
        RideRequest ride = RideRequest.builder()
                .passenger(passenger)
                .pickupLat(req.getPickupLat())
                .pickupLng(req.getPickupLng())
                .pickupAddress(req.getPickupAddress())
                .destinationLat(req.getDestinationLat())
                .destinationLng(req.getDestinationLng())
                .destinationAddress(req.getDestinationAddress())
                .estimatedDistanceKm(distance)
                .estimatedDurationMin(duration)
                .estimatedPrice(pricing.getEstimatedPrice())
                .currency(com.shareway.domain.model.Trip.Currency.valueOf(currency.toUpperCase()))
                .platformFeePercent(pricing.getPlatformFeePercent())
                .passengerCount(req.getPassengerCount())
                .notes(req.getNotes())
                .searchStartedAt(LocalDateTime.now())
                .searchTimeoutAt(LocalDateTime.now().plusMinutes(getSearchTimeoutMinutes()))
                .status(RideRequest.RideStatus.SEARCHING)
                .build();

        rideRequestRepository.save(ride);

        // Notifier les chauffeurs
        if (req.getDriverId() != null && !req.getDriverId().isBlank()) {
            // Empêcher un passager de réserver avec lui-même
            if (req.getDriverId().equals(passengerId)) {
                throw new InvalidOperationException("Vous ne pouvez pas réserver une course avec vous-même");
            }

            // Cibler un conducteur spécifique
            User targetedUser = userRepository.findByIdAndDeletedAtIsNull(req.getDriverId())
                    .orElseThrow(() -> new UserNotFoundException("Conducteur introuvable"));

            DriverAvailability targetedDriver = driverAvailabilityRepository.findByUser(targetedUser)
                    .orElseThrow(() -> new DriverNotAvailableException("Profil chauffeur introuvable"));

            if (!targetedDriver.isAvailable()) {
                throw new DriverNotAvailableException("Ce conducteur n'est plus disponible");
            }

            ride.setDriver(targetedUser);
            ride.setStatus(RideRequest.RideStatus.DRIVER_FOUND);
            ride.setDriverNotifiedAt(LocalDateTime.now());
            rideRequestRepository.save(ride);

            messaging.convertAndSendToUser(
                    targetedUser.getId(),
                    "/queue/ride-request",
                    buildRideRequestMessage(ride, passenger,
                            req.getPickupAddress(), req.getDestinationAddress(),
                            pricing.getEstimatedPrice(), distance, duration, currency, req.getPassengerCount()));

            notificationPort.notifyWithLink(
                    targetedUser.getId(), "BOOKING",
                    "Nouvelle demande de course",
                    "Un passager vous a sélectionné directement",
                    "/driver/ride/active"
            );
        } else {
            // Sélectionner automatiquement le chauffeur le plus proche
            List<DriverAvailability> nearby = driverMatchingService.findNearbyDrivers(
                    req.getPickupLat().doubleValue(), req.getPickupLng().doubleValue(), getRebroadcastRadiusKm());

            // Exclure le passager lui-même de la liste des chauffeurs
            nearby = nearby.stream()
                    .filter(da -> !da.getUser().getId().equals(passengerId))
                    .toList();

            if (nearby.isEmpty()) {
                throw new DriverNotAvailableException("Aucun chauffeur disponible à proximité");
            }

            // Le premier est le plus proche (trié par distance)
            DriverAvailability nearestDriver = nearby.get(0);
            User nearestUser = nearestDriver.getUser();

            ride.setDriver(nearestUser);
            ride.setStatus(RideRequest.RideStatus.DRIVER_FOUND);
            ride.setDriverNotifiedAt(LocalDateTime.now());
            rideRequestRepository.save(ride);

            messaging.convertAndSendToUser(
                    nearestUser.getId(),
                    "/queue/ride-request",
                    buildRideRequestMessage(ride, passenger,
                            req.getPickupAddress(), req.getDestinationAddress(),
                            pricing.getEstimatedPrice(), distance, duration, currency, req.getPassengerCount()));

            notificationPort.notifyWithLink(
                    nearestUser.getId(), "BOOKING",
                    "Nouvelle demande de course",
                    "Un passager cherche un chauffeur à proximité",
                    "/driver/ride/active"
            );
        }

        auditPort.log("RIDE_REQUESTED", "RideRequest", ride.getId(), null, null, passengerId);
        return toResponse(ride, pricing.getSurgeMultiplier());
    }

    // ════════════════════════════════════════════════════════════════
    // PASSENGER — Annuler une demande
    // ════════════════════════════════════════════════════════════════

    public void cancelRide(String rideId, String reason, String passengerId) {
        RideRequest ride = rideRequestRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Course introuvable"));

        if (!ride.getPassenger().getId().equals(passengerId))
            throw new NotAuthorizedException("Seul le passager peut annuler cette course");

        if (!ride.canBeCancelledByPassenger())
            throw new InvalidRideStateException("Impossible d'annuler cette course (statut: " + ride.getStatus() + ")");

        RideRequest.RideStatus previousStatus = ride.getStatus();
        ride.cancelByPassenger(reason);
        rideRequestRepository.save(ride);

        // Notifier TOUS les chauffeurs proches qui ont reçu la demande via WS topic
        if (previousStatus == RideRequest.RideStatus.DRIVER_FOUND || previousStatus == RideRequest.RideStatus.SEARCHING) {
            messaging.convertAndSend(
                    "/topic/ride/" + rideId + "/status",
                    Map.of(
                            "rideId", rideId,
                            "status", "CANCELLED",
                            "message", "La course a été annulée par le passager"
                    ));
        }

        // Libérer le chauffeur si assigné
        if (ride.getDriver() != null) {
            rideRequestDomainService.releaseDriver(ride.getDriver().getId());

            messaging.convertAndSendToUser(
                    ride.getDriver().getId(),
                    "/queue/ride-update",
                    Map.of(
                            "rideId", rideId,
                            "status", "CANCELLED",
                            "message", "Le passager a annulé la course"
                    ));

            notificationPort.notify(
                    ride.getDriver().getId(), "CANCELLATION",
                    "Course annulée",
                    "Le passager a annulé la course");
        }

        auditPort.log("RIDE_CANCELLED", "RideRequest", rideId, null, reason, passengerId);
    }

    // ════════════════════════════════════════════════════════════════
    // PASSENGER — Suivi de course active
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Optional<RideResponse> getActiveRide(String passengerId) {
        return rideRequestRepository.findActiveByPassengerId(passengerId)
                .map(r -> toResponse(r, null));
    }

    // ════════════════════════════════════════════════════════════════
    // PASSENGER — Historique des courses
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<RideResponse> getPassengerHistory(String passengerId) {
        return rideRequestRepository.findByPassengerIdOrderByCreatedAtDesc(passengerId).stream()
                .map(r -> toResponse(r, null))
                .toList();
    }

    // ════════════════════════════════════════════════════════════════
    // PASSENGER — Détail d'une course
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public RideResponse getRideById(String rideId, String userId) {
        RideRequest ride = rideRequestRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Course introuvable"));
        return toResponse(ride, null);
    }

    // ════════════════════════════════════════════════════════════════
    // PASSENGER — Noter une course
    // ════════════════════════════════════════════════════════════════

    public RideRatingResponse rateRide(String rideId, RateRideRequest req, String fromUserId) {
        RideRequest ride = rideRequestRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Course introuvable"));

        if (ride.getStatus() != RideRequest.RideStatus.COMPLETED)
            throw new InvalidOperationException("Vous ne pouvez noter qu'une course terminée");

        boolean alreadyRated = rideRatingRepository.findByRideRequestIdAndFromUserId(rideId, fromUserId).isPresent();
        if (alreadyRated)
            throw new InvalidOperationException("Vous avez déjà noté cette course");

        User fromUser = userRepository.findByIdAndDeletedAtIsNull(fromUserId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));
        User toUser = ride.getPassenger().getId().equals(fromUserId)
                ? ride.getDriver()
                : ride.getPassenger();

        RideRating rating = RideRating.builder()
                .rideRequest(ride)
                .fromUser(fromUser)
                .toUser(toUser)
                .rating(req.getRating())
                .comment(req.getComment())
                .build();

        rideRatingRepository.save(rating);

        // Mettre à jour la note moyenne de l'utilisateur noté
        toUser.updateRating(req.getRating());
        userRepository.save(toUser);

        auditPort.log("RIDE_RATED", "RideRating", rating.getId(), null,
                req.getRating() + "/5", fromUserId);

        return RideRatingResponse.builder()
                .id(rating.getId())
                .rideRequestId(rideId)
                .fromUserId(fromUserId)
                .fromUserFirstName(fromUser.getFirstName())
                .fromUserLastName(fromUser.getLastName())
                .fromUserAvatarUrl(fromUser.getAvatarUrl())
                .toUserId(toUser.getId())
                .rating(req.getRating())
                .comment(req.getComment())
                .createdAt(rating.getCreatedAt() != null ? rating.getCreatedAt().toString() : null)
                .build();
    }

    // ════════════════════════════════════════════════════════════════
    // RATINGS — Avis taxi reçus par un utilisateur
    // ════════════════════════════════════════════════════════════════

    public List<RideRatingResponse> getRideRatingsForUser(String userId) {
        return rideRatingRepository.findByToUserIdOrderByCreatedAtDesc(userId).stream()
                .map(r -> RideRatingResponse.builder()
                        .id(r.getId())
                        .rideRequestId(r.getRideRequest() != null ? r.getRideRequest().getId() : null)
                        .fromUserId(r.getFromUser().getId())
                        .fromUserFirstName(r.getFromUser().getFirstName())
                        .fromUserLastName(r.getFromUser().getLastName())
                        .fromUserAvatarUrl(r.getFromUser().getAvatarUrl())
                        .toUserId(r.getToUser().getId())
                        .rating(r.getRating())
                        .comment(r.getComment())
                        .type("RIDE")
                        .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null)
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════════
    // CHAUFFEUR — Accepter une demande
    // ════════════════════════════════════════════════════════════════

    public RideResponse acceptRide(String rideId, String driverId) {
        RideRequest ride = rideRequestRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Course introuvable"));

        if (ride.getStatus() != RideRequest.RideStatus.SEARCHING
                && ride.getStatus() != RideRequest.RideStatus.DRIVER_FOUND)
            throw new InvalidRideStateException("Cette course n'est plus disponible");

        User driver = userRepository.findByIdAndDeletedAtIsNull(driverId)
                .orElseThrow(() -> new UserNotFoundException("Conducteur introuvable"));

        DriverAvailability availability = driverAvailabilityRepository.findByDriverId(driverId)
                .orElseThrow(() -> new DriverNotAvailableException("Vous n'êtes pas en mode chauffeur"));

        if (!availability.isAvailable() && ride.getStatus() == RideRequest.RideStatus.SEARCHING)
            throw new DriverNotAvailableException("Vous n'êtes pas disponible");

        ride.accept(driver);
        availability.setOnTrip();
        resetConsecutiveRefusals(driverId);

        rideRequestRepository.save(ride);
        driverAvailabilityRepository.save(availability);

        // Notifier le passager
        notificationPort.notifyWithLink(
                ride.getPassenger().getId(), "BOOKING",
                "Chauffeur trouvé !",
                driver.getFirstName() + " a accepté votre course",
                "/ride/tracking/" + rideId
        );

        // Push WebSocket au passager
        messaging.convertAndSendToUser(
                ride.getPassenger().getId(),
                "/queue/ride-update",
                Map.of(
                        "rideId", rideId,
                        "status", "ACCEPTED",
                        "driverName", driver.getFullName(),
                        "driverPhone", driver.getPhone() != null ? driver.getPhone() : ""
                ));

        // Notifier les autres chauffeurs que la course est prise
        messaging.convertAndSend("/topic/ride/" + rideId + "/status",
                Map.of("rideId", rideId, "status", "ACCEPTED"));

        auditPort.log("RIDE_ACCEPTED", "RideRequest", rideId, null, null, driverId);
        return toResponse(ride, null);
    }

    // ════════════════════════════════════════════════════════════════
    // CHAUFFEUR — Refuser une demande
    // ════════════════════════════════════════════════════════════════

    public void rejectRide(String rideId, String driverId, String reason) {
        RideRequest ride = rideRequestRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Course introuvable"));

        if (!driverId.equals(ride.getDriver() != null ? ride.getDriver().getId() : null))
            throw new NotAuthorizedException("Vous n'êtes pas le chauffeur assigné");

        String rejectionReason = (reason != null && !reason.isBlank()) ? reason : "Aucun motif précisé";

        RideRejection rejection = RideRejection.builder()
                .rideId(rideId)
                .driverId(driverId)
                .passengerId(ride.getPassenger() != null ? ride.getPassenger().getId() : null)
                .reason(rejectionReason)
                .build();
        rideRejectionRepository.save(rejection);

        ride.reject();
        rideRequestDomainService.releaseDriver(driverId);

        // Pas de pénalité si le passager a déjà refusé de payer
        long passengerRefusals = paymentRefusalRepository.countByUserId(ride.getPassenger().getId());
        if (passengerRefusals == 0) {
            applyProgressiveRefusalPenalty(driverId);
        } else {
            log.info("Driver {} refusal exempted — passenger {} has {} payment refusal(s)",
                    driverId, ride.getPassenger().getId(), passengerRefusals);
        }

        rideRequestRepository.save(ride);

        messaging.convertAndSendToUser(
                ride.getPassenger().getId(),
                "/queue/ride-update",
                Map.of(
                        "rideId", rideId,
                        "status", "SEARCHING",
                        "message", "Le chauffeur a refusé (" + rejectionReason + "). Recherche du prochain chauffeur..."
                ));

        Map<String, Object> adminNotif = new java.util.HashMap<>();
        adminNotif.put("type", "RIDE_REJECTED");
        adminNotif.put("rideId", rideId);
        adminNotif.put("driverId", driverId);
        adminNotif.put("reason", rejectionReason);
        adminNotif.put("passengerName", ride.getPassenger().getFullName());
        adminNotif.put("timestamp", LocalDateTime.now().toString());
        messaging.convertAndSend("/topic/admin/ride-rejections", adminNotif);

        notificationPort.notifyWithLink(
                ride.getPassenger().getId(),
                "RIDE_REJECTED",
                "Chauffeur a refusé une course",
                "Motif: " + rejectionReason + " — Passager: " + ride.getPassenger().getFullName(),
                "/admin/trips"
        );

        List<DriverAvailability> nearby = driverMatchingService.findNearbyDrivers(
                ride.getPickupLat().doubleValue(), ride.getPickupLng().doubleValue(), getRebroadcastRadiusKm());

        // Exclure le chauffeur qui a refusé ET le passager lui-même
        List<DriverAvailability> candidates = nearby.stream()
                .filter(d -> !d.getUser().getId().equals(driverId))
                .filter(d -> !d.getUser().getId().equals(ride.getPassenger().getId()))
                .toList();

        if (candidates.isEmpty()) {
            ride.cancelByPassenger("Aucun autre chauffeur disponible après refus");
            rideRequestRepository.save(ride);

            messaging.convertAndSendToUser(
                    ride.getPassenger().getId(),
                    "/queue/ride-update",
                    Map.of(
                            "rideId", rideId,
                            "status", "EXPIRED",
                            "message", "Aucun autre chauffeur n'est disponible. Votre course a été annulée."
                    ));

            notificationPort.notify(
                    ride.getPassenger().getId(),
                    "CANCELLATION",
                    "Course annulée",
                    "Aucun chauffeur disponible après refus.");

            log.info("Ride {} cancelled — no more drivers within 2km after rejection", rideId);
        } else {
            // Assigner au premier (le plus proche) pour que le scheduler puisse gérer le timeout
            DriverAvailability nearest = candidates.get(0);
            ride.setDriver(nearest.getUser());
            ride.setStatus(RideRequest.RideStatus.DRIVER_FOUND);
            ride.setDriverNotifiedAt(LocalDateTime.now());
            rideRequestRepository.save(ride);

            // Notifier TOUS les candidats dans les 2km
            for (DriverAvailability candidate : candidates) {
                messaging.convertAndSendToUser(
                        candidate.getUser().getId(),
                        "/queue/ride-request",
                        buildRideRequestMessage(ride, ride.getPassenger(),
                                ride.getPickupAddress(), ride.getDestinationAddress(),
                                ride.getEstimatedPrice(),
                                ride.getEstimatedDistanceKm(),
                                ride.getEstimatedDurationMin() != null ? ride.getEstimatedDurationMin() : 0,
                                ride.getCurrency().name(), ride.getPassengerCount()));

                notificationPort.notifyWithLink(
                        candidate.getUser().getId(), "BOOKING",
                        "Nouvelle demande de course",
                        "Un passager cherche un chauffeur à proximité",
                        "/driver/ride/active"
                );
            }

            log.info("Ride {} sent to {} drivers within 2km after rejection from {}", rideId, candidates.size(), driverId);
        }

        auditPort.log("RIDE_REJECTED", "RideRequest", rideId, null, "Reason: " + rejectionReason, driverId);
    }

    // ════════════════════════════════════════════════════════════════
    // CHAUFFEUR — Timeout (pas de réponse à temps)
    // ════════════════════════════════════════════════════════════════

    public void timeoutRide(String rideId, String driverId) {
        RideRequest ride = rideRequestRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Course introuvable"));

        if (!driverId.equals(ride.getDriver() != null ? ride.getDriver().getId() : null))
            throw new NotAuthorizedException("Vous n'êtes pas le chauffeur assigné");

        ride.reject();
        rideRequestDomainService.releaseDriver(driverId);
        rideRequestRepository.save(ride);

        messaging.convertAndSendToUser(
                ride.getPassenger().getId(),
                "/queue/ride-update",
                Map.of(
                        "rideId", rideId,
                        "status", "SEARCHING",
                        "message", "Le chauffeur n'a pas pu répondre à temps. Recherche du prochain chauffeur..."
                ));

        notificationPort.notify(
                ride.getPassenger().getId(),
                "RIDE_TIMEOUT",
                "Chauffeur indisponible",
                "Le chauffeur n'a pas pu répondre à temps. Recherche d'un autre chauffeur..."
        );

        List<DriverAvailability> nearby = driverMatchingService.findNearbyDrivers(
                ride.getPickupLat().doubleValue(), ride.getPickupLng().doubleValue(), getRebroadcastRadiusKm());

        List<DriverAvailability> candidates = nearby.stream()
                .filter(d -> !d.getUser().getId().equals(driverId) && d.isAvailable())
                .filter(d -> !d.getUser().getId().equals(ride.getPassenger().getId()))
                .toList();

        if (candidates.isEmpty()) {
            ride.cancelByPassenger("Aucun autre chauffeur disponible après timeout");
            rideRequestRepository.save(ride);

            messaging.convertAndSendToUser(
                    ride.getPassenger().getId(),
                    "/queue/ride-update",
                    Map.of(
                            "rideId", rideId,
                            "status", "EXPIRED",
                            "message", "Aucun autre chauffeur n'est disponible. Votre course a été annulée."
                    ));

            notificationPort.notify(
                    ride.getPassenger().getId(),
                    "CANCELLATION",
                    "Course annulée",
                    "Aucun chauffeur n'a répondu à temps.");

            log.info("Ride {} cancelled — no more drivers within 2km after timeout", rideId);
        } else {
            DriverAvailability nearest = candidates.get(0);
            ride.setDriver(nearest.getUser());
            ride.setStatus(RideRequest.RideStatus.DRIVER_FOUND);
            ride.setDriverNotifiedAt(LocalDateTime.now());
            rideRequestRepository.save(ride);

            for (DriverAvailability candidate : candidates) {
                messaging.convertAndSendToUser(
                        candidate.getUser().getId(),
                        "/queue/ride-request",
                        buildRideRequestMessage(ride, ride.getPassenger(),
                                ride.getPickupAddress(), ride.getDestinationAddress(),
                                ride.getEstimatedPrice(),
                                ride.getEstimatedDistanceKm(),
                                ride.getEstimatedDurationMin() != null ? ride.getEstimatedDurationMin() : 0,
                                ride.getCurrency().name(), ride.getPassengerCount()));

                notificationPort.notifyWithLink(
                        candidate.getUser().getId(), "BOOKING",
                        "Nouvelle demande de course",
                        "Un passager cherche un chauffeur à proximité",
                        "/driver/ride/active"
                );
            }

            log.info("Ride {} sent to {} drivers within 2km after timeout from {}", rideId, candidates.size(), driverId);
        }

        auditPort.log("RIDE_TIMEOUT", "RideRequest", rideId, null, "Driver did not respond in time", driverId);
    }

    // ════════════════════════════════════════════════════════════════
    // CHAUFFEUR — Mettre à jour le statut de la course
    // ════════════════════════════════════════════════════════════════

    public RideResponse driverEnRoute(String rideId, String driverId) {
        RideRequest ride = getAndValidateDriverRide(rideId, driverId);
        rideRequestDomainService.validateTransition(ride, RideRequest.RideStatus.DRIVER_EN_ROUTE);
        ride.driverEnRoute();
        rideRequestRepository.save(ride);

        notifyPassenger(ride, "DRIVER_EN_ROUTE", "Le chauffeur est en route vers vous");
        return toResponse(ride, null);
    }

    public RideResponse driverArrived(String rideId, String driverId) {
        RideRequest ride = getAndValidateDriverRide(rideId, driverId);
        rideRequestDomainService.validateTransition(ride, RideRequest.RideStatus.ARRIVED);
        ride.driverArrived();
        rideRequestRepository.save(ride);

        notifyPassenger(ride, "DRIVER_ARRIVED", "Le chauffeur est arrivé au point de prise en charge");
        return toResponse(ride, null);
    }

    public RideResponse startRide(String rideId, String driverId) {
        RideRequest ride = getAndValidateDriverRide(rideId, driverId);
        rideRequestDomainService.validateTransition(ride, RideRequest.RideStatus.IN_PROGRESS);
        ride.start();
        rideRequestRepository.save(ride);

        notifyPassenger(ride, "RIDE_IN_PROGRESS", "La course a commencé");
        return toResponse(ride, null);
    }

    public RideResponse completeRide(String rideId, String driverId) {
        RideRequest ride = getAndValidateDriverRide(rideId, driverId);
        rideRequestDomainService.validateTransition(ride, RideRequest.RideStatus.COMPLETED);

        ride.complete(ride.getEstimatedPrice());
        rideRequestDomainService.releaseDriver(driverId);
        rideRequestRepository.save(ride);

        // Notifier le passager
        messaging.convertAndSendToUser(
                ride.getPassenger().getId(),
                "/queue/ride-update",
                Map.of(
                        "rideId", rideId,
                        "status", "COMPLETED",
                        "finalPrice", ride.getFinalPrice() != null ? ride.getFinalPrice() : ride.getEstimatedPrice(),
                        "currency", ride.getCurrency().name()
                ));

        notificationPort.notifyWithLink(
                ride.getPassenger().getId(), "REVIEW",
                "Course terminée",
                "Merci d'avoir utilisé ShareWay ! Donnez votre avis.",
                "/ride/" + rideId + "/rate");

        auditPort.log("RIDE_COMPLETED", "RideRequest", rideId, null, null, driverId);
        return toResponse(ride, null);
    }

    // ════════════════════════════════════════════════════════════════
    // CHAUFFEUR — Annuler une course assignée
    // ════════════════════════════════════════════════════════════════

    public void driverCancelRide(String rideId, String reason, String driverId) {
        RideRequest ride = getAndValidateDriverRide(rideId, driverId);

        if (!ride.canBeCancelledByDriver())
            throw new InvalidRideStateException("Impossible d'annuler cette course");

        ride.cancelByDriver(reason);
        rideRequestDomainService.releaseDriver(driverId);
        resetConsecutiveRefusals(driverId);
        applyCooldownPenalty(driverId);
        rideRequestRepository.save(ride);

        // Notifier le passager
        messaging.convertAndSendToUser(
                ride.getPassenger().getId(),
                "/queue/ride-update",
                Map.of("rideId", rideId, "status", "CANCELLED", "message", "Le chauffeur a annulé la course"));

        notificationPort.notify(
                ride.getPassenger().getId(), "CANCELLATION",
                "Course annulée",
                "Le chauffeur a annulé votre course");

        auditPort.log("RIDE_CANCELLED_DRIVER", "RideRequest", rideId, null, reason, driverId);
    }

    // ════════════════════════════════════════════════════════════════
    // CHAUFFEUR — Course active
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Optional<RideResponse> getDriverActiveRide(String driverId) {
        return rideRequestRepository.findActiveByDriverId(driverId)
                .map(r -> toResponse(r, null));
    }

    // ════════════════════════════════════════════════════════════════
    // CHAUFFEUR — Historique
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<RideResponse> getDriverHistory(String driverId) {
        List<RideResponse> rides = rideRequestRepository.findByDriverIdOrderByCreatedAtDesc(driverId).stream()
                .map(r -> toResponse(r, null))
                .toList();

        List<RideResponse> rejections = rideRejectionRepository.findByDriverIdOrderByCreatedAtDesc(driverId).stream()
                .map(rejection -> {
                    RideRequest ride = rideRequestRepository.findById(rejection.getRideId()).orElse(null);
                    RideResponse resp = ride != null ? toResponse(ride, null) : new RideResponse();
                    resp.setRejectedByDriver(true);
                    resp.setRejectionReason(rejection.getReason());
                    resp.setRejectedAt(rejection.getCreatedAt());
                    if (ride != null) {
                        resp.setId(ride.getId());
                    } else {
                        resp.setId(rejection.getRideId());
                    }
                    return resp;
                })
                .toList();

        List<RideResponse> merged = new java.util.ArrayList<>(rides);
        merged.addAll(rejections);
        merged.sort((a, b) -> {
            var ta = a.getCreatedAt() != null ? a.getCreatedAt() : a.getRejectedAt();
            var tb = b.getCreatedAt() != null ? b.getCreatedAt() : b.getRejectedAt();
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });
        return merged;
    }

    // ════════════════════════════════════════════════════════════════
    // HISTORY — Filtré par statut (passager + chauffeur)
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<RideResponse> getPassengerHistoryByStatus(String passengerId, RideRequest.RideStatus status) {
        return rideRequestRepository.findByPassengerIdAndStatusOrderByCreatedAtDesc(passengerId, status).stream()
                .map(r -> toResponse(r, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RideResponse> getDriverHistoryByStatus(String driverId, RideRequest.RideStatus status) {
        return rideRequestRepository.findByDriverIdAndStatusOrderByCreatedAtDesc(driverId, status).stream()
                .map(r -> toResponse(r, null))
                .toList();
    }

    // ════════════════════════════════════════════════════════════════
    // ARCHIVE — Archiver une course
    // ════════════════════════════════════════════════════════════════

    @Transactional
    public void archiveRide(String rideId, String userId) {
        RideRequest ride = rideRequestRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Course introuvable"));

        boolean isPassenger = ride.getPassenger() != null && ride.getPassenger().getId().equals(userId);
        boolean isDriver = ride.getDriver() != null && ride.getDriver().getId().equals(userId);
        if (!isPassenger && !isDriver) {
            throw new RuntimeException("Vous n'avez pas accès à cette course");
        }

        ride.archive();
        rideRequestRepository.save(ride);
    }

    public RideResponse payRide(String rideId, String userId) {
        RideRequest ride = rideRequestRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Course introuvable"));

        boolean isDriver = ride.getDriver() != null && ride.getDriver().getId().equals(userId);
        if (!isDriver) {
            throw new RuntimeException("Seul le chauffeur peut confirmer la réception du paiement");
        }

        ride.markAsPaid();
        rideRequestRepository.save(ride);

        messaging.convertAndSendToUser(
                ride.getPassenger().getId(),
                "/queue/ride-update",
                Map.of(
                        "rideId", ride.getId(),
                        "status", ride.getStatus().name(),
                        "paymentStatus", "CAPTURED",
                        "message", "Paiement confirmé par le chauffeur"
                ));

        return toResponse(ride, null);
    }

    public RideResponse confirmPaymentRefused(String rideId, String userId) {
        RideRequest ride = rideRequestRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Course introuvable"));

        boolean isDriver = ride.getDriver() != null && ride.getDriver().getId().equals(userId);
        if (!isDriver) {
            throw new RuntimeException("Seul le chauffeur peut confirmer le refus de paiement");
        }
        if (ride.getStatus() != RideRequest.RideStatus.COMPLETED) {
            throw new RuntimeException("La course doit être terminée pour confirmer le refus");
        }
        if (ride.getPaymentStatus() == RideRequest.PaymentStatus.CAPTURED) {
            throw new RuntimeException("Le paiement a déjà été confirmé");
        }
        if (ride.getPaymentStatus() == RideRequest.PaymentStatus.REFUSED) {
            throw new RuntimeException("Le refus de paiement a déjà été confirmé");
        }

        ride.markAsRefused();
        rideRequestRepository.save(ride);

        BigDecimal originalAmount = ride.getFinalPrice() != null ? ride.getFinalPrice() : ride.getEstimatedPrice();
        if (originalAmount == null) originalAmount = BigDecimal.ZERO;
        String currency = ride.getCurrency() != null ? ride.getCurrency().name() : "FBU";

        BigDecimal feePercent = getSettingDecimal("ride.unpaid_fee_percent", "10");
        BigDecimal fineAmount = getSettingDecimal("ride.unpaid_fine_amount", "5000");
        BigDecimal feeAmount = originalAmount.multiply(feePercent).divide(new BigDecimal("100"));
        BigDecimal totalDebt = originalAmount.add(feeAmount).add(fineAmount);

        User passenger = ride.getPassenger();

        PaymentRefusal refusal = PaymentRefusal.builder()
                .rideId(rideId)
                .userId(passenger.getId())
                .userFirstName(passenger.getFirstName())
                .userLastName(passenger.getLastName())
                .pickupAddress(ride.getPickupAddress())
                .destinationAddress(ride.getDestinationAddress())
                .estimatedDistanceKm(ride.getEstimatedDistanceKm())
                .originalAmount(originalAmount)
                .feeAmount(feeAmount)
                .fineAmount(fineAmount)
                .totalDebt(totalDebt)
                .currency(currency)
                .resolved(false)
                .build();
        paymentRefusalRepository.save(refusal);

        passenger.block("Refus de paiement — dette: " + totalDebt + " " + currency, "SYSTEM");
        passenger.addDebt(totalDebt, currency);

        long totalRefusals1 = paymentRefusalRepository.countByUserId(passenger.getId());
        if (totalRefusals1 >= 3) {
            passenger.setPermanentlyLocked(true);
            passenger.setActive(false);
            passenger.setBlockReason("Banni — " + totalRefusals1 + " refus de paiement");
        }
        userRepository.save(passenger);

        List<String> adminIds = adminRoleRepository.findAllUserIds();
        for (String adminId : adminIds) {
            notificationPort.notifyWithLink(
                    adminId,
                    "PAYMENT",
                    "Refus de paiement confirmé par le chauffeur",
                    "Le chauffeur " + ride.getDriver().getFirstName() + " " + ride.getDriver().getLastName()
                            + " a confirmé que " + passenger.getFirstName() + " " + passenger.getLastName()
                            + " a refusé de payer " + originalAmount + " " + currency
                            + " pour la course " + rideId
                            + ". Dette totale (avec frais + amende): " + totalDebt + " " + currency
                            + ". Utilisateur blacklisté.",
                    "/ride/tracking/" + rideId
            );
        }

        messaging.convertAndSendToUser(
                passenger.getId(),
                "/queue/ride-update",
                Map.of(
                        "rideId", ride.getId(),
                        "status", ride.getStatus().name(),
                        "paymentStatus", "REFUSED",
                        "totalDebt", totalDebt.toPlainString(),
                        "currency", currency,
                        "message", "Le chauffeur a confirmé le refus de paiement. Vous êtes blacklisté jusqu'au règlement de " + totalDebt + " " + currency
                ));

        return toResponse(ride, null);
    }

    public RideResponse refusePayment(String rideId, String userId) {
        RideRequest ride = rideRequestRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Course introuvable"));

        boolean isPassenger = ride.getPassenger() != null && ride.getPassenger().getId().equals(userId);
        if (!isPassenger) {
            throw new RuntimeException("Seul le passager peut refuser de payer");
        }
        if (ride.getStatus() != RideRequest.RideStatus.COMPLETED) {
            throw new RuntimeException("La course doit être terminée pour refuser le paiement");
        }
        if (ride.getPaymentStatus() == RideRequest.PaymentStatus.CAPTURED) {
            throw new RuntimeException("Le paiement a déjà été confirmé");
        }

        BigDecimal originalAmount = ride.getFinalPrice() != null ? ride.getFinalPrice() : ride.getEstimatedPrice();
        if (originalAmount == null) originalAmount = BigDecimal.ZERO;
        String currency = ride.getCurrency() != null ? ride.getCurrency().name() : "FBU";

        BigDecimal feePercent = getSettingDecimal("ride.unpaid_fee_percent", "10");
        BigDecimal fineAmount = getSettingDecimal("ride.unpaid_fine_amount", "5000");
        BigDecimal feeAmount = originalAmount.multiply(feePercent).divide(new BigDecimal("100"));
        BigDecimal totalDebt = originalAmount.add(feeAmount).add(fineAmount);

        User passenger = ride.getPassenger();

        PaymentRefusal refusal = PaymentRefusal.builder()
                .rideId(rideId)
                .userId(passenger.getId())
                .userFirstName(passenger.getFirstName())
                .userLastName(passenger.getLastName())
                .pickupAddress(ride.getPickupAddress())
                .destinationAddress(ride.getDestinationAddress())
                .estimatedDistanceKm(ride.getEstimatedDistanceKm())
                .originalAmount(originalAmount)
                .feeAmount(feeAmount)
                .fineAmount(fineAmount)
                .totalDebt(totalDebt)
                .currency(currency)
                .resolved(false)
                .build();
        paymentRefusalRepository.save(refusal);

        passenger.block("Refus de paiement — dette: " + totalDebt + " " + currency, "SYSTEM");
        passenger.addDebt(totalDebt, currency);

        long totalRefusals2 = paymentRefusalRepository.countByUserId(passenger.getId());
        if (totalRefusals2 >= 3) {
            passenger.setPermanentlyLocked(true);
            passenger.setActive(false);
            passenger.setBlockReason("Banni — " + totalRefusals2 + " refus de paiement");
        }
        userRepository.save(passenger);

        List<String> adminIds = adminRoleRepository.findAllUserIds();
        for (String adminId : adminIds) {
            notificationPort.notifyWithLink(
                    adminId,
                    "PAYMENT",
                    "Refus de paiement",
                    passenger.getFirstName() + " " + passenger.getLastName()
                            + " a refusé de payer " + originalAmount + " " + currency
                            + " pour la course " + rideId
                            + ". Dette totale (avec frais + amende): " + totalDebt + " " + currency,
                    "/ride/tracking/" + rideId
            );
        }

        messaging.convertAndSendToUser(
                userId,
                "/queue/ride-update",
                Map.of(
                        "rideId", ride.getId(),
                        "status", ride.getStatus().name(),
                        "paymentStatus", "REFUSED",
                        "totalDebt", totalDebt.toPlainString(),
                        "currency", currency,
                        "message", "Paiement refusé. Vous êtes temporairement bloqué jusqu'au règlement de " + totalDebt + " " + currency
                ));

        return toResponse(ride, null);
    }

    private BigDecimal getSettingDecimal(String key, String defaultValue) {
        return systemSettingRepository.findByKey(key)
                .map(s -> new BigDecimal(s.getValue()))
                .orElse(new BigDecimal(defaultValue));
    }

    // ════════════════════════════════════════════════════════════════
    // CHAUFFEUR — Toggle disponibilité
    // ════════════════════════════════════════════════════════════════

    public DriverAvailabilityResponse toggleAvailability(String driverId) {
        DriverAvailability availability = driverAvailabilityRepository.findByDriverId(driverId)
                .orElseGet(() -> {
                    User driver = userRepository.findByIdAndDeletedAtIsNull(driverId)
                            .orElseThrow(() -> new UserNotFoundException("Conducteur introuvable"));
                    return DriverAvailability.builder()
                            .user(driver)
                            .status(DriverAvailability.DriverStatus.OFFLINE)
                            .build();
                });

        if (availability.getStatus() == DriverAvailability.DriverStatus.BUSY
                || availability.getStatus() == DriverAvailability.DriverStatus.ON_TRIP) {
            throw new InvalidOperationException("Impossible de changer de statut pendant une course");
        }

        if (!availability.isAvailable()) {
            User driver = userRepository.findByIdAndDeletedAtIsNull(driverId).orElse(null);
            if (driver != null && driver.getBlockedUntil() != null
                    && driver.getBlockedUntil().isAfter(LocalDateTime.now())) {
                long remaining = java.time.Duration.between(LocalDateTime.now(), driver.getBlockedUntil()).toSeconds();
                throw new InvalidOperationException("Cooldown actif. Veuillez patienter " + (remaining / 60) + " min " + (remaining % 60) + " sec avant de vous remettre en ligne.");
            }
        }

        if (availability.isAvailable()) {
            availability.goOffline();
        } else {
            availability.goOnline();
        }

        driverAvailabilityRepository.save(availability);
        return toAvailabilityResponse(availability);
    }

    // ════════════════════════════════════════════════════════════════
    // CHAUFFEUR — Mettre à jour la position GPS
    // ════════════════════════════════════════════════════════════════

    public void updateDriverLocation(String driverId, UpdateDriverLocationRequest req) {
        DriverAvailability availability = driverAvailabilityRepository.findByDriverId(driverId)
                .orElse(null);

        if (availability == null) {
            User driver = userRepository.findById(driverId)
                    .orElseThrow(() -> new UserNotFoundException(driverId));
            availability = DriverAvailability.builder()
                    .user(driver)
                    .isAvailable(false)
                    .status(DriverAvailability.DriverStatus.OFFLINE)
                    .build();
            driverAvailabilityRepository.save(availability);
        }

        availability.updateLocation(req.getLat(), req.getLng(), req.getHeading());
        driverAvailabilityRepository.save(availability);

        // Si le chauffeur a une course active, envoyer la position au passager
        rideRequestRepository.findActiveByDriverId(driverId).ifPresent(ride -> {
            // Sauvegarder le tracking
            RideTracking tracking = RideTracking.builder()
                    .rideRequest(ride)
                    .lat(req.getLat())
                    .lng(req.getLng())
                    .heading(req.getHeading())
                    .build();
            rideTrackingRepository.save(tracking);

            // Envoyer via WebSocket
            messaging.convertAndSendToUser(
                    ride.getPassenger().getId(),
                    "/queue/driver-location",
                    Map.of(
                            "rideId", ride.getId(),
                            "lat", req.getLat(),
                            "lng", req.getLng(),
                            "heading", req.getHeading() != null ? req.getHeading() : 0
                    ));

            // Aussi sur le topic de la course
            messaging.convertAndSend(
                    "/topic/ride/" + ride.getId() + "/tracking",
                    Map.of(
                            "lat", req.getLat(),
                            "lng", req.getLng(),
                            "heading", req.getHeading() != null ? req.getHeading() : 0
                    ));
        });
    }

    // ════════════════════════════════════════════════════════════════
    // CHAUFFEUR — Statut de disponibilité
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public DriverAvailabilityResponse getDriverAvailability(String driverId) {
        return driverAvailabilityRepository.findByDriverId(driverId)
                .map(this::toAvailabilityResponse)
                .orElse(DriverAvailabilityResponse.builder()
                        .userId(driverId)
                        .available(false)
                        .status("OFFLINE")
                        .build());
    }

    // ════════════════════════════════════════════════════════════════
    // CHAUFFEUR — Chauffeurs proches (debug/admin)
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<NearbyDriverResponse> getNearbyDrivers(double lat, double lng, int radiusKm) {
        List<DriverAvailability> nearby = driverMatchingService.findNearbyDrivers(lat, lng, radiusKm);
        return nearby.stream().map(a -> toNearbyDriverResponse(a, lat, lng)).toList();
    }

    // ════════════════════════════════════════════════════════════════
    // PUBLIC — Estimation de prix
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public RideEstimateResponse getEstimate(BigDecimal pickupLat, BigDecimal pickupLng,
                                            BigDecimal destLat, BigDecimal destLng, String currency) {
        BigDecimal distance = driverMatchingService.calculateDistance(pickupLat, pickupLng, destLat, destLng);
        int duration = driverMatchingService.estimateDurationMinutes(distance);
        RidePricingService.PricingResult pricing = ridePricingService.calculatePrice(distance, duration, currency);

        long nearbyCount = driverAvailabilityRepository.findNearbyDriversWithinRadius(
                pickupLat.doubleValue(), pickupLng.doubleValue(), getRebroadcastRadiusKm()).size();

        return RideEstimateResponse.builder()
                .distanceKm(distance)
                .durationMin(duration)
                .estimatedPrice(pricing.getEstimatedPrice())
                .surgeMultiplier(pricing.getSurgeMultiplier())
                .currency(pricing.getCurrency().name())
                .basePrice(pricing.getBasePrice())
                .pricePerKm(pricing.getPricePerKm())
                .pricePerMin(pricing.getPricePerMin())
                .platformFeePercent(pricing.getPlatformFeePercent())
                .surgeActive(pricing.getSurgeMultiplier().compareTo(BigDecimal.ONE) > 0)
                .nearbyDriversCount((int) nearbyCount)
                .build();
    }

    // ════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════

    private RideRequest getAndValidateDriverRide(String rideId, String driverId) {
        RideRequest ride = rideRequestRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Course introuvable"));

        if (ride.getDriver() == null || !ride.getDriver().getId().equals(driverId))
            throw new NotAuthorizedException("Vous n'êtes pas le chauffeur assigné");

        return ride;
    }

    // ════════════════════════════════════════════════════════════════
    // ADMIN — Pricing Config
    // ════════════════════════════════════════════════════════════════

    public List<PricingConfigResponse> getAllPricingConfigs() {
        return pricingConfigRepository.findAll().stream()
                .map(this::toPricingConfigResponse)
                .toList();
    }

    public PricingConfigResponse getPricingConfig(String id) {
        PricingConfig config = pricingConfigRepository.findById(id)
                .orElseThrow(() -> new RideNotFoundException("Pricing config introuvable"));
        return toPricingConfigResponse(config);
    }

    public PricingConfigResponse createPricingConfig(PricingConfigResponse req) {
        PricingConfig config = PricingConfig.builder()
                .name(req.getName())
                .currency(Trip.Currency.valueOf(req.getCurrency()))
                .basePrice(req.getBasePrice())
                .pricePerKm(req.getPricePerKm())
                .pricePerMin(req.getPricePerMin())
                .minimumPrice(req.getMinimumPrice())
                .surgeMultiplier(req.getSurgeMultiplier())
                .surgeThreshold(req.getSurgeThreshold())
                .platformFeePercent(req.getPlatformFeePercent())
                .freeCancellationMinutes(req.getFreeCancellationMinutes())
                .active(req.isActive())
                .build();
        config = pricingConfigRepository.save(config);
        return toPricingConfigResponse(config);
    }

    public PricingConfigResponse updatePricingConfig(String id, PricingConfigResponse req) {
        PricingConfig config = pricingConfigRepository.findById(id)
                .orElseThrow(() -> new RideNotFoundException("Pricing config introuvable"));
        config.setName(req.getName());
        config.setCurrency(Trip.Currency.valueOf(req.getCurrency()));
        config.setBasePrice(req.getBasePrice());
        config.setPricePerKm(req.getPricePerKm());
        config.setPricePerMin(req.getPricePerMin());
        config.setMinimumPrice(req.getMinimumPrice());
        config.setSurgeMultiplier(req.getSurgeMultiplier());
        config.setSurgeThreshold(req.getSurgeThreshold());
        config.setPlatformFeePercent(req.getPlatformFeePercent());
        config.setFreeCancellationMinutes(req.getFreeCancellationMinutes());
        config.setActive(req.isActive());
        config = pricingConfigRepository.save(config);
        return toPricingConfigResponse(config);
    }

    public void deletePricingConfig(String id) {
        pricingConfigRepository.deleteById(id);
    }

    // ════════════════════════════════════════════════════════════════
    // ADMIN — SMS Config
    // ════════════════════════════════════════════════════════════════

    public SmsConfigResponse getSmsConfig() {
        SmsConfig config = smsConfigRepository.findTopByOrderByIdAsc()
                .orElseGet(() -> smsConfigRepository.save(SmsConfig.builder().build()));
        return toSmsConfigResponse(config);
    }

    public SmsConfigResponse updateSmsConfig(SmsConfigResponse req) {
        SmsConfig config = smsConfigRepository.findTopByOrderByIdAsc()
                .orElseGet(() -> SmsConfig.builder().build());
        config.setProvider(SmsConfig.SmsProvider.valueOf(req.getProvider()));
        config.setEnabled(req.isEnabled());
        config.setApiKey(req.getApiKey());
        config.setApiSecret(req.getApiSecret());
        config.setSenderNumber(req.getSenderNumber());
        config.setSenderName(req.getSenderName());
        config = smsConfigRepository.save(config);
        return toSmsConfigResponse(config);
    }

    // ════════════════════════════════════════════════════════════════
    // PRIVATE — Helpers
    // ════════════════════════════════════════════════════════════════

    private PricingConfigResponse toPricingConfigResponse(PricingConfig config) {
        return PricingConfigResponse.builder()
                .id(config.getId())
                .name(config.getName())
                .currency(config.getCurrency().name())
                .basePrice(config.getBasePrice())
                .pricePerKm(config.getPricePerKm())
                .pricePerMin(config.getPricePerMin())
                .minimumPrice(config.getMinimumPrice())
                .surgeMultiplier(config.getSurgeMultiplier())
                .surgeThreshold(config.getSurgeThreshold())
                .platformFeePercent(config.getPlatformFeePercent())
                .freeCancellationMinutes(config.getFreeCancellationMinutes())
                .active(config.isActive())
                .build();
    }

    private SmsConfigResponse toSmsConfigResponse(SmsConfig config) {
        return SmsConfigResponse.builder()
                .id(config.getId())
                .provider(config.getProvider().name())
                .enabled(config.isEnabled())
                .apiKey(config.getApiKey())
                .apiSecret(config.getApiSecret())
                .senderNumber(config.getSenderNumber())
                .senderName(config.getSenderName())
                .build();
    }

    private void notifyPassenger(RideRequest ride, String type, String message) {
        messaging.convertAndSendToUser(
                ride.getPassenger().getId(),
                "/queue/ride-update",
                Map.of(
                        "rideId", ride.getId(),
                        "status", ride.getStatus().name(),
                        "message", message
                ));
    }

    private RideResponse toResponse(RideRequest ride, BigDecimal surgeMultiplier) {
        RideResponse.RideResponseBuilder builder = RideResponse.builder()
                .id(ride.getId())
                .passengerId(ride.getPassenger().getId())
                .passengerFirstName(ride.getPassenger().getFirstName())
                .passengerLastName(ride.getPassenger().getLastName())
                .passengerAvatarUrl(ride.getPassenger().getAvatarUrl())
                .passengerRating(ride.getPassenger().getRating())
                .pickupLat(ride.getPickupLat())
                .pickupLng(ride.getPickupLng())
                .pickupAddress(ride.getPickupAddress())
                .destinationLat(ride.getDestinationLat())
                .destinationLng(ride.getDestinationLng())
                .destinationAddress(ride.getDestinationAddress())
                .estimatedDistanceKm(ride.getEstimatedDistanceKm())
                .estimatedDurationMin(ride.getEstimatedDurationMin())
                .estimatedPrice(ride.getEstimatedPrice())
                .finalPrice(ride.getFinalPrice())
                .currency(ride.getCurrency().name())
                .status(ride.getStatus().name())
                .createdAt(ride.getCreatedAt())
                .driverNotifiedAt(ride.getDriverNotifiedAt())
                .driverRespondedAt(ride.getDriverRespondedAt())
                .pickupAt(ride.getPickupAt())
                .startedAt(ride.getStartedAt())
                .completedAt(ride.getCompletedAt())
                .paymentStatus(ride.getPaymentStatus().name())
                .platformFeeAmount(ride.getPlatformFeeAmount())
                .driverEarnings(ride.getDriverEarnings())
                .notes(ride.getNotes())
                .passengerCount(ride.getPassengerCount())
                .surgeMultiplier(surgeMultiplier);

        if (ride.getCancelledBy() != null) builder.cancelledBy(ride.getCancelledBy().name());
        if (ride.getCancelReason() != null) builder.cancelReason(ride.getCancelReason());

        if (ride.getDriver() != null) {
            builder.driverId(ride.getDriver().getId())
                    .driverFirstName(ride.getDriver().getFirstName())
                    .driverLastName(ride.getDriver().getLastName())
                    .driverAvatarUrl(ride.getDriver().getAvatarUrl())
                    .driverRating(ride.getDriver().getRating())
                    .driverPhone(ride.getDriver().isPhoneVisible() ? ride.getDriver().getPhone() : null);

            userDocumentRepository.findByUserIdAndDeletedAtIsNull(ride.getDriver().getId()).stream()
                    .filter(d -> d.getType() == UserDocument.DocumentType.DRIVER_LICENSE
                            && d.getStatus() == UserDocument.DocumentStatus.APPROVED)
                    .findFirst()
                    .ifPresent(doc -> builder.driverLicenseId(doc.getId()));

            Vehicle vehicle = driverMatchingService.getDriverVehicle(ride.getDriver().getId());
            if (vehicle == null && ride.getDriver().getVehicles() != null) {
                vehicle = ride.getDriver().getVehicles().stream()
                        .filter(Vehicle::isActive)
                        .findFirst()
                        .orElse(null);
            }
            if (vehicle != null) {
                builder.driverVehicleBrand(vehicle.getBrand())
                        .driverVehicleModel(vehicle.getModel())
                        .driverVehicleColor(vehicle.getColor())
                        .driverVehiclePlate(vehicle.getLicensePlate());
            }
        }

        return builder.build();
    }

    private DriverAvailabilityResponse toAvailabilityResponse(DriverAvailability avail) {
        return DriverAvailabilityResponse.builder()
                .id(avail.getId())
                .userId(avail.getUser().getId())
                .firstName(avail.getUser().getFirstName())
                .lastName(avail.getUser().getLastName())
                .avatarUrl(avail.getUser().getAvatarUrl())
                .rating(avail.getUser().getRating())
                .available(avail.isAvailable())
                .status(avail.getStatus().name())
                .currentLat(avail.getCurrentLat())
                .currentLng(avail.getCurrentLng())
                .currentHeading(avail.getCurrentHeading())
                .maxDistanceKm(avail.getMaxDistanceKm())
                .autoAccept(avail.isAutoAccept())
                .lastLocationUpdate(avail.getLastLocationUpdate())
                .build();
    }

    private NearbyDriverResponse toNearbyDriverResponse(DriverAvailability avail, double userLat, double userLng) {
        Vehicle vehicle = driverMatchingService.getDriverVehicle(avail.getUser().getId());
        double dist = driverMatchingService.calculateDistanceKm(
                userLat, userLng,
                avail.getCurrentLat().doubleValue(), avail.getCurrentLng().doubleValue());
        return NearbyDriverResponse.builder()
                .userId(avail.getUser().getId())
                .firstName(avail.getUser().getFirstName())
                .lastName(avail.getUser().getLastName())
                .avatarUrl(avail.getUser().getAvatarUrl())
                .rating(avail.getUser().getRating())
                .reviewCount(avail.getUser().getReviewCount())
                .distanceKm(BigDecimal.valueOf(dist).setScale(1, java.math.RoundingMode.HALF_UP))
                .currentLat(avail.getCurrentLat())
                .currentLng(avail.getCurrentLng())
                .vehicleBrand(vehicle != null ? vehicle.getBrand() : null)
                .vehicleModel(vehicle != null ? vehicle.getModel() : null)
                .vehicleColor(vehicle != null ? vehicle.getColor() : null)
                .build();
    }

    // ════════════════════════════════════════════════════════════════
    // SOS — Alerte d'urgence
    // ════════════════════════════════════════════════════════════════

    @Transactional
    public void triggerSosAlert(String rideId, String userId, Double currentLat, Double currentLng) {
        RideRequest ride = rideRequestRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Course non trouvée: " + rideId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé: " + userId));

        String driverId = ride.getDriver() != null ? ride.getDriver().getId() : null;

        // Use real-time GPS from frontend, fallback to pickup coords
        double sosLat = (currentLat != null) ? currentLat : (ride.getPickupLat() != null ? ride.getPickupLat().doubleValue() : 0.0);
        double sosLng = (currentLng != null) ? currentLng : (ride.getPickupLng() != null ? ride.getPickupLng().doubleValue() : 0.0);

        // Build SOS alert data
        java.util.Map<String, Object> sosData = new java.util.HashMap<>();
        sosData.put("type", "SOS_ALERT");
        sosData.put("rideId", rideId);
        sosData.put("userId", userId);
        sosData.put("userName", user.getFirstName() + " " + user.getLastName());
        sosData.put("userPhone", user.getPhone());
        sosData.put("currentLat", sosLat);
        sosData.put("currentLng", sosLng);
        sosData.put("pickupLat", ride.getPickupLat());
        sosData.put("pickupLng", ride.getPickupLng());
        sosData.put("destinationLat", ride.getDestinationLat());
        sosData.put("destinationLng", ride.getDestinationLng());
        sosData.put("triggeredBy", driverId != null && driverId.equals(userId) ? "DRIVER" : "PASSENGER");
        sosData.put("timestamp", LocalDateTime.now().toString());

        // Find all admin user IDs, exclude the driver
        List<String> adminUserIds = adminRoleRepository.findAllUserIds();
        List<String> targetAdminIds = adminUserIds.stream()
                .filter(id -> driverId == null || !id.equals(driverId))
                .toList();

        // Send WS alert to each admin individually (not broadcast)
        for (String adminId : targetAdminIds) {
            messaging.convertAndSendToUser(adminId, "/queue/admin/sos", sosData);
        }

        // Also keep the topic for admin layout badge
        messaging.convertAndSend("/topic/admin/sos", sosData);

        // In-app notification to each admin (not the driver)
        String notifBody = "Alerte SOS declenchee par " + user.getFirstName() + " " + user.getLastName()
                + " — Position GPS: " + sosLat + ", " + sosLng
                + " (Course: " + rideId + ")";
        for (String adminId : targetAdminIds) {
            try {
                notificationPort.notifyWithLink(
                        adminId, "SOS", "SOS URGENT", notifBody,
                        "/ride/tracking/" + rideId);
            } catch (Exception e) {
                log.error("Failed to send SOS notification to admin {}: {}", adminId, e.getMessage());
            }
        }

        // SMS aux contacts d'urgence
        List<EmergencyContact> contacts = emergencyContactRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId);
        if (!contacts.isEmpty() && smsPort.isAvailable()) {
            String smsBody = "ALERTE SOS — " + user.getFirstName() + " " + user.getLastName()
                    + " a declenche une alerte d'urgence."
                    + " Position actuelle: " + sosLat + "," + sosLng
                    + ". Course: " + rideId;
            for (EmergencyContact contact : contacts) {
                try {
                    smsPort.sendSms(contact.getPhone(), smsBody);
                    log.info("SOS SMS sent to emergency contact {} ({})", contact.getName(), contact.getPhone());
                } catch (Exception e) {
                    log.error("Failed to send SOS SMS to {}: {}", contact.getPhone(), e.getMessage());
                }
            }
        }

        log.warn("SOS ALERT — ride={}, user={}, phone={}, currentPosition=({},{}), admins notified={}",
                rideId, userId, user.getPhone(),
                sosLat, sosLng, targetAdminIds.size());
    }

    // ════════════════════════════════════════════════════════════════
    // CHAT — Messagerie in-ride
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> getRideMessages(String rideId, String userId) {
        RideRequest ride = rideRequestRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Course non trouvée: " + rideId));

        boolean isPassenger = ride.getPassenger() != null && ride.getPassenger().getId().equals(userId);
        boolean isDriver = ride.getDriver() != null && ride.getDriver().getId().equals(userId);
        if (!isPassenger && !isDriver) {
            throw new NotAuthorizedException("Vous n'êtes pas participant de cette course");
        }

        return rideMessages.getOrDefault(rideId, new CopyOnWriteArrayList<>());
    }

    @Transactional
    public java.util.Map<String, Object> sendRideMessage(String rideId, String content, String userId) {
        RideRequest ride = rideRequestRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Course non trouvée: " + rideId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé: " + userId));

        boolean isPassenger = ride.getPassenger() != null && ride.getPassenger().getId().equals(userId);
        boolean isDriver = ride.getDriver() != null && ride.getDriver().getId().equals(userId);
        if (!isPassenger && !isDriver) {
            throw new NotAuthorizedException("Vous n'êtes pas participant de cette course");
        }

        java.util.Map<String, Object> message = new java.util.HashMap<>();
        message.put("id", java.util.UUID.randomUUID().toString());
        message.put("rideId", rideId);
        message.put("senderId", userId);
        message.put("senderName", user.getFirstName() + " " + user.getLastName());
        message.put("senderRole", isPassenger ? "PASSENGER" : "DRIVER");
        message.put("content", content);
        message.put("timestamp", LocalDateTime.now().toString());

        // Store in memory
        rideMessages.computeIfAbsent(rideId, k -> new CopyOnWriteArrayList<>()).add(message);

        // Send via WebSocket to the other party
        String recipientId = isPassenger
                ? (ride.getDriver() != null ? ride.getDriver().getId() : null)
                : ride.getPassenger().getId();

        if (recipientId != null) {
            messaging.convertAndSendToUser(recipientId, "/queue/ride-chat", message);

            // Notification dans la cloche
            String senderLabel = isPassenger ? "Passager" : "Chauffeur";
            notificationPort.notifyWithLink(
                    recipientId, "MESSAGE",
                    senderLabel + " : " + user.getFirstName() + " " + user.getLastName(),
                    content,
                    "/ride/tracking/" + rideId
            );
        }

        // Broadcast to ride topic for real-time UI
        messaging.convertAndSend("/topic/ride/" + rideId + "/chat", message);

        return message;
    }

    // ════════════════════════════════════════════════════════════════
    // EARNINGS — Statistiques chauffeur
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getDriverEarnings(String driverId) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new UserNotFoundException("Chauffeur non trouvé: " + driverId));

        List<RideRequest> completedRides = rideRequestRepository
                .findByDriverAndStatus(driver, RideRequest.RideStatus.COMPLETED);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime weekStart = now.toLocalDate().minusDays(now.getDayOfWeek().getValue() - 1).atStartOfDay();
        LocalDateTime monthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay();

        java.math.BigDecimal todayEarnings = completedRides.stream()
                .filter(r -> r.getCompletedAt() != null && r.getCompletedAt().isAfter(todayStart))
                .map(r -> r.getDriverEarnings() != null ? r.getDriverEarnings() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal weekEarnings = completedRides.stream()
                .filter(r -> r.getCompletedAt() != null && r.getCompletedAt().isAfter(weekStart))
                .map(r -> r.getDriverEarnings() != null ? r.getDriverEarnings() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal monthEarnings = completedRides.stream()
                .filter(r -> r.getCompletedAt() != null && r.getCompletedAt().isAfter(monthStart))
                .map(r -> r.getDriverEarnings() != null ? r.getDriverEarnings() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        long todayTrips = completedRides.stream()
                .filter(r -> r.getCompletedAt() != null && r.getCompletedAt().isAfter(todayStart))
                .count();

        long weekTrips = completedRides.stream()
                .filter(r -> r.getCompletedAt() != null && r.getCompletedAt().isAfter(weekStart))
                .count();

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("todayEarnings", todayEarnings);
        result.put("weekEarnings", weekEarnings);
        result.put("monthEarnings", monthEarnings);
        result.put("todayTrips", todayTrips);
        result.put("weekTrips", weekTrips);
        result.put("totalTrips", completedRides.size());
        result.put("currency", "FBU");

        return result;
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> getDriverEarningsWeekly(String driverId) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new UserNotFoundException("Chauffeur non trouvé: " + driverId));

        List<RideRequest> completedRides = rideRequestRepository
                .findByDriverAndStatus(driver, RideRequest.RideStatus.COMPLETED);

        LocalDateTime now = LocalDateTime.now();
        List<java.util.Map<String, Object>> weeklyData = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDateTime day = now.toLocalDate().minusDays(i).atStartOfDay();
            LocalDateTime dayEnd = day.plusDays(1);

            long trips = completedRides.stream()
                    .filter(r -> r.getCompletedAt() != null
                            && r.getCompletedAt().isAfter(day)
                            && r.getCompletedAt().isBefore(dayEnd))
                    .count();

            java.math.BigDecimal earnings = completedRides.stream()
                    .filter(r -> r.getCompletedAt() != null
                            && r.getCompletedAt().isAfter(day)
                            && r.getCompletedAt().isBefore(dayEnd))
                    .map(r -> r.getDriverEarnings() != null ? r.getDriverEarnings() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            java.util.Map<String, Object> dayData = new java.util.HashMap<>();
            dayData.put("date", day.toLocalDate().toString());
            dayData.put("dayName", day.getDayOfWeek().toString());
            dayData.put("trips", trips);
            dayData.put("earnings", earnings);
            weeklyData.add(dayData);
        }

        return weeklyData;
    }

    // ════════════════════════════════════════════════════════════════
    // PROMO — Codes promo
    // ════════════════════════════════════════════════════════════════

    @Transactional
    public RideResponse applyPromoCode(String rideId, String code, String userId) {
        RideRequest ride = rideRequestRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Course non trouvée: " + rideId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé: " + userId));

        if (ride.getPassenger() == null || !ride.getPassenger().getId().equals(userId)) {
            throw new NotAuthorizedException("Seul le passager peut appliquer un code promo");
        }

        if (ride.getStatus() != RideRequest.RideStatus.SEARCHING) {
            throw new InvalidRideStateException("Un code promo ne peut être appliqué qu'en cours de recherche");
        }

        if (code == null || code.trim().isEmpty()) {
            throw new InvalidOperationException("Code promo invalide");
        }

        java.math.BigDecimal discount = ride.getEstimatedPrice()
                .multiply(new java.math.BigDecimal("0.10"))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        java.math.BigDecimal newPrice = ride.getEstimatedPrice().subtract(discount);
        ride.setEstimatedPrice(newPrice);

        rideRequestRepository.save(ride);

        log.info("Promo code '{}' applied to ride {} — discount: {} {}", code, rideId, discount, ride.getCurrency());
        return toResponse(ride, java.math.BigDecimal.ONE);
    }

    // ════════════════════════════════════════════════════════════════
    // CHAUFFEUR — Rendre (transférer) la course au chauffeur le plus proche
    // ════════════════════════════════════════════════════════════════

    public RideResponse transferRide(String rideId, String driverId) {
        RideRequest ride = getAndValidateDriverRide(rideId, driverId);

        List<RideRequest.RideStatus> transferable = List.of(
                RideRequest.RideStatus.ACCEPTED,
                RideRequest.RideStatus.DRIVER_EN_ROUTE,
                RideRequest.RideStatus.ARRIVED);
        if (!transferable.contains(ride.getStatus())) {
            throw new InvalidRideStateException("Impossible de transférer cette course depuis le statut " + ride.getStatus());
        }

        rideRequestDomainService.releaseDriver(driverId);
        resetConsecutiveRefusals(driverId);

        // Pas de cooldown si le passager a déjà refusé de payer
        long passengerRefusals2 = paymentRefusalRepository.countByUserId(ride.getPassenger().getId());
        if (passengerRefusals2 == 0) {
            applyCooldownPenalty(driverId);
        } else {
            log.info("Driver {} transfer exempted — passenger {} has {} payment refusal(s)",
                    driverId, ride.getPassenger().getId(), passengerRefusals2);
        }

        ride.setDriver(null);
        ride.render();
        ride.setSearchStartedAt(LocalDateTime.now());
        ride.setSearchTimeoutAt(LocalDateTime.now().plusMinutes(3));
        rideRequestRepository.save(ride);

        // Notifier l'ancien chauffeur
        messaging.convertAndSendToUser(driverId, "/queue/ride-update", Map.of(
                "rideId", rideId,
                "status", "RENDERED",
                "message", "Course rendue — recherche d'un autre chauffeur..."));

        // Notifier tous les chauffeurs proches du statut RENDERED
        messaging.convertAndSend("/topic/ride/" + rideId + "/status", Map.of(
                "rideId", rideId,
                "status", "RENDERED",
                "message", "Course rendue par le chauffeur"));

        // Chercher le chauffeur le plus proche
        List<DriverAvailability> nearby = driverMatchingService.findNearbyDrivers(
                ride.getPickupLat().doubleValue(), ride.getPickupLng().doubleValue(), getRebroadcastRadiusKm());

        DriverAvailability nearestDriver = null;
        for (DriverAvailability da : nearby) {
            if (!da.getUser().getId().equals(driverId) && !da.getUser().getId().equals(ride.getPassenger().getId())) {
                nearestDriver = da;
                break;
            }
        }

        if (nearestDriver == null) {
            ride.render();
            rideRequestRepository.save(ride);

            messaging.convertAndSendToUser(
                    ride.getPassenger().getId(),
                    "/queue/ride-update",
                    Map.of(
                            "rideId", rideId,
                            "status", "RENDERED",
                            "message", "Aucun autre chauffeur disponible. Votre course a été rendue."));

            notificationPort.notify(
                    ride.getPassenger().getId(),
                    "RENDERED",
                    "Course rendue",
                    "Aucun autre chauffeur n'a été trouvé pour reprendre votre course.");

            auditPort.log("RIDE_RENDERED", "RideRequest", rideId,
                    null, "Aucun autre chauffeur disponible pour transfert", driverId);
            return toResponse(ride, null);
        }

        User newDriverUser = nearestDriver.getUser();
        ride.setDriver(newDriverUser);
        ride.setStatus(RideRequest.RideStatus.DRIVER_FOUND);
        ride.setDriverNotifiedAt(LocalDateTime.now());
        rideRequestRepository.save(ride);

        messaging.convertAndSendToUser(
                newDriverUser.getId(),
                "/queue/ride-request",
                buildRideRequestMessage(ride, ride.getPassenger(),
                        ride.getPickupAddress(), ride.getDestinationAddress(),
                        ride.getEstimatedPrice(),
                        ride.getEstimatedDistanceKm(),
                        ride.getEstimatedDurationMin() != null ? ride.getEstimatedDurationMin() : 0,
                        ride.getCurrency().name(), ride.getPassengerCount()));

        messaging.convertAndSendToUser(
                ride.getPassenger().getId(),
                "/queue/ride-update",
                Map.of(
                        "rideId", rideId,
                        "status", "DRIVER_FOUND",
                        "message", "Un nouveau chauffeur a été trouvé !"));

        notificationPort.notifyWithLink(
                newDriverUser.getId(), "BOOKING",
                "Course transférée",
                "Une course vous a été assignée par un autre chauffeur",
                "/driver/ride/" + rideId);

        auditPort.log("RIDE_TRANSFERRED", "RideRequest", rideId,
                null, "Course transférée par " + driverId + " vers " + newDriverUser.getId(), driverId);

        return toResponse(ride, null);
    }

    // ════════════════════════════════════════════════════════════════
    // CHAUFFEUR — Statut cooldown
    // ════════════════════════════════════════════════════════════════

    public java.util.Map<String, Object> getDriverCooldownStatus(String driverId) {
        var map = new java.util.LinkedHashMap<String, Object>();
        User driver = userRepository.findByIdAndDeletedAtIsNull(driverId).orElse(null);
        if (driver != null && driver.getBlockedUntil() != null
                && driver.getBlockedUntil().isAfter(LocalDateTime.now())) {
            long remainingSec = java.time.Duration.between(LocalDateTime.now(), driver.getBlockedUntil()).toSeconds();
            map.put("blocked", true);
            map.put("remainingSeconds", remainingSec);
            map.put("blockedUntil", driver.getBlockedUntil().toString());
        } else {
            map.put("blocked", false);
            map.put("remainingSeconds", 0);
        }
        map.put("cooldownMinutes", getDriverCooldownMinutes());
        map.put("consecutiveRefusals", driver != null ? driver.getConsecutiveRefusals() : 0);
        int refCount = driver != null ? driver.getConsecutiveRefusals() : 0;
        map.put("currentPenaltyMinutes", refCount > 1 ? getRefusalPenaltyForCount(refCount - 1) : 0);
        map.put("nextRefusalPenalty", getRefusalPenaltyForCount(refCount));
        return map;
    }

    // ════════════════════════════════════════════════════════════════
    // AUTO-CANCEL — Courses expirées (timeout configurable via admin)
    // ════════════════════════════════════════════════════════════════

    private int getSearchTimeoutMinutes() {
        return systemSettingRepository.findByKey("ride.search_timeout_minutes")
                .map(s -> {
                    try { return Integer.parseInt(s.getValue()); }
                    catch (NumberFormatException e) { return 3; }
                })
                .orElse(3);
    }

    private int getRebroadcastRadiusKm() {
        return systemSettingRepository.findByKey("ride.rebroadcast_radius_km")
                .map(s -> {
                    try { return Integer.parseInt(s.getValue()); }
                    catch (NumberFormatException e) { return 2; }
                })
                .orElse(2);
    }

    private int getDriverCooldownMinutes() {
        return systemSettingRepository.findByKey("ride.driver_cooldown_minutes")
                .map(s -> {
                    try { return Integer.parseInt(s.getValue()); }
                    catch (NumberFormatException e) { return 15; }
                })
                .orElse(15);
    }

    private void applyCooldownPenalty(String driverId) {
        int minutes = getDriverCooldownMinutes();
        if (minutes <= 0) return;
        userRepository.findByIdAndDeletedAtIsNull(driverId).ifPresent(driver -> {
            driver.setBlockedUntil(LocalDateTime.now().plusMinutes(minutes));
            userRepository.save(driver);
        });
    }

    // ════════════════════════════════════════════════════════════════
    // PÉNALITÉ PROGRESSIVE — Refus successifs
    // ════════════════════════════════════════════════════════════════

    private void applyProgressiveRefusalPenalty(String driverId) {
        userRepository.findByIdAndDeletedAtIsNull(driverId).ifPresent(driver -> {
            int count = driver.getConsecutiveRefusals() + 1;
            driver.setConsecutiveRefusals(count);

            if (count <= 1) {
                // 1er refus pur = toléré, pas de blocage
                log.info("Driver {} 1st refusal — tolerated, no block", driverId);
            } else {
                // 2e refus consécutif et plus = pénalité progressive
                int minutes = getRefusalPenaltyForCount(count - 1);
                driver.setBlockedUntil(LocalDateTime.now().plusMinutes(minutes));
                log.info("Driver {} refusal #{} — blocked for {} min", driverId, count, minutes);
            }

            userRepository.save(driver);
        });
    }

    public void resetConsecutiveRefusals(String driverId) {
        userRepository.findByIdAndDeletedAtIsNull(driverId).ifPresent(driver -> {
            if (driver.getConsecutiveRefusals() > 0) {
                driver.setConsecutiveRefusals(0);
                userRepository.save(driver);
                log.info("Driver {} refusals reset to 0 (ride accepted)", driverId);
            }
        });
    }

    private int getRefusalPenaltyForCount(int count) {
        String key = switch (count) {
            case 1 -> "ride.refusal_penalty_t1";
            case 2 -> "ride.refusal_penalty_t2";
            case 3 -> "ride.refusal_penalty_t3";
            case 4 -> "ride.refusal_penalty_t4";
            case 5 -> "ride.refusal_penalty_t5";
            default -> "ride.refusal_penalty_t6plus";
        };
        int defaultVal = switch (count) {
            case 1 -> 5;
            case 2 -> 10;
            case 3 -> 60;
            case 4 -> 120;
            case 5 -> 180;
            default -> 240;
        };
        return systemSettingRepository.findByKey(key)
                .map(s -> { try { return Integer.parseInt(s.getValue()); } catch (Exception e) { return defaultVal; } })
                .orElse(defaultVal);
    }

    public int getConsecutiveRefusals(String driverId) {
        return userRepository.findByIdAndDeletedAtIsNull(driverId)
                .map(User::getConsecutiveRefusals)
                .orElse(0);
    }

    public java.util.Map<String, Object> getRefusalPenaltyConfig() {
        var map = new java.util.LinkedHashMap<String, Object>();
        map.put("t1", getRefusalPenaltyForCount(1));
        map.put("t2", getRefusalPenaltyForCount(2));
        map.put("t3", getRefusalPenaltyForCount(3));
        map.put("t4", getRefusalPenaltyForCount(4));
        map.put("t5", getRefusalPenaltyForCount(5));
        map.put("t6plus", getRefusalPenaltyForCount(6));
        return map;
    }

    public Map<String, Object> getSearchTimeoutConfig() {
        int timeoutMinutes = getSearchTimeoutMinutes();
        int radiusKm = getRebroadcastRadiusKm();
        double notificationVolume = getNotificationVolume();
        var map = new java.util.LinkedHashMap<String, Object>();
        map.put("timeoutMinutes", timeoutMinutes);
        map.put("timeoutSeconds", timeoutMinutes * 60);
        map.put("rebroadcastRadiusKm", radiusKm);
        map.put("notificationVolume", notificationVolume);
        map.put("defaultRideRequestSound", getSettingString("ride.default_ride_request_sound", "classic"));
        map.put("defaultRideAcceptedSound", getSettingString("ride.default_ride_accepted_sound", "success"));
        map.put("defaultRideCancelledSound", getSettingString("ride.default_ride_cancelled_sound", "alert"));
        map.put("defaultRideCompletedSound", getSettingString("ride.default_ride_completed_sound", "tada"));
        map.put("defaultMessageSound", getSettingString("ride.default_message_sound", "ping"));
        map.put("defaultSosSound", getSettingString("ride.default_sos_sound", "siren"));
        map.put("userSoundConfigEnabled", getSettingString("ride.user_sound_config_enabled", "true"));
        map.put("refusalPenalty", getRefusalPenaltyConfig());
        return map;
    }

    private double getNotificationVolume() {
        return systemSettingRepository.findByKey("ride.notification_volume")
                .map(s -> {
                    try {
                        double v = Double.parseDouble(s.getValue());
                        return Math.max(0.0, Math.min(1.0, v));
                    } catch (NumberFormatException e) { return 0.3; }
                })
                .orElse(0.3);
    }

    private String getSettingString(String key, String defaultVal) {
        return systemSettingRepository.findByKey(key)
                .map(s -> s.getValue() != null ? s.getValue() : defaultVal)
                .orElse(defaultVal);
    }

    @Transactional
    public int autoCancelExpiredSearchingRides() {
        int timeout = getSearchTimeoutMinutes();
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeout);
        List<RideRequest> expired = rideRequestRepository.findExpiredSearches(cutoff);

        for (RideRequest ride : expired) {
            String reason = "Aucun chauffeur trouvé après " + timeout + " minute(s) de recherche";

            ride.cancelByPassenger(reason);
            rideRequestRepository.save(ride);

            messaging.convertAndSendToUser(
                    ride.getPassenger().getId(),
                    "/queue/ride-update",
                    Map.of(
                            "rideId", ride.getId(),
                            "status", "EXPIRED",
                            "message", "Aucun chauffeur n'a été trouvé. La demande a été annulée."));

            notificationPort.notify(
                    ride.getPassenger().getId(),
                    "CANCELLATION",
                    "Recherche expirée",
                    "Aucun chauffeur disponible dans votre zone.");

            auditPort.log("RIDE_AUTO_CANCELLED", "RideRequest", ride.getId(),
                    null, reason, ride.getPassenger().getId());
        }

        if (!expired.isEmpty()) {
            log.info("Auto-cancelled {} expired SEARCHING rides (timeout={}min)", expired.size(), timeout);
        }
        return expired.size();
    }

    @Transactional
    public int autoCancelExpiredDriverFoundRides() {
        int timeout = getSearchTimeoutMinutes();
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeout);
        List<RideRequest> expired = rideRequestRepository.findExpiredDriverFound(cutoff);

        int rebroadcastCount = 0;
        int cancelledCount = 0;

        for (RideRequest ride : expired) {
            long totalMinutesSinceSearch = java.time.Duration.between(ride.getSearchStartedAt(), LocalDateTime.now()).toMinutes();
            int maxTotalMinutes = timeout * 3;

            if (ride.getDriver() != null) {
                rideRequestDomainService.releaseDriver(ride.getDriver().getId());
            }

            List<DriverAvailability> nearby = driverMatchingService.findNearbyDrivers(
                    ride.getPickupLat().doubleValue(), ride.getPickupLng().doubleValue(), getRebroadcastRadiusKm());

            List<DriverAvailability> candidates = nearby.stream()
                    .filter(d -> d.isAvailable())
                    .filter(d -> !d.getUser().getId().equals(ride.getPassenger().getId()))
                    .toList();

            if (!candidates.isEmpty() && totalMinutesSinceSearch < maxTotalMinutes) {
                DriverAvailability nearest = candidates.get(0);
                ride.setDriver(nearest.getUser());
                ride.setStatus(RideRequest.RideStatus.DRIVER_FOUND);
                ride.setDriverNotifiedAt(LocalDateTime.now());
                rideRequestRepository.save(ride);

            for (DriverAvailability candidate : candidates) {
                messaging.convertAndSendToUser(
                        candidate.getUser().getId(),
                        "/queue/ride-request",
                        buildRideRequestMessage(ride, ride.getPassenger(),
                                ride.getPickupAddress(), ride.getDestinationAddress(),
                                ride.getEstimatedPrice(),
                                ride.getEstimatedDistanceKm(),
                                ride.getEstimatedDurationMin() != null ? ride.getEstimatedDurationMin() : 0,
                                ride.getCurrency().name(), ride.getPassengerCount()));

                    notificationPort.notifyWithLink(
                            candidate.getUser().getId(), "BOOKING",
                            "Nouvelle demande de course",
                            "Un passager cherche un chauffeur à proximité",
                            "/driver/ride/active"
                    );
                }

                messaging.convertAndSendToUser(
                        ride.getPassenger().getId(),
                        "/queue/ride-update",
                        Map.of(
                                "rideId", ride.getId(),
                                "status", "DRIVER_FOUND",
                                "message", "Recherche d'un autre chauffeur..."));

                rebroadcastCount++;
                log.info("Ride {} rebroadcast to {} drivers after timeout (attempt {}/3)",
                        ride.getId(), candidates.size(), totalMinutesSinceSearch / timeout + 1);
            } else {
                String reason = "Temps d'attente dépassé — aucun chauffeur n'a répondu";

                ride.cancelByPassenger(reason);
                rideRequestRepository.save(ride);

                messaging.convertAndSend(
                        "/topic/ride/" + ride.getId() + "/status",
                        Map.of(
                                "rideId", ride.getId(),
                                "status", "EXPIRED",
                                "message", "Aucun chauffeur n'a accepté votre course. La demande a été annulée."
                        ));

                messaging.convertAndSendToUser(
                        ride.getPassenger().getId(),
                        "/queue/ride-update",
                        Map.of(
                                "rideId", ride.getId(),
                                "status", "EXPIRED",
                                "message", "Aucun chauffeur n'a accepté votre course. La demande a été annulée."));

                notificationPort.notify(
                        ride.getPassenger().getId(),
                        "CANCELLATION",
                        "Course expirée",
                        "Aucun chauffeur n'a accepté votre course dans les temps.");

                auditPort.log("RIDE_AUTO_CANCELLED", "RideRequest", ride.getId(),
                        null, reason, ride.getPassenger().getId());

                cancelledCount++;
            }
        }

        if (rebroadcastCount > 0 || cancelledCount > 0) {
            log.info("Expired DRIVER_FOUND rides: {} rebroadcast, {} cancelled (timeout={}min)",
                    rebroadcastCount, cancelledCount, timeout);
        }
        return rebroadcastCount + cancelledCount;
    }

    // ════════════════════════════════════════════════════════════════
    // EARNINGS — Détail mensuel + carburant
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Map<String, Object> getDriverEarningsDetailed(String driverId) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new UserNotFoundException("Chauffeur non trouvé: " + driverId));

        List<RideRequest> completedRides = rideRequestRepository
                .findByDriverAndStatus(driver, RideRequest.RideStatus.COMPLETED);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        // ── Tous les temps ─────────────────────────────────────
        BigDecimal totalGross = completedRides.stream()
                .map(r -> r.getFinalPrice() != null ? r.getFinalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNet = completedRides.stream()
                .map(r -> r.getDriverEarnings() != null ? r.getDriverEarnings() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFees = completedRides.stream()
                .map(r -> r.getPlatformFeeAmount() != null ? r.getPlatformFeeAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalKm = completedRides.stream()
                .map(r -> r.getEstimatedDistanceKm() != null ? r.getEstimatedDistanceKm() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalTrips = completedRides.size();

        // Fuel total (all time)
        BigDecimal totalFuelLiters = fuelEntryRepository.sumAllLitersByDriver(driver);
        BigDecimal totalFuelCost = fuelEntryRepository.sumAllCostByDriver(driver);

        // ── Ce mois ────────────────────────────────────────────
        java.time.LocalDate monthStart = now.toLocalDate().withDayOfMonth(1);
        java.time.LocalDate monthEnd = monthStart.plusMonths(1);

        BigDecimal monthGross = completedRides.stream()
                .filter(r -> r.getCompletedAt() != null && r.getCompletedAt().isAfter(monthStart.atStartOfDay()))
                .map(r -> r.getFinalPrice() != null ? r.getFinalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthNet = completedRides.stream()
                .filter(r -> r.getCompletedAt() != null && r.getCompletedAt().isAfter(monthStart.atStartOfDay()))
                .map(r -> r.getDriverEarnings() != null ? r.getDriverEarnings() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthFees = completedRides.stream()
                .filter(r -> r.getCompletedAt() != null && r.getCompletedAt().isAfter(monthStart.atStartOfDay()))
                .map(r -> r.getPlatformFeeAmount() != null ? r.getPlatformFeeAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthKm = completedRides.stream()
                .filter(r -> r.getCompletedAt() != null && r.getCompletedAt().isAfter(monthStart.atStartOfDay()))
                .map(r -> r.getEstimatedDistanceKm() != null ? r.getEstimatedDistanceKm() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long monthTrips = completedRides.stream()
                .filter(r -> r.getCompletedAt() != null && r.getCompletedAt().isAfter(monthStart.atStartOfDay()))
                .count();

        BigDecimal monthFuelLiters = fuelEntryRepository.sumLitersByDriverAndDateRange(driver, monthStart, monthEnd);
        BigDecimal monthFuelCost = fuelEntryRepository.sumCostByDriverAndDateRange(driver, monthStart, monthEnd);

        // ── Breakdown mensuel (12 derniers mois) ────────────────
        List<Map<String, Object>> monthlyBreakdown = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            java.time.LocalDate mStart = now.toLocalDate().withDayOfMonth(1).minusMonths(i);
            java.time.LocalDate mEnd = mStart.plusMonths(1);
            String monthKey = mStart.getYear() + "-" + String.format("%02d", mStart.getMonthValue());

            List<RideRequest> monthRides = completedRides.stream()
                    .filter(r -> r.getCompletedAt() != null
                            && !r.getCompletedAt().isBefore(mStart.atStartOfDay())
                            && r.getCompletedAt().isBefore(mEnd.atStartOfDay()))
                    .toList();

            BigDecimal mGross = monthRides.stream()
                    .map(r -> r.getFinalPrice() != null ? r.getFinalPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal mNet = monthRides.stream()
                    .map(r -> r.getDriverEarnings() != null ? r.getDriverEarnings() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal mFees = monthRides.stream()
                    .map(r -> r.getPlatformFeeAmount() != null ? r.getPlatformFeeAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal mKm = monthRides.stream()
                    .map(r -> r.getEstimatedDistanceKm() != null ? r.getEstimatedDistanceKm() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal mFuelLiters = fuelEntryRepository.sumLitersByDriverAndDateRange(driver, mStart, mEnd);
            BigDecimal mFuelCost = fuelEntryRepository.sumCostByDriverAndDateRange(driver, mStart, mEnd);

            Map<String, Object> monthData = new java.util.LinkedHashMap<>();
            monthData.put("month", monthKey);
            monthData.put("monthLabel", mStart.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.FRANCE));
            monthData.put("year", mStart.getYear());
            monthData.put("trips", monthRides.size());
            monthData.put("grossEarnings", mGross);
            monthData.put("netEarnings", mNet);
            monthData.put("platformFees", mFees);
            monthData.put("distanceKm", mKm);
            monthData.put("fuelLiters", mFuelLiters);
            monthData.put("fuelCost", mFuelCost);
            monthlyBreakdown.add(monthData);
        }

        // Currency from completed rides
        String currency = completedRides.isEmpty() ? "FBU" :
                completedRides.get(0).getCurrency().name();

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("currency", currency);

        // All-time totals
        Map<String, Object> allTime = new java.util.LinkedHashMap<>();
        allTime.put("totalTrips", totalTrips);
        allTime.put("grossEarnings", totalGross);
        allTime.put("netEarnings", totalNet);
        allTime.put("platformFees", totalFees);
        allTime.put("distanceKm", totalKm);
        allTime.put("fuelLiters", totalFuelLiters);
        allTime.put("fuelCost", totalFuelCost);
        result.put("allTime", allTime);

        // Current month
        Map<String, Object> currentMonth = new java.util.LinkedHashMap<>();
        currentMonth.put("monthLabel", now.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.FRANCE));
        currentMonth.put("trips", monthTrips);
        currentMonth.put("grossEarnings", monthGross);
        currentMonth.put("netEarnings", monthNet);
        currentMonth.put("platformFees", monthFees);
        currentMonth.put("distanceKm", monthKm);
        currentMonth.put("fuelLiters", monthFuelLiters);
        currentMonth.put("fuelCost", monthFuelCost);
        result.put("currentMonth", currentMonth);

        result.put("monthlyBreakdown", monthlyBreakdown);

        return result;
    }

    @Transactional
    public FuelEntry addFuelEntry(String driverId, Map<String, Object> request) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new UserNotFoundException("Chauffeur non trouvé: " + driverId));

        FuelEntry entry = FuelEntry.builder()
                .driver(driver)
                .refuelDate(java.time.LocalDate.parse((String) request.get("refuelDate")))
                .liters(new java.math.BigDecimal(request.get("liters").toString()))
                .pricePerLiter(new java.math.BigDecimal(request.get("pricePerLiter").toString()))
                .currency(request.getOrDefault("currency", "FBU").toString())
                .odometerKm(request.get("odometerKm") != null ?
                        new java.math.BigDecimal(request.get("odometerKm").toString()) : null)
                .stationName((String) request.get("stationName"))
                .notes((String) request.get("notes"))
                .build();

        return fuelEntryRepository.save(entry);
    }

    @Transactional
    public FuelEntry updateFuelEntry(String driverId, String entryId, Map<String, Object> request) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new UserNotFoundException("Chauffeur non trouvé: " + driverId));

        FuelEntry entry = fuelEntryRepository.findByIdAndDriver(entryId, driver)
                .orElseThrow(() -> new RuntimeException("Entrée de carburant non trouvée"));

        if (request.containsKey("refuelDate")) {
            entry.setRefuelDate(java.time.LocalDate.parse((String) request.get("refuelDate")));
        }
        if (request.containsKey("liters")) {
            entry.setLiters(new java.math.BigDecimal(request.get("liters").toString()));
        }
        if (request.containsKey("pricePerLiter")) {
            entry.setPricePerLiter(new java.math.BigDecimal(request.get("pricePerLiter").toString()));
        }
        if (request.containsKey("currency")) {
            entry.setCurrency(request.get("currency").toString());
        }
        if (request.containsKey("odometerKm")) {
            entry.setOdometerKm(request.get("odometerKm") != null ?
                    new java.math.BigDecimal(request.get("odometerKm").toString()) : null);
        }
        if (request.containsKey("stationName")) {
            entry.setStationName((String) request.get("stationName"));
        }
        if (request.containsKey("notes")) {
            entry.setNotes((String) request.get("notes"));
        }

        return fuelEntryRepository.save(entry);
    }

    @Transactional
    public void deleteFuelEntry(String driverId, String entryId) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new UserNotFoundException("Chauffeur non trouvé: " + driverId));

        FuelEntry entry = fuelEntryRepository.findByIdAndDriver(entryId, driver)
                .orElseThrow(() -> new RuntimeException("Entrée de carburant non trouvée"));

        fuelEntryRepository.delete(entry);
    }

    @Transactional(readOnly = true)
    public java.util.List<FuelEntry> getFuelEntries(String driverId) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new UserNotFoundException("Chauffeur non trouvé: " + driverId));

        return fuelEntryRepository.findByDriverOrderByRefuelDateDesc(driver);
    }

    // ════════════════════════════════════════════════════════════════
    // EARNINGS — Détail journalier (calendrier)
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Map<String, Object> getDriverEarningsDaily(String driverId, int year, int month) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new UserNotFoundException("Chauffeur non trouvé: " + driverId));

        java.time.LocalDate monthStart = java.time.LocalDate.of(year, month, 1);
        java.time.LocalDate monthEnd = monthStart.plusMonths(1);

        List<RideRequest> completedRides = rideRequestRepository
                .findByDriverAndStatus(driver, RideRequest.RideStatus.COMPLETED);

        // Filter rides for this month
        List<RideRequest> monthRides = completedRides.stream()
                .filter(r -> r.getCompletedAt() != null
                        && !r.getCompletedAt().isBefore(monthStart.atStartOfDay())
                        && r.getCompletedAt().isBefore(monthEnd.atStartOfDay()))
                .toList();

        // Fuel entries for this month
        List<FuelEntry> monthFuel = fuelEntryRepository.findByDriverAndDateRange(driver, monthStart, monthEnd);

        // Build daily map
        Map<String, Map<String, Object>> dailyMap = new java.util.LinkedHashMap<>();

        // Initialize all days of the month
        for (int d = 1; d <= monthStart.lengthOfMonth(); d++) {
            String dayKey = String.format("%04d-%02d-%02d", year, month, d);
            Map<String, Object> dayData = new java.util.LinkedHashMap<>();
            dayData.put("date", dayKey);
            dayData.put("dayOfMonth", d);
            dayData.put("grossEarnings", BigDecimal.ZERO);
            dayData.put("netEarnings", BigDecimal.ZERO);
            dayData.put("platformFees", BigDecimal.ZERO);
            dayData.put("distanceKm", BigDecimal.ZERO);
            dayData.put("trips", 0);
            dayData.put("fuelLiters", BigDecimal.ZERO);
            dayData.put("fuelCost", BigDecimal.ZERO);
            dayData.put("rides", new ArrayList<>());
            dailyMap.put(dayKey, dayData);
        }

        // Populate rides per day
        for (RideRequest ride : monthRides) {
            String dayKey = ride.getCompletedAt().toLocalDate().toString();
            Map<String, Object> day = dailyMap.get(dayKey);
            if (day == null) continue;

            day.put("grossEarnings", ((BigDecimal) day.get("grossEarnings"))
                    .add(ride.getFinalPrice() != null ? ride.getFinalPrice() : BigDecimal.ZERO));
            day.put("netEarnings", ((BigDecimal) day.get("netEarnings"))
                    .add(ride.getDriverEarnings() != null ? ride.getDriverEarnings() : BigDecimal.ZERO));
            day.put("platformFees", ((BigDecimal) day.get("platformFees"))
                    .add(ride.getPlatformFeeAmount() != null ? ride.getPlatformFeeAmount() : BigDecimal.ZERO));
            day.put("distanceKm", ((BigDecimal) day.get("distanceKm"))
                    .add(ride.getEstimatedDistanceKm() != null ? ride.getEstimatedDistanceKm() : BigDecimal.ZERO));
            day.put("trips", (int) day.get("trips") + 1);

            // Ride summary for the list
            Map<String, Object> rideSummary = new java.util.LinkedHashMap<>();
            rideSummary.put("id", ride.getId());
            rideSummary.put("pickupAddress", ride.getPickupAddress());
            rideSummary.put("destinationAddress", ride.getDestinationAddress());
            rideSummary.put("finalPrice", ride.getFinalPrice());
            rideSummary.put("driverEarnings", ride.getDriverEarnings());
            rideSummary.put("estimatedDistanceKm", ride.getEstimatedDistanceKm());
            rideSummary.put("estimatedDurationMin", ride.getEstimatedDurationMin());
            rideSummary.put("currency", ride.getCurrency().name());
            rideSummary.put("completedAt", ride.getCompletedAt() != null ? ride.getCompletedAt().toString() : null);
            @SuppressWarnings("unchecked")
            ArrayList<Map<String, Object>> ridesList = (ArrayList<Map<String, Object>>) day.get("rides");
            ridesList.add(rideSummary);
        }

        // Populate fuel per day
        for (FuelEntry fuel : monthFuel) {
            String dayKey = fuel.getRefuelDate().toString();
            Map<String, Object> day = dailyMap.get(dayKey);
            if (day == null) continue;
            day.put("fuelLiters", ((BigDecimal) day.get("fuelLiters")).add(fuel.getLiters()));
            day.put("fuelCost", ((BigDecimal) day.get("fuelCost"))
                    .add(fuel.getTotalCost() != null ? fuel.getTotalCost() : BigDecimal.ZERO));
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("year", year);
        result.put("month", month);
        result.put("days", new ArrayList<>(dailyMap.values()));
        return result;
    }

    private Map<String, Object> buildRideRequestMessage(RideRequest ride, User passenger,
            String pickupAddress, String destinationAddress,
            BigDecimal estimatedPrice, BigDecimal distance, int duration, String currency,
            int passengerCount) {
        long paymentRefusals = paymentRefusalRepository.countByUserId(passenger.getId());
        var msg = new java.util.LinkedHashMap<String, Object>();
        msg.put("rideId", ride.getId());
        msg.put("passengerName", passenger.getFullName());
        msg.put("pickupAddress", pickupAddress != null ? pickupAddress : "Position actuelle");
        msg.put("destinationAddress", destinationAddress != null ? destinationAddress : "Destination");
        msg.put("estimatedPrice", estimatedPrice);
        msg.put("distance", distance != null ? distance.doubleValue() : 0);
        msg.put("duration", duration);
        msg.put("currency", currency);
        msg.put("passengerCount", passengerCount);
        msg.put("passengerPaymentRefusals", paymentRefusals);
        return msg;
    }
}
