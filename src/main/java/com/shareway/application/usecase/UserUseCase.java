package com.shareway.application.usecase;

import com.shareway.application.dto.request.SaveVehicleRequest;
import com.shareway.application.dto.request.UpdateUserProfileRequest;
import com.shareway.application.dto.response.DashboardStatsResponse;
import com.shareway.application.dto.response.NotificationResponse;
import com.shareway.application.dto.response.TravelPreferencesResponse;
import com.shareway.application.dto.response.UserResponse;
import com.shareway.application.dto.response.VehicleResponse;
import com.shareway.application.port.out.AuditPort;
import com.shareway.application.port.out.StoragePort;
import com.shareway.domain.exception.InvalidOperationException;
import com.shareway.domain.exception.NotAuthorizedException;
import com.shareway.domain.exception.UserNotFoundException;
import com.shareway.domain.model.Booking;
import com.shareway.domain.model.Trip;
import com.shareway.domain.model.User;
import com.shareway.domain.model.UserDocument;
import com.shareway.domain.model.UserTravelPreferences;
import com.shareway.domain.model.Vehicle;
import com.shareway.domain.repository.BookingRepository;
import com.shareway.domain.repository.NotificationRepository;
import com.shareway.domain.repository.ReviewRepository;
import com.shareway.domain.repository.TripRepository;
import com.shareway.domain.repository.UserDocumentRepository;
import com.shareway.domain.repository.UserRepository;
import com.shareway.domain.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Use case couvrant tous les endpoints du UserService Angular :
 * <p>
 * GET    /users/{id}                     → getProfile()
 * GET    /users/me                       → getMe()
 * PUT    /users/me                       → updateProfile()
 * POST   /users/me/avatar               → uploadAvatar()
 * POST   /users/me/identity             → uploadIdentity()
 * PUT    /users/me/vehicle              → saveVehicle()
 * PATCH  /users/me/role                 → switchRole()
 * GET    /users/me/notifications        → getNotifications()
 * PATCH  /users/me/notifications/read   → markNotificationsRead()
 * GET    /users/me/stats                → getDashboardStats()
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserUseCase {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;
    private final UserDocumentRepository documentRepository;
    private final StoragePort storagePort;
    private final AuditPort auditPort;

    // ─────────────────────────────────────────────
    // GET /users/{id}  →  profil public
    // ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public UserResponse getProfile(String userId) {
        User user = findActive(userId);
        return toResponse(user, false);   // false = ne pas exposer les champs privés
    }

    // ─────────────────────────────────────────────
    // GET /users/me  →  mon profil complet
    // ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public UserResponse getMe(String userId) {
        User user = findActive(userId);
        return toResponse(user, true);    // true = champs privés inclus
    }

    // ─────────────────────────────────────────────
    // PUT /users/me  →  mettre à jour le profil
    // ─────────────────────────────────────────────
    public UserResponse updateProfile(UpdateUserProfileRequest req, String userId) {
        User user = findActive(userId);

        if (req.getFirstName() != null) user.setFirstName(req.getFirstName());
        if (req.getLastName() != null) user.setLastName(req.getLastName());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getPhoneVisible() != null) user.setPhoneVisible(req.getPhoneVisible());
        if (req.getBio() != null) user.setBio(req.getBio());
        if (req.getPreferredLang() != null)
            user.setPreferredLang(User.Language.valueOf(req.getPreferredLang()));

        // Mise à jour des préférences de voyage
        if (req.getPreferences() != null) {
            UserTravelPreferences prefs = user.getTravelPreferences();
            if (prefs == null) {
                prefs = UserTravelPreferences.builder().user(user).build();
                user.setTravelPreferences(prefs);
            }
            var p = req.getPreferences();
            if (p.getMusic() != null) prefs.setMusic(p.getMusic());
            if (p.getSmoking() != null) prefs.setSmoking(p.getSmoking());
            if (p.getPets() != null) prefs.setPets(p.getPets());
            if (p.getTalking() != null) prefs.setTalking(p.getTalking());
        }

        userRepository.save(user);
        auditPort.log("USER_PROFILE_UPDATED", "User", userId, null, null, userId);
        return toResponse(user, true);
    }

    // ─────────────────────────────────────────────
    // POST /users/me/avatar  →  uploader son avatar
    // ─────────────────────────────────────────────
    public String uploadAvatar(MultipartFile file, String userId) {
        User user = findActive(userId);
        validateImageFile(file);

        // Suppression de l'ancien avatar
        if (user.getAvatarUrl() != null) {
            try {
                storagePort.delete(user.getAvatarUrl());
            } catch (Exception ignored) {
            }
        }

        String url = storagePort.upload(file, "avatars/" + userId);
        user.setAvatarUrl(url);
        userRepository.save(user);

        auditPort.log("AVATAR_UPLOADED", "User", userId, null, null, userId);
        return url;
    }

    // ─────────────────────────────────────────────
    // POST /users/me/identity  →  uploader pièce d'identité
    // ─────────────────────────────────────────────
    public void uploadIdentity(MultipartFile file, String userId) {
        User user = findActive(userId);
        validateFile(file, 10 * 1024 * 1024L); // max 10 MB

        String url = storagePort.upload(file, "documents/" + userId + "/identity");

        UserDocument doc = UserDocument.builder()
                .user(user)
                .type(UserDocument.DocumentType.ID_CARD)
                .fileUrl(url)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .status(UserDocument.DocumentStatus.PENDING)
                .build();

        documentRepository.save(doc);
        auditPort.log("IDENTITY_UPLOADED", "UserDocument", doc.getId(), null, null, userId);
        log.info("Identity document uploaded for user {} – awaiting review", userId);
    }

    // ─────────────────────────────────────────────
    // PUT /users/me/vehicle  →  créer / mettre à jour le véhicule
    // ─────────────────────────────────────────────
    public VehicleResponse saveVehicle(SaveVehicleRequest req, String userId) {
        User user = findActive(userId);

        if (!user.isDriver())
            throw new InvalidOperationException("Only drivers can register a vehicle");

        // Réutiliser le véhicule actif s'il existe, sinon en créer un nouveau
        Vehicle vehicle = vehicleRepository
                .findFirstByUserIdAndActiveTrue(userId)
                .orElse(Vehicle.builder().user(user).build());

        vehicle.setBrand(req.getBrand());
        vehicle.setModel(req.getModel());
        vehicle.setColor(req.getColor());
        vehicle.setLicensePlate(req.getLicensePlate());
        vehicle.setYear(req.getYear());

        vehicleRepository.save(vehicle);
        auditPort.log("VEHICLE_SAVED", "Vehicle", vehicle.getId(), null, null, userId);

        return VehicleResponse.builder()
                .id(vehicle.getId())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .color(vehicle.getColor())
                .licensePlate(vehicle.getLicensePlate())
                .year(vehicle.getYear())
                .photoUrl(vehicle.getPhotoUrl())
                .build();
    }

    // ─────────────────────────────────────────────
    // PATCH /users/me/role  →  changer de rôle
    // ─────────────────────────────────────────────
    public UserResponse switchRole(String role, String userId) {
        User user = findActive(userId);
        User.UserRole newRole = User.UserRole.valueOf(role);

        // Si le conducteur a des trajets OPEN, on ne peut pas passer en PASSENGER
        if (newRole == User.UserRole.PASSENGER) {
            long openTrips = tripRepository.countOpenByDriver(userId);
            if (openTrips > 0)
                throw new InvalidOperationException(
                        "Cannot switch to PASSENGER while you have " + openTrips + " open trip(s)");
        }

        user.setRole(newRole);
        userRepository.save(user);
        auditPort.log("ROLE_SWITCHED", "User", userId, null, role, userId);

        return toResponse(user, true);
    }

    // ─────────────────────────────────────────────
    // GET /users/me/notifications
    // ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(String userId) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, Pageable.ofSize(50))
                .stream()
                .map(n -> NotificationResponse.builder()
                        .id(n.getId())
                        .type(n.getType().name())
                        .title(n.getTitle())
                        .body(n.getBody())
                        .link(n.getLink())
                        .read(n.isRead())
                        .createdAt(n.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // PATCH /users/me/notifications/read  →  marquer comme lues
    // ─────────────────────────────────────────────
    public void markNotificationsRead(List<String> ids, String userId) {
        if (ids == null || ids.isEmpty()) {
            // Marquer toutes les notifs comme lues
            notificationRepository.markAllAsRead(userId);
            return;
        }
        ids.forEach(id ->
                notificationRepository.findById(id).ifPresent(n -> {
                    if (!n.getUser().getId().equals(userId))
                        throw new NotAuthorizedException("Not your notification");
                    n.markAsRead();
                    notificationRepository.save(n);
                })
        );
    }

    // ─────────────────────────────────────────────
    // GET /users/me/stats  →  dashboard statistiques
    // ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(String userId) {
        User user = findActive(userId);
        boolean isDriver = user.isDriver();

        // ── Trajets ──────────────────────────────
        List<Trip> myTrips = isDriver
                ? tripRepository.findByDriverId(userId)
                : List.of();

        List<Booking> myBookings = bookingRepository.findByPassengerIdAndDeletedAtIsNull(userId);

        long totalTrips = isDriver ? myTrips.size() : myBookings.size();

        long completedCount = isDriver
                ? myTrips.stream().filter(t -> t.getStatus() == Trip.TripStatus.COMPLETED).count()
                : myBookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.COMPLETED).count();

        long cancelledCount = isDriver
                ? myTrips.stream().filter(t -> t.getStatus() == Trip.TripStatus.CANCELLED).count()
                : myBookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.CANCELLED).count();

        long upcomingTrips = isDriver
                ? myTrips.stream().filter(t -> t.getStatus() == Trip.TripStatus.OPEN
                && t.getDepartureTime().isAfter(LocalDateTime.now())).count()
                : myBookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED
                && b.getTrip().getDepartureTime().isAfter(LocalDateTime.now())).count();

        // ── Passagers (conducteur uniquement) ────
        long totalPassengers = isDriver
                ? myTrips.stream()
                .filter(t -> t.getStatus() == Trip.TripStatus.COMPLETED)
                .mapToLong(Trip::getPassengerCount)
                .sum()
                : 0L;

        // ── Taux de complétion ───────────────────
        double completionRate = totalTrips > 0
                ? Math.round(((double) completedCount / totalTrips) * 1000.0) / 10.0
                : 0.0;

        // ── Gains par devise ────────────────────
        Map<String, BigDecimal> earningsByCurrency = new LinkedHashMap<>();
        BigDecimal totalEarnings = BigDecimal.ZERO;

        if (isDriver) {
            // Sommer les bookings CONFIRMED/COMPLETED des trajets du conducteur
            List<Booking> driverBookings = myTrips.stream()
                    .flatMap(t -> t.getBookings().stream())
                    .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED
                            || b.getStatus() == Booking.BookingStatus.COMPLETED)
                    .filter(b -> b.getAmountPaid() != null)
                    .collect(Collectors.toList());

            for (Booking b : driverBookings) {
                String cur = b.getCurrency().name();
                earningsByCurrency.merge(cur, b.getAmountPaid(), BigDecimal::add);
            }

            // Total en FBU (devise principale)
            totalEarnings = earningsByCurrency.getOrDefault("FBU", BigDecimal.ZERO);
        }

        // ── Gains mensuels (12 derniers mois) ───
        List<DashboardStatsResponse.MonthlyEarning> monthlyEarnings = buildMonthlyEarnings(
                isDriver ? myTrips : List.of(), userId
        );

        // ── Note ────────────────────────────────
        double rating = user.getRating() != null ? user.getRating().doubleValue() : 0.0;

        return DashboardStatsResponse.builder()
                .totalTrips(totalTrips)
                .totalPassengers(totalPassengers)
                .totalEarnings(totalEarnings)
                .earningsByCurrency(earningsByCurrency)
                .rating(rating)
                .reviewCount(user.getReviewCount())
                .completionRate(completionRate)
                .upcomingTrips(upcomingTrips)
                .cancelledTrips(cancelledCount)
                .monthlyEarnings(monthlyEarnings)
                .build();
    }

    // ─────────────────────────────────────────────
    // Helpers privés
    // ─────────────────────────────────────────────

    private User findActive(String userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) throw new InvalidOperationException("File is empty");
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/"))
            throw new InvalidOperationException("Only image files are accepted");
        if (file.getSize() > 5 * 1024 * 1024L)
            throw new InvalidOperationException("Image must be under 5 MB");
    }

    private void validateFile(MultipartFile file, long maxBytes) {
        if (file.isEmpty()) throw new InvalidOperationException("File is empty");
        if (file.getSize() > maxBytes)
            throw new InvalidOperationException(
                    "File must be under " + (maxBytes / 1024 / 1024) + " MB");
    }

    /**
     * Construit les gains mensuels sur les 12 derniers mois,
     * initialisés à 0 pour les mois sans activité.
     */
    private List<DashboardStatsResponse.MonthlyEarning> buildMonthlyEarnings(
            List<Trip> trips, String userId) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, BigDecimal> byMonth = new LinkedHashMap<>();

        // Initialiser les 12 derniers mois à 0
        LocalDateTime now = LocalDateTime.now();
        for (int i = 11; i >= 0; i--) {
            byMonth.put(now.minusMonths(i).format(fmt), BigDecimal.ZERO);
        }

        // Agréger les gains par mois
        trips.stream()
                .filter(t -> t.getStatus() == Trip.TripStatus.COMPLETED)
                .flatMap(t -> t.getBookings().stream())
                .filter(b -> (b.getStatus() == Booking.BookingStatus.CONFIRMED
                        || b.getStatus() == Booking.BookingStatus.COMPLETED)
                        && b.getAmountPaid() != null
                        && b.getCurrency().name().equals("FBU"))
                .forEach(b -> {
                    String month = b.getCreatedAt().format(fmt);
                    if (byMonth.containsKey(month))
                        byMonth.merge(month, b.getAmountPaid(), BigDecimal::add);
                });

        return byMonth.entrySet().stream()
                .map(e -> DashboardStatsResponse.MonthlyEarning.builder()
                        .month(e.getKey())
                        .amount(e.getValue().setScale(2, RoundingMode.HALF_UP))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Construit un UserResponse (public ou privé selon le flag).
     */
    private UserResponse toResponse(User u, boolean includePrivateFields) {
        VehicleResponse vehicle = vehicleRepository
                .findFirstByUserIdAndActiveTrue(u.getId())
                .map(v -> VehicleResponse.builder()
                        .id(v.getId()).brand(v.getBrand()).model(v.getModel())
                        .color(v.getColor()).licensePlate(v.getLicensePlate())
                        .year(v.getYear()).photoUrl(v.getPhotoUrl()).build())
                .orElse(null);

        TravelPreferencesResponse prefs = u.getTravelPreferences() != null
                ? TravelPreferencesResponse.builder()
                .music(u.getTravelPreferences().isMusic())
                .smoking(u.getTravelPreferences().isSmoking())
                .pets(u.getTravelPreferences().isPets())
                .talking(u.getTravelPreferences().isTalking()).build()
                : null;

        return UserResponse.builder()
                .id(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .email(includePrivateFields ? u.getEmail() : null)
                .phone(u.isPhoneVisible() || includePrivateFields ? u.getPhone() : null)
                .phoneVisible(u.isPhoneVisible())
                .avatarUrl(u.getAvatarUrl())
                .bio(u.getBio())
                .role(u.getRole().name())
                .preferredLang(u.getPreferredLang().name())
                .emailVerified(u.isEmailVerified())
                .phoneVerified(u.isPhoneVerified())
                .identityVerified(u.isIdentityVerified())
                .twoFaEnabled(includePrivateFields && u.isTwoFaEnabled())
                .active(u.isActive())
                .blocked(u.isBlocked())
                .blockReason(includePrivateFields ? u.getBlockReason() : null)
                .rating(u.getRating())
                .reviewCount(u.getReviewCount())
                .vehicle(vehicle)
                .preferences(prefs)
                .createdAt(u.getCreatedAt())
                .lastLoginAt(includePrivateFields ? u.getLastLoginAt() : null)
                .build();
    }
}
