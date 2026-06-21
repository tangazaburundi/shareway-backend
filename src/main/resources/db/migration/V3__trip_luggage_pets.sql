-- ============================================================
-- V6 : Nouvelles préférences trajet (valises + animaux)
--      + index pour la recherche par ville de départ seule
-- ============================================================

-- Ajout des colonnes de préférences valises/animaux
ALTER TABLE trip_preferences
    ADD COLUMN small_luggage   TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Petite valise acceptée',
    ADD COLUMN large_luggage   TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Grande valise acceptée';

-- La colonne pets existe déjà (V1) — on s'assure juste que l'index est bon
-- Index pour recherche par ville de départ seule (sans destination obligatoire)
CREATE INDEX idx_trips_departure_city_time
    ON trips(departure_city, departure_time, status, deleted_at);

-- Index pour la recherche fulltext uniquement sur departure_city
-- (permet de retrouver les trajets même sans saisir l'arrivée)
ALTER TABLE trips
    ADD FULLTEXT idx_trips_departure_ft (departure_city);
