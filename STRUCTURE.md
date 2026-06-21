# 📁 Structure complète du projet Shareway Backend

```
shareway-backend/
│
├── pom.xml                                          ← Dépendances Maven (Part1)
├── .gitignore
├── .env.example                                     ← Variables d'env à copier
├── README.md
├── STRUCTURE.md                                     ← Ce fichier
│
├── docker/
│   ├── Dockerfile                                   ← Build multi-stage Java 21
│   └── docker-compose.yml                           ← MySQL + phpMyAdmin + Backend
│
├── logs/                                            ← Logs applicatifs (gitignored)
├── uploads/                                         ← Fichiers uploadés (gitignored)
│
└── src/
    ├── main/
    │   ├── resources/
    │   │   ├── application.yml                      ← Config Spring Boot complète (Part1)
    │   │   └── db/migration/
    │   │       ├── V1__init_core_tables.sql          ← users, trips, bookings, reviews, messages
    │   │       ├── V2__notifications_documents.sql   ← notifications, documents, tokens, 2FA
    │   │       ├── V3__premium_features.sql          ← favoris, blacklist, coupons, paiements
    │   │       ├── V4__audit_logs.sql                ← audit, stats, admin, system_settings
    │   │       └── V5__views_indexes.sql             ← vues SQL, index, procédures stockées
    │   │
    │   └── java/com/shareway/
    │       │
    │       ├── SharewayApplication.java              ← @SpringBootApplication (Part1)
    │       │
    │       ├── domain/                              ══ COUCHE DOMAINE (Part2) ══
    │       │   │
    │       │   ├── model/                           ← Entités JPA
    │       │   │   ├── User.java                    ← Utilisateur (vérif, blocage, 2FA, soft delete)
    │       │   │   ├── Vehicle.java                 ← Véhicule du conducteur
    │       │   │   ├── UserTravelPreferences.java   ← Préférences (musique, fumeur, animaux…)
    │       │   │   ├── Trip.java                    ← Trajet (récurrence, GPS, soft delete)
    │       │   │   ├── TripPreferences.java         ← Préférences par trajet
    │       │   │   ├── StopPoint.java               ← Point d'arrêt intermédiaire
    │       │   │   ├── Booking.java                 ← Réservation (Stripe intégré)
    │       │   │   ├── Review.java                  ← Avis bidirectionnel (modération)
    │       │   │   ├── Message.java                 ← Message (signalement, soft delete)
    │       │   │   ├── Notification.java            ← Notification multi-type
    │       │   │   ├── Report.java                  ← Signalement multi-entités
    │       │   │   ├── AuditLog.java                ← Log d'audit complet
    │       │   │   ├── UserFavorite.java            ← Conducteurs favoris (Part5)
    │       │   │   ├── UserBlacklist.java           ← Liste noire (Part5)
    │       │   │   ├── UserDocument.java            ← Permis, carte grise… (Part5)
    │       │   │   ├── Coupon.java                  ← Coupons de réduction (Part5)
    │       │   │   ├── CouponUsage.java             ← Utilisation des coupons (Part5)
    │       │   │   └── Payment.java                 ← Transactions Stripe (Part5)
    │       │   │
    │       │   ├── repository/                      ← Interfaces Spring Data JPA
    │       │   │   ├── UserRepository.java
    │       │   │   ├── TripRepository.java
    │       │   │   ├── BookingRepository.java
    │       │   │   ├── ReviewRepository.java
    │       │   │   ├── MessageRepository.java
    │       │   │   ├── NotificationRepository.java
    │       │   │   ├── ReportRepository.java
    │       │   │   ├── AuditLogRepository.java
    │       │   │   ├── UserFavoriteRepository.java  ← (Part5)
    │       │   │   ├── UserBlacklistRepository.java ← (Part5)
    │       │   │   ├── UserDocumentRepository.java  ← (Part5)
    │       │   │   ├── CouponRepository.java        ← (Part5)
    │       │   │   ├── CouponUsageRepository.java   ← (Part5)
    │       │   │   └── PaymentRepository.java       ← (Part5)
    │       │   │
    │       │   ├── service/                         ← Logique métier pure (règles domaine)
    │       │   │   ├── TripDomainService.java       ← Validation réservation, avis, taux remplissage
    │       │   │   └── UserDomainService.java       ← Validation inscription, OTP, tokens
    │       │   │
    │       │   ├── valueobject/                     ← Objets valeur immuables
    │       │   │   ├── Money.java                   ← Montant + devise (add, multiply…)
    │       │   │   ├── Currency.java                ← FBU / USD / EUR + taux de change
    │       │   │   ├── GeoLocation.java             ← Coordonnées GPS + distance Haversine
    │       │   │   └── Rating.java                  ← Note 0-5 + recalcul moyenne
    │       │   │
    │       │   ├── event/                           ← Événements domaine (Spring Events)
    │       │   │   ├── DomainEvent.java             ← Interface commune
    │       │   │   ├── TripBookedEvent.java
    │       │   │   ├── TripCancelledEvent.java
    │       │   │   ├── ReviewCreatedEvent.java
    │       │   │   └── UserBlockedEvent.java
    │       │   │
    │       │   └── exception/                       ← Exceptions métier typées
    │       │       ├── DomainException.java         ← Base avec code erreur
    │       │       ├── UserNotFoundException.java
    │       │       ├── TripNotFoundException.java
    │       │       ├── BookingNotFoundException.java
    │       │       ├── ReviewNotFoundException.java
    │       │       ├── NotAuthorizedException.java
    │       │       ├── ResourceAlreadyExistsException.java
    │       │       ├── InvalidOperationException.java
    │       │       ├── InsufficientSeatsException.java
    │       │       └── AccountBlockedException.java
    │       │
    │       ├── application/                         ══ COUCHE APPLICATION (Part3) ══
    │       │   │
    │       │   ├── usecase/                         ← Un use case = une feature complète
    │       │   │   ├── AuthUseCase.java             ← Register, login, verify email, forgot password
    │       │   │   ├── TripUseCase.java             ← CRUD trip, book, cancel, complete, search
    │       │   │   ├── ReviewUseCase.java           ← Créer avis, lister, signaler
    │       │   │   ├── MessageUseCase.java          ← Envoyer, lister conversation, signaler
    │       │   │   ├── DashboardUseCase.java        ← Stats conducteur + passager
    │       │   │   ├── AdminUseCase.java            ← Dashboard admin, users, modération, export
    │       │   │   ├── UserProfileUseCase.java      ← Profil, avatar, MDP, 2FA (Part5)
    │       │   │   ├── FavoritesUseCase.java        ← Favoris + liste noire (Part5)
    │       │   │   ├── NotificationUseCase.java     ← Centre notifications (Part5)
    │       │   │   ├── DocumentUseCase.java         ← Upload docs, approbation admin (Part5)
    │       │   │   ├── CouponUseCase.java           ← Validation coupons (Part5)
    │       │   │   ├── StripeUseCase.java           ← PaymentIntent, webhook, remboursement (Part5)
    │       │   │   └── UserMapper.java              ← Interface mapper User → UserResponse
    │       │   │
    │       │   ├── dto/
    │       │   │   ├── request/                     ← DTOs entrants (validation @NotBlank, @Email…)
    │       │   │   │   ├── LoginRequest.java
    │       │   │   │   ├── RegisterRequest.java
    │       │   │   │   ├── CreateTripRequest.java
    │       │   │   │   ├── BookTripRequest.java
    │       │   │   │   ├── CancelBookingRequest.java
    │       │   │   │   ├── CreateReviewRequest.java
    │       │   │   │   ├── SendMessageRequest.java
    │       │   │   │   ├── UpdateProfileRequest.java
    │       │   │   │   ├── ChangePasswordRequest.java
    │       │   │   │   ├── TwoFaVerifyRequest.java
    │       │   │   │   ├── TripSearchRequest.java
    │       │   │   │   ├── StopPointRequest.java
    │       │   │   │   ├── TripPreferencesRequest.java
    │       │   │   │   ├── ReportRequest.java
    │       │   │   │   ├── AdminBlockUserRequest.java
    │       │   │   │   └── AdminReviewReportRequest.java
    │       │   │   │
    │       │   │   └── response/                    ← DTOs sortants
    │       │   │       ├── ApiResponse.java         ← Enveloppe universelle {success, data, error}
    │       │   │       ├── PageResponse.java        ← Pagination générique
    │       │   │       ├── AuthResponse.java
    │       │   │       ├── UserResponse.java
    │       │   │       ├── VehicleResponse.java
    │       │   │       ├── TravelPreferencesResponse.java
    │       │   │       ├── TripResponse.java
    │       │   │       ├── TripUserResponse.java
    │       │   │       ├── PassengerResponse.java
    │       │   │       ├── StopPointResponse.java
    │       │   │       ├── TripPreferencesResponse.java
    │       │   │       ├── ReviewResponse.java
    │       │   │       ├── ReviewAuthorResponse.java
    │       │   │       ├── MessageResponse.java
    │       │   │       ├── ConversationResponse.java
    │       │   │       ├── ConversationParticipant.java
    │       │   │       ├── NotificationResponse.java
    │       │   │       ├── ReportResponse.java
    │       │   │       ├── DocumentResponse.java
    │       │   │       └── AuditLogResponse.java
    │       │   │
    │       │   └── port/out/                        ← Interfaces des adaptateurs (Hexagonal)
    │       │       ├── JwtPort.java                 ← Génération / validation JWT
    │       │       ├── EmailPort.java               ← Envoi d'emails
    │       │       ├── AuditPort.java               ← Logging d'audit
    │       │       ├── NotificationPort.java        ← Push notifications
    │       │       ├── TwoFaPort.java               ← TOTP 2FA
    │       │       ├── StoragePort.java             ← Stockage fichiers
    │       │       └── ExportPort.java              ← Export CSV / Excel
    │       │
    │       └── infrastructure/                      ══ COUCHE INFRA (Part4 + Part5) ══
    │           │
    │           ├── adapter/                         ← Implémentations des ports
    │           │   ├── audit/
    │           │   │   └── AuditLogAdapter.java     ← Persistance async des audit logs
    │           │   ├── email/
    │           │   │   └── EmailAdapter.java        ← Emails HTML avec templates inline
    │           │   ├── export/
    │           │   │   └── ExportAdapter.java       ← CSV (OpenCSV) + Excel stylé (Apache POI)
    │           │   ├── twofa/
    │           │   │   └── TwoFaAdapter.java        ← TOTP (java-otp) + QR Code (ZXing)
    │           │   └── storage/
    │           │       └── LocalStorageAdapter.java ← Stockage local (→ S3 en production)
    │           │
    │           ├── config/                          ← Configurations Spring
    │           │   ├── SecurityConfig.java          ← Filtres, CORS, autorisation par rôle
    │           │   ├── WebSocketConfig.java         ← STOMP broker (topic, queue, user)
    │           │   ├── CacheConfig.java             ← Caffeine (trips, users, stats…)
    │           │   └── OpenApiConfig.java           ← Swagger UI avec JWT bearer
    │           │
    │           ├── security/
    │           │   ├── JwtService.java              ← Impl JwtPort (JJWT 0.12)
    │           │   ├── JwtAuthFilter.java           ← OncePerRequestFilter
    │           │   └── SecurityUtils.java           ← currentUserId(), isAdmin()…
    │           │
    │           ├── scheduler/
    │           │   └── DailyStatsScheduler.java     ← Stats nuit, nettoyage tokens, auto-complétion
    │           │
    │           ├── web/
    │           │   ├── advice/
    │           │   │   └── GlobalExceptionHandler.java  ← Gestion centralisée des erreurs
    │           │   └── controller/                  ← Controllers REST (1 controller = 1 domaine)
    │           │       ├── AuthController.java      ← /auth/**
    │           │       ├── TripController.java      ← /trips/**
    │           │       ├── ReviewController.java    ← /reviews/**
    │           │       ├── MessageController.java   ← /messages/**
    │           │       ├── DashboardController.java ← /dashboard/**
    │           │       ├── AdminController.java     ← /admin/**
    │           │       ├── UserProfileController.java ← /profile/**
    │           │       ├── FavoritesController.java ← /favorites/** + /blacklist/**
    │           │       ├── NotificationController.java ← /notifications/**
    │           │       ├── DocumentController.java  ← /documents/** + /admin/documents/**
    │           │       ├── PaymentController.java   ← /payments/**
    │           │       └── FileController.java      ← /files/** (serveur statique)
    │           │
    │           └── websocket/
    │               ├── WebSocketNotificationService.java ← Impl NotificationPort + push GPS
    │               └── WebSocketController.java    ← @MessageMapping (GPS, ping)
    │
    └── test/java/com/shareway/                     ← Tests (à compléter)
```
