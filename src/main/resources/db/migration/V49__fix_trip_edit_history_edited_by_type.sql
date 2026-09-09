-- Fix trip_edit_history column types: CHAR(36) -> VARCHAR(255)
-- Alignées sur les colonnes référencées (trips.id, users.id) et sur l'entité TripEditHistory.
-- MySQL refuse de modifier une colonne impliquée dans une FK : on la recrée autour du ALTER.

ALTER TABLE trip_edit_history DROP FOREIGN KEY trip_edit_history_ibfk_1;

ALTER TABLE trip_edit_history MODIFY COLUMN trip_id   VARCHAR(255) NOT NULL;
ALTER TABLE trip_edit_history MODIFY COLUMN edited_by VARCHAR(255) NOT NULL;

ALTER TABLE trip_edit_history
    ADD CONSTRAINT trip_edit_history_ibfk_1
        FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE;