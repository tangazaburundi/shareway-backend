-- V35: Add RENDERED and ARCHIVED statuses, rendered_at, archived_at columns, and ride_rendered_sound
ALTER TABLE ride_requests
    ADD COLUMN archived_at DATETIME NULL AFTER updated_at,
    ADD COLUMN rendered_at DATETIME NULL AFTER archived_at;

ALTER TABLE user_sound_preferences
    ADD COLUMN ride_rendered_sound VARCHAR(50) NOT NULL DEFAULT 'transfer' AFTER ride_completed_sound;
