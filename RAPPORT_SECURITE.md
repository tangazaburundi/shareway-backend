# 🔒 Rapport d'audit de sécurité — Shareway Backend

> **Date :** 08/09/2026
> **Périmètre :** analyse statique complète du code source (Spring Boot 3.2, DDD/Hexagonal)
> **Objectif :** identifier les failles de sécurité et proposer des solutions avant implémentation

---

## 📋 Synthèse

| Criticité | Nombre |
|-----------|--------|
| 🔴 Critique | 12 |
| 🟠 Élevée | 14 |
| 🟡 Moyenne | 12 |
| 🟢 Bonne pratique / Amélioration | 8 |

---

## 🔴 1. Failles CRITIQUES

### 1.1 Secret JWT en clair dans le code (forgable)
**Fichier :** `src/main/resources/application.yml:82,86`
```yaml
jwt:
  secret: ${JWT_SECRET:dev-jwt-secret-key-change-in-production-minimum-256-bits}
  ...
admin-jwt:
  secret: ${ADMIN_JWT_SECRET:dev-admin-secret-key-change-in-production-256-bits}
```
**Risque :** si une instance (prod/staging) oublie de définir la variable d'environnement, la clé secrète publique par défaut est utilisée. N'importe qui peut alors **forger un JWT valide** et usurper n'importe quel utilisateur ou admin.
**Solution :**
- Remplacer les défauts par `?` (obligatoire) comme déjà fait dans `application-prod.yml` :
```yaml
jwt:
  secret: ${JWT_SECRET:?JWT_SECRET is required}
```
- Exiger une longueur minimale dans `JwtService` (comme `AdminJwtService`) :
```java
if (secret == null || secret.getBytes(UTF_8).length < 32)
    throw new IllegalStateException("JWT secret must be at least 32 bytes");
```
- Générer une clé forte : `openssl rand -base64 48`

---

### 1.2 Clé API Brevo (SMTP) en clair dans le repo
**Fichier :** `.env:4`
```
MAIL_PASSWORD=xsmtpsib-REDACTED_REAL_KEY_WAS_HERE
```
**Risque :** clé API SMTP **réelle et active** commitée. Un attaquant peut envoyer des emails en votre nom (spam, phishing), consommer votre quota, siphonner vos emails.
**Solution :**
1. Révoguer immédiatement cette clé côté Brevo.
2. Ajouter `.env` au `.gitignore`.
3. Utiliser un gestionnaire de secrets (GitHub Secrets, AWS Secrets Manager, Vault).
4. Effectuer une rotation régulière des clés.

---

### 1.3 Mot de passe admin + hash bcrypt en clair dans une migration
**Fichier :** `src/main/resources/db/migration/V12__admin_approval_and_role_requests.sql:30-44`
```sql
-- 3. Insérer l'admin par défaut (email: sharewaybdi@gmail.com, password: Rurenza2020+)
--    Hash bcrypt généré avec BCryptPasswordEncoder(12)
INSERT INTO users (...) VALUES (UUID(), 'Admin', 'ShareWay', 'sharewaybdi@gmail.com',
    '$2a$12$qXKUEvhU7vArPmQl9nfafedBKhXn4v4cjGYy4eK04KSL8OBNgJBqG', ...);
```
**Risque :** le compte **SUPER_ADMIN de production** est totalement compromis — password en clair + hash divulgués dans le code source.
**Solution :**
1. **Changer immédiatement** ce mot de passe en base.
2. **Supprimer le commentaire** contenant le mot de passe en clair.
3. Ne **jamais** insérer d'admin par défaut via une migration SQL versionnée/commitée. Créer l'admin via un script d'initialisation sécurisé (seed) non versionné, ou une commande admin one-shot, avec mot de passe fourni par variable d'environnement.
4. À terme : créer une nouvelle migration `V48__rotate_admin_password.sql` avec un hash généré depuis une variable d'env (ou le faire manuellement en prod).

---

### 1.4 Webhook Stripe non vérifié et inopérant
**Fichier :** `StripeUseCase.java:218-223` + `PaymentController.java:32-39`
```java
public void handleWebhook(String payload, String sigHeader) {
    Stripe.apiKey = stripeSecretKey;
    // In production: verify signature with Stripe.webhookEndpointSecret
    log.info("Stripe webhook received (signature check should be added in production)");
}
```
**Risques :**
- La signature `Stripe-Signature` n'est **jamais vérifiée** → n'importe quel payload est accepté (faux `200 OK`).
- Aucun traitement d'événement (confirmation/capture/remboursement) n'est branché.
- L'endpoint n'est pas `permitAll` → Stripe (sans JWT) serait rejeté en 401 ; mais tout utilisateur authentifié peut l'appeler.
**Solution :**
```java
@PostMapping("/webhook")
public ResponseEntity<Void> webhook(
        @RequestBody String payload,
        @RequestHeader("Stripe-Signature") String sig) {
    stripeUseCase.handleWebhook(payload, sig);
    return ResponseEntity.ok().build();
}
```
```java
public void handleWebhook(String payload, String sigHeader) {
    try {
        Event event = Webhook.constructEvent(payload, sigHeader, webhookEndpointSecret); // ← vérif signature
        switch (event.getType()) {
            case "payment_intent.succeeded":
                // confirmer le paiement, marquer le booking payé
                break;
            case "payment_intent.capture_failed":
            case "charge.refunded":
                // gérer
                break;
        }
    } catch (SignatureVerificationException e) {
        throw new InvalidOperationException("Invalid Stripe signature");
    }
}
```
- Ajouter `/payments/webhook` à la liste `permitAll` dans `SecurityConfig` (c'est sûr car la signature protège l'endpoint).
- Rendre le traitement **idempotent** (on peut recevoir un événement plusieurs fois).

---

### 1.5 IDOR — Profil complet (email, téléphone, 2FA) de n'importe quel utilisateur
**Fichier :** `UserProfileController.java:124-128` + `UserProfileUseCase.java:187-198`
```java
@GetMapping("/{userId}")
public ResponseEntity<ApiResponse<UserResponse>> getUserProfile(@PathVariable String userId) {
    return ResponseEntity.ok(ApiResponse.ok(userProfileUseCase.getProfile(userId)));
}
```
L'endpoint renvoie **email privé, téléphone (même si `phoneVisible=false`), état 2FA, blockReason, lastLoginAt** de n'importe quel utilisateur dont vous connaissez l'ID.
**Solution :**
- Créer un DTO **profil public** distinct (sans email/phone/2FA/blockReason/lastLoginAt), en miroir de ce que fait déjà correctement `UserUseCase.toResponse(user, false)`.
```java
// UserProfileUseCase
public UserResponse getPublicProfile(String userId) {
    User user = ...;
    return toPublicResponse(user);  // masque les champs sensibles
}
```

---

### 1.6 IDOR — Détails d'une course + facture/reçu accessibles à tous
**Fichier :** `RideUseCase.java:312-316` (`getRideById`), `RideController.java:123-129,279-303`
```java
public RideResponse getRideById(String rideId, String userId) {
    RideRequest ride = rideRequestRepository.findById(rideId).orElseThrow(...);
    return toResponse(ride, null);   // ← le paramètre userId est IGNORÉ
}
```
Aucune vérification que le demandeur est passager, chauffeur ou admin. Expose : noms, `driverPhone`, `driverLicenseId`, plaque d'immatriculation, adresses, prix, `paymentStatus`, `driverEarnings`, `cancelReason`. Permet aussi de générer la **facture PDF** de n'importe quelle course.
**Solution :**
```java
public RideResponse getRideById(String rideId, String userId) {
    RideRequest ride = ...;
    boolean isPassenger = ride.getPassenger().getId().equals(userId);
    boolean isDriver = ride.getDriver().getId().equals(userId);
    boolean isAdmin = SecurityUtils.isAdmin(); // sous réserve d'implémentation
    if (!isPassenger && !isDriver && !isAdmin)
        throw new NotAuthorizedException("Not your ride");
    return toResponse(ride, null);
}
```

---

### 1.7 Escalade de privilèges MODERATOR → SUPER_ADMIN
**Fichier :** `AdminController.java:210-217` + `AdminUseCase.java:632-653`
Un MODERATOR peut s'auto-attribuer (ou attribuer à n'importe qui) le rôle `SUPER_ADMIN` via :
```
PUT /admin/users/{id}/system-role  {"systemRole": "SUPER_ADMIN"}
```
**Solution :**
- Restreindre au niveau méthode :
```java
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public ResponseEntity<...> assignSystemRole(...)
```
- Vérifier au niveau service que celui qui attribue un rôle **>= au sien** est bloqué, et empêcher l'auto-modification.
```java
public UserResponse assignSystemRole(String userId, String systemRole, String adminId) {
    AdminRole admin = adminRoleRepository.findByUserId(adminId).orElseThrow(...);
    SystemRole target = SystemRole.fromString(systemRole);
    if (admin.getRole().ordinal() <= target.ordinal() && !isSuperAdmin(admin))
        throw new NotAuthorizedException("Cannot assign a role >= your own");
}
```

---

### 1.8 Documents d'identité servis SANS authentification
**Fichier :** `SecurityConfig.java:87` + `FileController.java:37-76`
```java
.requestMatchers("/static-files/**").permitAll()
```
Les **pièces d'identité, permis, cartes grises** (`/static-files/documents/{userId}/identity/{uuid}.pdf`) sont téléchargeables par quiconque connaît l'URL, sans authentification ni contrôle de propriété.
**Solution :**
- **Option A (recommandée) :** retirer les documents du chemin public. Les servir via un endpoint authentifié qui vérifie que l'appelant est le propriétaire ou un admin :
```java
@PreAuthorize("isAuthenticated()")
public ResponseEntity<Resource> serveDocument(@PathVariable userId, @PathVariable fileId) {
    // vérifier propriété ou admin, puis servir
}
```
- **Option B :** stocker les documents dans un bucket privé (S3) avec **URL signées** à durée limitée renvoyées à la demande.
- Ne garder `permitAll` que pour les contenus réellement publics (avatars).

---

### 1.9 Race condition — Surbooking de places disponibles
**Fichier :** `TripUseCase.java:612-634` + `Trip.java:188-196`
Logique `read → modify → write` **sans verrou** (`@Version` absente, pas de `SELECT FOR UPDATE`). Deux réservations concurrentes peuvent toutes lire le même `availableSeats` et sur-réserver.
**Solution :**
- **Option A :** ajouter `@Version` sur `Trip` (locking optimiste) + gérer `OptimisticLockException`.
- **Option B (recommandée) :** requête atomique :
```java
@Modifying
@Query("UPDATE Trip t SET t.availableSeats = t.availableSeats - :seats " +
       "WHERE t.id = :id AND t.availableSeats >= :seats AND t.status = 'OPEN'")
int decrementSeats(@Param("id") String id, @Param("seats") int seats);
// si retour == 0 → pas assez de places
```

---

### 1.10 Path traversal — écriture / suppression de fichiers arbitraires
**Fichier :** `LocalStorageAdapter.java:39-42,64-65`
- **Upload :** le `folder` est partiellement contrôlé par l'utilisateur (`documents/{userId}/{type}` avec `type` non validé, `avatars/{userId}`). Aucun `normalize()` + `startsWith(uploadRoot)` (contrairement à `FileController`).
- **Delete :** `relativePath` extrait de l'URL, `.normalize()` appliqué mais **pas de vérification** que le résultat reste sous `uploadDir` → suppression arbitraire possible.
**Solution :**
```java
public String upload(MultipartFile file, String folder) {
    if (!isSafeFolder(folder)) throw new InvalidOperationException("Invalid folder");
    Path dir = Paths.get(uploadDir, folder).toAbsolutePath().normalize();
    Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
    if (!dir.startsWith(root)) throw new InvalidOperationException("Invalid folder");
    ...
}

public void delete(String url) {
    Path file = Paths.get(uploadDir).resolve(relativePath).toAbsolutePath().normalize();
    Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
    if (!file.startsWith(root)) throw new InvalidOperationException("Invalid path");
    Files.deleteIfExists(file);
}
// + whitelist de caractères pour folder : [a-zA-Z0-9/_-], rejeter ".."
```

---

### 1.11 Fuite de messages privés via WebSocket `/topic/messages`
**Fichier :** `MessageController.java:61` + `WebSocketConfig.java:23`
```java
messaging.convertAndSend("/topic/messages", msg);   // tous les messages privés sur topic global
```
Le broker STOMP expose `/topic` sans restriction (pas d'intercepteur SUBSCRIBE) → tout client WS peut écouter **tous les messages privés** de tous les utilisateurs.
**Solution :**
- Remplacer `convertAndSend("/topic/messages", ...)` par un envoi ciblé par utilisateur :
```java
messaging.convertAndSendToUser(receiverId, "/queue/messages", msg); // /user/{receiverId}/queue/messages
```
- Ajouter un interceptor **SUBSCRIBE** (cf. §2.2) qui autorise uniquement `/user/**` (résolu vers le principal) et refuse `/topic/**` non autorisés.

---

### 1.12 Rate limiting contournable via X-Forwarded-For forgé
**Fichier :** `RateLimitingFilter.java:68-74`
```java
private String getClientIp(HttpServletRequest request) {
    String xfHeader = request.getHeader("X-Forwarded-For");
    if (xfHeader != null && !xfHeader.isBlank()) {
        return xfHeader.split(",")[0].trim();   // ← forgé par le client
    }
    return request.getRemoteAddr();
}
```
Tout client peut envoyer `X-Forwarded-For: <ip-arbitraire>` à chaque requête → **bypass total** du rate limiting sur login/register/forgot-password (brute force).
**Solution :**
- Ne se fier qu'à `getRemoteAddr()`, OU n'utiliser XFF **que** derrière un reverse-proxy de confiance qui écrase/valide ce header :
```yaml
server.forward-headers-strategy: framework
```
- Valider/verrouiller le `X-Forwarded-For` à l'entrée (proxy NGINX : `proxy_set_header X-Real-IP $remote_addr;`).
- Idem dans `VisitorController.java:70-76` (analytics pollués).

---

## 🟠 2. Failles ÉLEVÉES

### 2.1 Positions GPS temps réel exposées publiquement + `max` non borné
**Fichier :** `RideController.java:59-66` + `SecurityConfig.java:57-58`
```java
@GetMapping("/nearby")
public ResponseEntity<...> getNearbyDrivers(@RequestParam double lat, @RequestParam double lng,
        @RequestParam(defaultValue = "10") int max) { ... }
```
Autorisé **sans authentification**, expose `userId`, noms, note et **coordonnées GPS temps réel** des chauffeurs. `max` non borné.
**Solution :**
- Rendre le endpoint **authentifié**.
- Borner : `@RequestParam @Min(1) @Max(50) int max`, valider lat/lng (`-90..90`, `-180..180`).
- Ne renvoyer que des positions **approximatives** (arrondies) pour les non-participants.

---

### 2.2 Aucun contrôle des abonnements WebSocket (SUBSCRIBE)
**Fichier :** `WebSocketAuthChannelInterceptor.java:28-58` + `WebSocketConfig.java:23`
Seul `CONNECT` est traité. Les frames `SUBSCRIBE` ne sont jamais validées → accès aux topics d'autres trajets, GPS, **alarmes SOS admin** (`/topic/admin/sos`).
**Solution :**
```java
if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
    String dest = accessor.getDestination();
    Principal principal = accessor.getUser();
    if (dest != null && dest.startsWith("/user/")) {
        // vérifier que la partie /user/{id} == principal.getName()
        if (!isOwner(dest, principal)) throw new AccessDeniedException(...);
    } else if (dest != null && dest.startsWith("/topic/trip/")) {
        // vérifier que l'utilisateur est participant du trip
        if (!isParticipant(...)) throw new AccessDeniedException(...);
    } else if (dest != null && dest.contains("admin")) {
        if (!isAdmin(principal)) throw new AccessDeniedException(...);
    }
}
```

---

### 2.3 GPS spoofing — n'importe qui peut falsifier la position d'un trajet
**Fichier :** `WebSocketController.java:24-31`
```java
@MessageMapping("/trip/{tripId}/location")
public void updateLocation(@DestinationVariable String tripId,
        @Payload Map<String, Double> payload, Principal principal) {
    if (principal == null) return;
    double lat = payload.getOrDefault("lat", 0.0);
    double lng = payload.getOrDefault("lng", 0.0);
    notificationService.broadcastTripLocation(tripId, lat, lng);
}
```
**Aucune validation** des coordonnées ni vérification que l'émetteur est le **conducteur** du trajet.
**Solution :**
```java
public void updateLocation(@PathVariable String tripId, @Payload payload, Principal principal) {
    // 1. vérifier que principal est bien le conducteur du trip
    if (!tripDomainService.isDriver(tripId, principal.getName())) return;
    // 2. valider lat ∈ [-90,90], lng ∈ [-180,180]
    if (lat < -90 || lat > 90 || lng < -180 || lng > 180) return;
    // 3. rate limiting sur les messages GPS
    notificationService.broadcastTripLocation(tripId, lat, lng);
}
```

---

### 2.4 Refresh token / 2FA token acceptés comme token d'accès
**Fichier :** `JwtService.java:68-75` + `JwtAuthFilter.java:31`
`isValid` ne vérifie que la signature, pas le claim `type`. Un **refresh token** ou un **2FA session token** (5 min) passe comme authentification.
**Solution :** vérifier le type dans le filtre :
```java
Claims claims = parse(token);
if (!"ACCESS".equals(claims.get("type"))) return null; // rejeter refresh/2fa
```

---

### 2.5 Handshake WebSocket jamais bloqué + JWT dans query string
**Fichier :** `WebSocketHandshakeInterceptor.java:29-57` + `WebSocketAuthChannelInterceptor.java:54-56`
- Si token absent → `return true` (handshake autorisé sans auth).
- Token passé en `?token=...` → fuite dans logs/historique/Référer.
**Solution :**
- Rejeter le handshake si aucun token valide (`return false`).
- Ne plus accepter le token via query string (utiliser header STOMP `Authorization` uniquement).

---

### 2.6 XSS stocké via upload de fichiers arbitraires
**Fichier :** `DocumentUseCase.java:33-57` + `UserUseCase.java:417-431` + `LocalStorageAdapter.java` + `FileController.java:62-65`
- `DocumentUseCase` ne vérifie que la taille (10 Mo), **aucun contrôle MIME/extension/magic bytes**.
- `validateImageFile` se fie au `Content-Type` client (forgeable) et autorise `image/svg+xml` (SVG = XSS).
- `FileController` sert via `Files.probeContentType` → un `.html` ou `.svg` est servi en HTML → **XSS stocké**.
**Solution :**
1. Contrôler les **magic bytes** (vraie détection du type), pas le Content-Type client.
2. **Interdire SVG** et toute extension exécutable : whitelist `[jpg, jpeg, png, pdf, webp, gif]`.
3. Servir les fichiers avec `Content-Disposition: attachment` et/ou `X-Content-Type-Options: nosniff` + `Content-Security-Policy: sandbox` pour les contenus non-image.
4. Configurer `spring.servlet.multipart.max-file-size` et `max-request-size`.

---

### 2.7 Messages d'erreur Stripe exposés au client
**Fichier :** `StripeUseCase.java:101,156,173,189,211,251` + `GlobalExceptionHandler.java:157-161`
```java
throw new InvalidOperationException("Payment initialization failed: " + e.getMessage());
```
`InvalidOperationException` → son message est renvoyé tel quel au client (via `handleInvalidOp`), exposant des détails Stripe internes (IDs, raisons de rejet).
**Solution :** logger l'erreur complète côté serveur, renvoyer un message générique au client ; ne propager que des messages sûres.

---

### 2.8 Injection HTML / XSS dans les emails
**Fichier :** `EmailAdapter.java:76-78` + appels `AdminUseCase.java:596-598,608-613,713-716,731-735,768-774,786-791,815-820`
```java
sendHtml(to, subject, "<p>" + body + "</p>");  // body non échappé
```
`firstName`, `reason`, `comment` (contrôlés par l'utilisateur) injectés dans du HTML sans échappement → XSS dans l'email.
**Solution :** échapper systématiquement toute valeur insérée :
```java
String escaped = StringEscapeUtils.escapeHtml4(value);
```
Ou utiliser un moteur de template (Thymeleaf) avec échappement automatique.

---

### 2.9 CSV injection (formule) dans les exports admin
**Fichier :** `ExportAdapter.java:19-35` + `AdminUseCase.java:489-505` (First/Last name, Email)
```java
values[i] = v != null ? v.toString() : "";   // cellule brute
```
Un utilisateur nommé `=cmd|' /C calc'!A0` ou `@SUM(...)` → **exécution de formule** à l'ouverture dans Excel (CWE-1236).
**Solution :** préfixer les cellules commençant par `=,+,−,@,\t,\r` avec `'` (ou espacer).

---

### 2.10 Rating de course frauduleux (pair non participant)
**Fichier :** `RideUseCase.java:322-368`
`rateRide` vérifie seulement `status == COMPLETED`, pas que l'auteur est passager ou chauffeur de la course.
**Solution :**
```java
boolean isPassenger = ride.getPassenger().getId().equals(userId);
boolean isDriver = ride.getDriver().getId().equals(userId);
if (!isPassenger && !isDriver) throw new NotAuthorizedException("Not your ride");
```

---

### 2.11 Abus de parrainage (réutilisation en race condition, pas d'unicité)
**Fichier :** `ReferralUseCase.java:36-53`
Contrôle `PENDING` → `COMPLETE` en read-modify-write sans verrou ; aucune contrainte `unique` sur `referred_user_id` ; pas de détection de boucle ni d'audit.
**Solution :**
- Contrainte `UNIQUE` en base sur `referred_user_id`.
- Mise à jour atomique : `UPDATE referrals SET status='COMPLETED' WHERE id=? AND status='PENDING'` (retour 0 → déjà utilisé).
- Détection de cycle / limite d'utilisations par compte.

---

### 2.12 Coupons non validés / non consommés
**Fichier :** `CouponUseCase.java:22-53`
La validation est **du code mort** (jamais câblée dans `TripUseCase.book` ni `StripeUseCase`), et si elle était câblée, elle ne consomme jamais le coupon (`currentUses` jamais incrémenté, aucun `CouponUsage` créé).
**Solution :** câbler l'application du coupon dans le flux de réservation, en incrémentant atomiquement les usages (avec verrou/`@Version`) et en vérifiant le montant côté serveur.

---

## 🟡 3. Failles MOYENNES

### 3.1 `GET /trips/{id}` public expose passagers + téléphone conducteur
**Fichier :** `TripController.java:123-127` + `TripUseCase.java:937-973`
Liste des passagers (noms, bookingId, statut) + téléphone conducteur rendus visibles **aux non-authentifiés**.
**Solution :** ne renvoyer la liste des passagers que si authentifié (et idéalement participant/admin) ; masquer `phone`/`bookingId` en public.

### 3.2 Pagination sans bornes partout (DoS mémoire/DB)
Ex : `TripController.java:63-67,86-89`, `MessageController.java:86-92`, `NotificationController.java:27-33`, `AdminController.java` (multiples), `RideController.java:60-65`.
**Solution :** `@Min(0) @Max(100)` sur `size`, `@Min(0)` sur `page` sur tous les DTO/params paginés.

### 3.3 Corps de requête `Map<String,String>` sans validation (~30 endpoints)
Ex : `AuthController.java:58,64,70,81`, `AdminController.java:196,205,213,239,249,276,287,305,326`, `RideController.java:136,208,260,490,537,557,580`, etc.
**Solution :** remplacer par des DTO dédiés avec `@Valid` + `@Size`/`@Pattern`.

### 3.4 `Booking.isActive()` inclut `COMPLETED` → annulation d'un trajet terminé
**Fichier :** `Booking.java:165-169` + `TripUseCase.java:741,961`
**Solution :** exclure `COMPLETED` de `isActive()` ; rejeter l'annulation d'un booking déjà payé/capturé.

### 3.5 Avis `approved` jamais initialisé à true
**Fichier :** `ReviewUseCase.java:111-120`
Les avis normaux sont invisibles publiquement (`findApprovedByTarget`), mais `updateRating` est appelé avant approbation.
**Solution :** initialiser `approved=true` pour les avis non signalés (ou introduire un statut explicite), et ne recalculer la note que sur les avis approuvés.

### 3.6 Fuites d'infos par `GlobalExceptionHandler`
**Fichier :** `GlobalExceptionHandler.java:62-67,71-76`
`handleNotFound` renvoie le chemin ; `handleMediaType` expose les media types.
**Solution :** messages génériques en prod, détails en log.

### 3.7 Buckets rate limiting in-memory (bypass multi-instances + fuite mémoire)
**Fichier :** `RateLimitingConfig.java`
**Solution :** utiliser Redis (ou un stockage partagé), avec expiration des buckets (éviter la fuite mémoire), idéalement une solution comme Bucket4j + Redis.

### 3.8 Topics absents / pas de rate limiting sur messages STOMP
**Fichier :** `WebSocketConfig`, `WebSocketController`
**Solution :** ajouter rate limiting par user sur SEND, limiter la taille des payloads.

### 3.9 Enumération d'emails newsletter
**Fichier :** `NewsletterController.java:33-42`
Réponse distincte « déjà inscrit » vs « inscription réussie ».
**Solution :** réponse générique identique dans les deux cas.

### 3.10 Notification de lecture sans vérification de participation (WS)
**Fichier :** `WebSocketController.java:46-55`
`conversationId` et `userId` concaténés dans le topic sans vérification → topic injection possible.
**Solution :** valider `conversationId` (format UUID), vérifier la participation avant de publier.

### 3.11 Email adapté mais sans validation de format
**Fichier :** `NewsletterController`, `AuthController` (sujet `\r\n` non filtré → échec silencieux ou comportement variable)
**Solution :** `@Email` validations, filtrer CR/LF.

### 3.12 Jokers `%`/`_` non échappés dans les recherches `LIKE`
**Fichier :** `UserSpecifications.java:23`, `TripRepository.java`
**Solution :** échapper `%` et `_` avec `\` dans les patterns search.

---

## 🟢 4. Points POSITIFS (à préserver)

- Anti path traversal **lecture** correct dans `FileController.java:51-55`.
- Ownership vérifiée sur la plupart des opérations trips/bookings/messages/notifications.
- `deleteUser`/`deleteTrip` restreints à ADMIN/SUPER_ADMIN.
- `BCryptPasswordEncoder(12)` — coût correct.
- Rate limiting ciblé login/register/forgot-password (sous réserve du bypass XFF).
- Aucune injection SQL directe (toutes les requêtes natives sont paramétrées) ✅
- Anti-énumération de comptes via hash factice sur login admin (`AdminUseCase.java:106-107`) + timing-safe.
- `application-prod.yml` exige bien les secrets via `?`.
- JWT refresh + courtes expirations configurées.
- CORS borné avec `setAllowedOriginPatterns` + `allowCredentials`.

---

## 🛠️ 5. Améliorations générales proposées (haut niveau)

1. **Audit des opérations financières** — `StripeUseCase` n'audite aucune opération (création, capture, annulation, transfert, remboursement). Ajouter `AuditPort`.
2. **Audit** des exports en masse, `updateSystemSetting`, `deleteMessage`, `applyReferralCode`.
3. **`AuditLogAdapter` @Async** — les erreurs d'écriture d'audit sont perdues silencieusement. Ajouter un mécanisme de reprise/alerte.
4. **Contrôle d'accès `createEscrowPaymentIntent`** — vérifier `booking.getPassenger().getId().equals(currentUser)`.
5. **Vérification du compte Stripe du conducteur** avant `createTransfer` (éviter transfert vers compte arbitraire).
6. **Gestion des arrondis** — `convertToSmallestUnit` tronque via `longValue()`. Utiliser `setScale`/arrondi explicite, règle par devise.
7. **Idempotence des PaymentIntents** — ne pas recréer un intent si `stripePaymentIntentId` existe déjà.
8. **`application-prod.yml`** — activer `useSSL=true` pour MySQL en prod, désactiver `try-it-out` Swagger en prod.
9. **Secrets** — rotation des clés, gitignore `.env`, gestionnaire de secrets.
10. **`instrumentation` WebSocket** — limiter le `userId` dans `convertAndSendToUser` à `principal.getName()`.
11. **Validation DTO bout-en-bout** — utiliser des DTO requête purs (pas de `Map`, pas de DTO-réponse en entrée).
12. **`SmsConfig` api_key** — ne jamais stocker de secret en clair en base (chiffrer ou externaliser).
13. **`WebSocketConfig`** — `setAllowedCredentials` explicite pour les WebSockets.
14. **`spring.servlet.multipart`** — configurer explicitement les limites max-file-size/max-request-size.
15. **Audit d'audit** — contrôler l'accès aux données en masse (`getUsers`, exports).

---

## ✅ 6. Plan d'action prioritaire recommandé

| Priorité | Action | Statut |
|----------|--------|--------|
| P0 (immédiat) | Révoguer la clé Brevo + le mot de passe admin (V12) + rotation secrets | ⏳ Action manuelle (hors code) |
| P0 | Remplacer les secrets JWT par défaut par des valeurs obligatoires | ✅ Appliqué (`application.yml`, `JwtService`) |
| P0 | Implémenter la vérification de signature du webhook Stripe | ✅ Appliqué (`StripeUseCase`) |
| P0 | Corriger l'upload (magic bytes + whitelist extensions + path traversal write/delete) | ✅ Appliqué (`LocalStorageAdapter`, `FileTypeValidator`) |
| P0 | Bloquer l'escalade de privilèges admin (system-role) | ✅ Appliqué (`AdminController`, `AdminUseCase`) |
| P1 | Corriger les IDOR (profil public, détails de course, factures) | ✅ Appliqué (`RideUseCase.getRideById` + contrôle ownership) |
| P1 | Ajouter l'intercepteur SUBSCRIBE + supprimer `/topic/messages` global | ✅ Appliqué (`WebSocketAuthChannelInterceptor`, `MessageController`) |
| P1 | Corriger le bypass XFF du rate limiting | ✅ Appliqué (`RateLimitingFilter`, `VisitorController`) |
| P1 | Fix race condition surbooking (requête atomique / @Version) | ✅ Appliqué (`Trip.@Version` + `findByIdForUpdate` + V48) |
| P2 | Pagination bornée, DTO validés, rate limiting GPS/WS | ✅ Partiel (`Pagination`, coordonnées GPS bornées) |
| P2 | Échappement HTML email + anti CSV injection | ✅ Appliqué (`EmailAdapter`, `ExportAdapter`) |
| P2 | Protéger les documents (endpoint authentifié / URLs signées) | ✅ Endpoint propriétaire/admin + `Content-Disposition: attachment` + `no-store` (`FileController`) |
| P2 | DTO validés (remplacement des `Map<String,String>` non typés) | ✅ Appliqué (auth, contacts d'urgence, carburant, promo, device token, admin, flag/reason) |
| P2 | Rate limiting élargi (auth + GPS + rides + messages) | ✅ Appliqué (`RateLimitingFilter`, `RateLimitingConfig`) |
| P2 | Cooldown GPS WebSocket (anti-spam) | ✅ Appliqué (`WebSocketController.updateLocation`, 2 s / trajet) |
| P2 | Store centralisé Redis pour les buckets | ✅ `DynamicRateLimitStore` : toggle admin `rateLimiterStore` (`redis`/`in-memory`) via `/admin/settings` + bascule automatique, Redis si joignable sinon mémoire (`RedisRateLimitStore` INCR/EXPIRE, `InMemoryRateLimitStore` Bucket4j+Caffeine) |
| — | Sécuriser `/rides/nearby` (GPS temps réel) | ✅ Appliqué (auth requise + max 50) |
| — | Fix GPS spoofing WebSocket (`updateLocation`) | ✅ Appliqué (vérif. conducteur + bornage) |
| — | Validation newsletter (format email) | ✅ Appliqué (`NewsletterController`) |
| — | Messages d'erreur génériques | ✅ Appliqué (`GlobalExceptionHandler`) |
| — | Migration MySQL `trips.version` | ✅ `V48__trip_optimistic_lock.sql` |
| — | Compilation + tests | ✅ `mvn compile` OK, **66/66 tests verts** |

---

*Audit statique — à compléter par un test dynamique (pentest) et la vérification sur chaque environnement.*
