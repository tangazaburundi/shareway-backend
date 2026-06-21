# 🚗 Shareway Backend

Backend Spring Boot 3.2 pour la plateforme de covoiturage **Shareway**.  
Architecture **DDD / Hexagonale** · MySQL + Flyway · JWT + 2FA · WebSocket · Stripe

---

## 🏗️ Architecture

```
src/main/java/com/shareway/
│
├── SharewayApplication.java              ← Point d'entrée Spring Boot
│
├── domain/                               ← COUCHE DOMAINE (cœur métier pur)
│   ├── model/                            ← Entités JPA
│   ├── repository/                       ← Interfaces (Ports entrants)
│   ├── service/                          ← Logique métier pure
│   ├── valueobject/                      ← Objets valeur immuables
│   ├── event/                            ← Événements domaine
│   └── exception/                        ← Exceptions métier
│
├── application/                          ← COUCHE APPLICATION (orchestration)
│   ├── usecase/                          ← Cas d'usage (1 classe = 1 feature)
│   ├── dto/
│   │   ├── request/                      ← DTOs entrants (validation Jakarta)
│   │   └── response/                     ← DTOs sortants
│   └── port/out/                         ← Interfaces des adaptateurs sortants
│
└── infrastructure/                       ← COUCHE INFRA (détails techniques)
    ├── adapter/
    │   ├── audit/                        ← Persistance des logs d'audit
    │   ├── email/                        ← Envoi d'emails (JavaMailSender)
    │   ├── export/                       ← CSV (OpenCSV) + Excel (Apache POI)
    │   ├── storage/                      ← Stockage fichiers (local → S3)
    │   └── twofa/                        ← TOTP + QR Code (java-otp + ZXing)
    ├── config/                           ← Spring Security, WebSocket, Cache, OpenAPI
    ├── security/                         ← JWT filter, SecurityUtils
    ├── scheduler/                        ← Tâches planifiées (stats, cleanup)
    ├── web/
    │   ├── advice/                       ← GlobalExceptionHandler
    │   └── controller/                   ← Controllers REST
    └── websocket/                        ← WebSocket (notifications + GPS)
```

---

## 🚀 Démarrage rapide

### Prérequis
- Java 21+
- Maven 3.9+
- Docker & Docker Compose

### 1. Variables d'environnement
```bash
cp .env.example .env
# Éditez .env avec vos vraies valeurs
```

### 2. Démarrer MySQL
```bash
cd docker
docker-compose up -d mysql
```

### 3. Lancer l'application
```bash
mvn spring-boot:run
# Flyway migre automatiquement la BDD au démarrage
```

### 4. Tout en Docker
```bash
cd docker
docker-compose up -d
```

---

## 🌐 URLs

| Service | URL |
|---------|-----|
| API REST | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/api/v1/swagger-ui |
| Actuator Health | http://localhost:8080/api/v1/actuator/health |
| phpMyAdmin | http://localhost:8081 |

---

## 📡 Endpoints principaux

### Auth
```
POST /auth/register          Inscription
POST /auth/login             Connexion (+ 2FA si activé)
GET  /auth/verify-email/{t}  Vérification email
POST /auth/forgot-password   Réinitialisation MDP
```

### Trajets
```
GET  /trips                  Recherche de trajets
POST /trips                  Créer un trajet (conducteur)
GET  /trips/{id}             Détail d'un trajet
POST /trips/{id}/join        Rejoindre un trajet (passager)
POST /trips/{id}/cancel      Annuler un trajet
GET  /trips/my               Mes trajets (conducteur)
GET  /trips/my/bookings      Mes réservations (passager)
```

### Profil
```
GET  /profile                Mon profil
PUT  /profile                Mettre à jour
POST /profile/avatar         Upload photo
POST /profile/change-password
POST /profile/2fa/setup      Démarrer 2FA → QR Code
POST /profile/2fa/enable     Activer 2FA
POST /profile/2fa/disable    Désactiver 2FA
```

### Social
```
GET  /favorites              Mes favoris
POST /favorites/{userId}     Ajouter favori
GET  /blacklist              Ma liste noire
POST /blacklist/{userId}     Blacklister
POST /reviews                Laisser un avis
GET  /messages/{userId}      Conversation
POST /messages               Envoyer un message
```

### Admin (rôle ADMIN / MODERATOR)
```
GET  /admin/dashboard        Stats temps réel
GET  /admin/users            Liste utilisateurs
POST /admin/users/{id}/block     Bloquer
POST /admin/users/{id}/unblock   Débloquer
POST /admin/users/{id}/verify-identity  Valider identité
GET  /admin/reviews/flagged       Avis signalés
POST /admin/reviews/{id}/approve  Approuver
POST /admin/reviews/{id}/reject   Rejeter
GET  /admin/messages/flagged      Messages signalés
GET  /admin/reports               Signalements
GET  /admin/audit                 Logs d'audit
GET  /admin/export/users/csv      Export CSV
GET  /admin/export/users/excel    Export Excel
```

---

## 🗄️ Base de données (27 tables)

Migrations Flyway dans `src/main/resources/db/migration/` :

| Migration | Tables |
|-----------|--------|
| V1 | users, vehicles, trips, trip_preferences, stop_points, bookings, reviews, messages |
| V2 | notifications, user_documents, refresh_tokens, two_fa_backup_codes, login_history, password_reset_tokens |
| V3 | user_favorites, user_blacklist, reports, coupons, coupon_usages, trip_locations, payments |
| V4 | audit_logs, daily_stats, driver_earnings, admin_roles, system_settings |
| V5 | Vues SQL, index composites, procédures stockées |

---

## 🔧 Technologies

| Technologie | Usage |
|-------------|-------|
| Spring Boot 3.2 | Framework principal |
| Spring Security | JWT stateless |
| Spring WebSocket + STOMP | Notifications temps réel, GPS |
| Flyway | Migrations BDD |
| MySQL 8.2 | Base de données |
| MapStruct | Mapping DTO ↔ Entité |
| Lombok | Réduction boilerplate |
| jjwt 0.12 | Tokens JWT |
| java-otp + ZXing | 2FA TOTP + QR Code |
| JavaMailSender | Emails HTML |
| Apache POI | Export Excel |
| OpenCSV | Export CSV |
| Stripe Java SDK | Paiements |
| Caffeine | Cache L1 |
| Springdoc OpenAPI | Swagger UI |

---

## 📁 Structure complète des fichiers

Voir `STRUCTURE.md` pour l'arborescence complète avec description de chaque fichier.
