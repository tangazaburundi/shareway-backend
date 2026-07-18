-- Fix trip_edit_history.id column type: CHAR(36) -> VARCHAR(255)
-- MySQL created CHAR(36) from UUID() default, but Hibernate expects VARCHAR(255)

ALTER TABLE trip_edit_history MODIFY COLUMN id VARCHAR(255) NOT NULL;
