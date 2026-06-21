package com.shareway.application.usecase;

import com.shareway.application.dto.request.CreateReviewRequest;
import com.shareway.application.dto.response.PageResponse;
import com.shareway.application.dto.response.ReviewAuthorResponse;
import com.shareway.application.dto.response.ReviewResponse;
import com.shareway.application.port.out.AuditPort;
import com.shareway.application.port.out.NotificationPort;
import com.shareway.infrastructure.adapter.audit.domain.exception.InvalidOperationException;
import com.shareway.infrastructure.adapter.audit.domain.exception.NotAuthorizedException;
import com.shareway.infrastructure.adapter.audit.domain.exception.ResourceAlreadyExistsException;
import com.shareway.infrastructure.adapter.audit.domain.exception.ReviewNotFoundException;
import com.shareway.infrastructure.adapter.audit.domain.exception.TripNotFoundException;
import com.shareway.infrastructure.adapter.audit.domain.exception.UserNotFoundException;
import com.shareway.infrastructure.adapter.audit.domain.model.Review;
import com.shareway.infrastructure.adapter.audit.domain.model.Trip;
import com.shareway.infrastructure.adapter.audit.domain.model.User;
import com.shareway.infrastructure.adapter.audit.domain.repository.BookingRepository;
import com.shareway.infrastructure.adapter.audit.domain.repository.ReviewRepository;
import com.shareway.infrastructure.adapter.audit.domain.repository.TripRepository;
import com.shareway.infrastructure.adapter.audit.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewUseCase {

    private final ReviewRepository reviewRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final AuditPort auditPort;
    private final NotificationPort notificationPort;

    // ─────────────────────────────────────────────────────────────────────
    // Créer un avis
    //
    // Qui peut noter qui ?
    //   • Passager → Conducteur  (après un trajet COMPLETED auquel le passager a participé)
    //   • Conducteur → Passager  (après un trajet COMPLETED qu'il a conduit)
    // ─────────────────────────────────────────────────────────────────────
    public ReviewResponse create(CreateReviewRequest req, String authorId) {
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(req.getTripId())
                .orElseThrow(() -> new TripNotFoundException("Voyage non trouvé: " + req.getTripId()));
        User author = userRepository.findByIdAndDeletedAtIsNull(authorId)
                .orElseThrow(() -> new UserNotFoundException("Auteur non trouvé"));
        User target = userRepository.findByIdAndDeletedAtIsNull(req.getTargetUserId())
                .orElseThrow(() -> new UserNotFoundException("Conducteur non trouvé: " + req.getTargetUserId()));

        // ── Règle 1 : le trajet doit être terminé ──────────────────────
        if (trip.getStatus() != Trip.TripStatus.COMPLETED)
            throw new InvalidOperationException(
                    "Vous ne pouvez laisser un avis que pour les voyages terminés (statut actuel: " + trip.getStatus() + ")");

        // ── Règle 2 : pas d'auto-évaluation ────────────────────────────
        if (authorId.equals(req.getTargetUserId()))
            throw new InvalidOperationException("Vous ne pouvez pas vous réviser");

        boolean authorIsDriver = trip.getDriver().getId().equals(authorId);
        boolean authorIsPassenger = bookingRepository
                .existsByTripIdAndPassengerIdAndDeletedAtIsNull(trip.getId(), authorId);

        // ── Règle 3 : l'auteur doit avoir participé au trajet ──────────
        if (!authorIsDriver && !authorIsPassenger)
            throw new NotAuthorizedException(
                    "Vous n'avez pas participé à ce voyage et ne pouvez donc pas laisser d'avis.");

        // ── Règle 4 : la cible doit être l'autre participant ───────────
        boolean targetIsDriver = trip.getDriver().getId().equals(req.getTargetUserId());
        boolean targetIsPassenger = bookingRepository
                .existsByTripIdAndPassengerIdAndDeletedAtIsNull(trip.getId(), req.getTargetUserId());

        if (!targetIsDriver && !targetIsPassenger)
            throw new InvalidOperationException(
                    "L'utilisateur visé n'a pas participé à ce voyage.");

        // ── Règle 5 : cohérence auteur ↔ cible ─────────────────────────
        // passager → conducteur   OU   conducteur → passager
        // passager → passager est interdit, conducteur → conducteur aussi
        if (authorIsPassenger && targetIsPassenger)
            throw new InvalidOperationException("Les passagers ne peuvent pas évaluer les autres passagers");
        if (authorIsDriver && targetIsDriver)
            throw new InvalidOperationException("Un conducteur ne peut pas s'auto-évaluer");

        // ── Règle 6 : un seul avis par paire (trip, author, target) ────
        if (reviewRepository.existsByTripIdAndAuthorIdAndTargetUserId(
                trip.getId(), authorId, req.getTargetUserId()))
            throw new ResourceAlreadyExistsException(
                    "Vous avez déjà évalué cet utilisateur pour ce voyage.");

        // ── Règle 7 : fenêtre de 14 jours après la date de départ ─────
        if (trip.getDepartureTime().isBefore(LocalDateTime.now().minusDays(14)))
            throw new InvalidOperationException(
                    "Le délai pour laisser un avis est expiré (14 jours après le voyage)");

        // ── Déterminer le type d'avis ──────────────────────────────────
        Review.ReviewType type = authorIsPassenger
                ? Review.ReviewType.PASSENGER_TO_DRIVER
                : Review.ReviewType.DRIVER_TO_PASSENGER;

        Review review = Review.builder()
                .trip(trip).author(author).targetUser(target)
                .rating(req.getRating()).comment(req.getComment())
                .type(type)
                .build();

        reviewRepository.save(review);

        // Mettre à jour la note moyenne de la cible
        target.updateRating(req.getRating());
        userRepository.save(target);

        // Notification push
        notificationPort.notifyWithLink(
                target.getId(), "REVIEW",
                author.getFirstName() + " vous a laissé un avis",
                author.getFullName() + " vous a donné " + req.getRating() + " étoile(s)",
                "/profile/" + authorId
        );
        auditPort.log("REVIEW_CREATED", "Review", review.getId(), null, null, authorId);

        return toResponse(review);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Avis reçus par un utilisateur (publics, approuvés)
    // ─────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getForUser(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(
                reviewRepository.findApprovedByTarget(userId, pageable).map(this::toResponse));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Avis laissés PAR un utilisateur (son historique)
    // ─────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ReviewResponse> getByAuthor(String authorId) {
        return reviewRepository.findByAuthorIdAndDeletedAtIsNull(authorId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Vérifier si l'utilisateur peut encore noter pour un trajet
    // ─────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public boolean canReview(String tripId, String authorId, String targetId) {
        return !reviewRepository.existsByTripIdAndAuthorIdAndTargetUserId(tripId, authorId, targetId);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Signaler un avis
    // ─────────────────────────────────────────────────────────────────────
    public void flagReview(String reviewId, String reason, String reporterId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found: " + reviewId));
        review.flag(reason);
        reviewRepository.save(review);
        auditPort.log("REVIEW_FLAGGED", "Review", reviewId, null, reason, reporterId);
    }

    private ReviewResponse toResponse(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .tripId(r.getTrip().getId())
                .author(ReviewAuthorResponse.builder()
                        .id(r.getAuthor().getId())
                        .firstName(r.getAuthor().getFirstName())
                        .lastName(r.getAuthor().getLastName())
                        .avatarUrl(r.getAuthor().getAvatarUrl()).build())
                .targetUserId(r.getTargetUser().getId())
                .rating(r.getRating())
                .comment(r.getComment())
                .type(r.getType().name())
                .flagged(r.isFlagged())
                .approved(r.isApproved())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
