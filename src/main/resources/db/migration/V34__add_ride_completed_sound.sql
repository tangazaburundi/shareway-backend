ALTER TABLE user_sound_preferences
ADD COLUMN ride_completed_sound VARCHAR(50) NOT NULL DEFAULT 'tada'
AFTER ride_cancelled_sound;
