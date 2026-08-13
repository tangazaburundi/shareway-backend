-- ============================================================
-- V21 : Verrouillage permanent après 3 blocages + re-hachage admin
-- ============================================================

-- 1. Nouveaux champs pour le verrouillage permanent
ALTER TABLE users
    ADD COLUMN lock_count         INT         NOT NULL DEFAULT 0,
    ADD COLUMN permanently_locked TINYINT(1)  NOT NULL DEFAULT 0;

-- 2. Re-hachage de l'admin par défaut vers bcrypt(sha256(mot_de_passe))
--    Le frontend envoie désormais un pré-hachage SHA-256 : le mot de passe
--    n'apparaît plus en clair dans l'inspection réseau.
--    sha256("Rurenza2020+") = 022c2829d01746602a6321b4c7a31263d2856f27165626d31bcd91578b2e8a4b
--    bcrypt(sha256, rounds=12) du hash ci-dessus.
UPDATE users
SET password_hash = '$2b$12$2TS0//ZI67ERS7kC6WVlO.xb84n.m2ptQRyZtRtjlpMm9WRvGlHde'
WHERE email = 'sharewaybdi@gmail.com';
