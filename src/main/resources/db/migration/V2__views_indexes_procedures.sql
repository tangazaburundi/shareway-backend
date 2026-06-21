-- ============================================================
-- SHAREWAY - V2 : Vues, index, procédures
-- ============================================================

CREATE OR REPLACE VIEW v_driver_stats AS
SELECT
    u.id AS driver_id,
    u.first_name, u.last_name, u.email, u.rating, u.review_count,
    COUNT(DISTINCT t.id) AS total_trips,
    COUNT(DISTINCT CASE WHEN t.status = 'COMPLETED' THEN t.id END) AS completed_trips,
    COUNT(DISTINCT CASE WHEN t.status = 'CANCELLED' THEN t.id END) AS cancelled_trips,
    SUM(CASE WHEN t.status = 'COMPLETED' THEN t.total_seats - t.available_seats ELSE 0 END) AS total_passengers,
    COALESCE(SUM(de.net_amount), 0) AS total_earnings_fbu,
    AVG(CASE WHEN t.status = 'COMPLETED' AND t.total_seats > 0
        THEN (t.total_seats - t.available_seats) / t.total_seats * 100
        ELSE NULL END) AS avg_fill_rate
FROM users u
LEFT JOIN trips t ON t.driver_id = u.id AND t.deleted_at IS NULL
LEFT JOIN driver_earnings de ON de.driver_id = u.id AND de.status = 'CONFIRMED'
WHERE u.deleted_at IS NULL AND (u.role = 'DRIVER' OR u.role = 'BOTH')
GROUP BY u.id, u.first_name, u.last_name, u.email, u.rating, u.review_count;

CREATE OR REPLACE VIEW v_passenger_stats AS
SELECT
    u.id AS passenger_id,
    u.first_name, u.last_name, u.email,
    COUNT(DISTINCT b.id) AS total_bookings,
    COUNT(DISTINCT CASE WHEN b.status = 'COMPLETED' THEN b.id END) AS completed_trips,
    COUNT(DISTINCT CASE WHEN b.status = 'CANCELLED' THEN b.id END) AS cancelled_trips,
    COALESCE(SUM(b.amount_paid), 0) AS total_spent,
    COUNT(DISTINCT MONTH(b.created_at)) AS active_months
FROM users u
LEFT JOIN bookings b ON b.passenger_id = u.id AND b.deleted_at IS NULL
WHERE u.deleted_at IS NULL
GROUP BY u.id, u.first_name, u.last_name, u.email;

CREATE OR REPLACE VIEW v_admin_dashboard AS
SELECT
    (SELECT COUNT(*) FROM users WHERE deleted_at IS NULL AND is_active = 1) AS total_active_users,
    (SELECT COUNT(*) FROM users WHERE deleted_at IS NULL AND DATE(created_at) = CURDATE()) AS new_users_today,
    (SELECT COUNT(*) FROM trips WHERE deleted_at IS NULL AND status = 'OPEN') AS open_trips,
    (SELECT COUNT(*) FROM trips WHERE deleted_at IS NULL AND status = 'COMPLETED') AS completed_trips,
    (SELECT COUNT(*) FROM bookings WHERE deleted_at IS NULL AND DATE(created_at) = CURDATE()) AS bookings_today,
    (SELECT COUNT(*) FROM reports WHERE status = 'PENDING') AS pending_reports,
    (SELECT COUNT(*) FROM user_documents WHERE status = 'PENDING') AS pending_documents,
    (SELECT COUNT(*) FROM reviews WHERE is_flagged = 1 AND is_approved = 1) AS flagged_reviews,
    (SELECT COUNT(*) FROM messages WHERE is_flagged = 1) AS flagged_messages,
    (SELECT COUNT(*) FROM users WHERE is_blocked = 1) AS blocked_users;

CREATE INDEX idx_trips_search ON trips(departure_city, arrival_city, departure_time, status, deleted_at);
CREATE INDEX idx_bookings_passenger_status ON bookings(passenger_id, status, created_at);
CREATE INDEX idx_reviews_target_type ON reviews(target_user_id, type, deleted_at);
CREATE INDEX idx_messages_conv_created ON messages(sender_id, receiver_id, created_at);
CREATE INDEX idx_notifications_user_read_created ON notifications(user_id, is_read, created_at);

DELIMITER $$
CREATE PROCEDURE recalculate_user_rating(IN p_user_id VARCHAR(255))
BEGIN
    UPDATE users
    SET rating = (
        SELECT COALESCE(AVG(r.rating), 0) FROM reviews r
        WHERE r.target_user_id = p_user_id AND r.deleted_at IS NULL AND r.is_approved = 1
    ),
    review_count = (
        SELECT COUNT(*) FROM reviews r
        WHERE r.target_user_id = p_user_id AND r.deleted_at IS NULL AND r.is_approved = 1
    )
    WHERE id = p_user_id;
END$$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE calculate_daily_stats(IN p_date DATE)
BEGIN
    INSERT INTO daily_stats (
        stat_date, new_users, active_users, new_trips,
        completed_trips, cancelled_trips, total_bookings,
        total_revenue_fbu, total_messages, total_reviews, calculated_at
    )
    SELECT
        p_date,
        (SELECT COUNT(*) FROM users WHERE DATE(created_at) = p_date AND deleted_at IS NULL),
        (SELECT COUNT(DISTINCT user_id) FROM login_history WHERE DATE(created_at) = p_date AND success = 1),
        (SELECT COUNT(*) FROM trips WHERE DATE(created_at) = p_date AND deleted_at IS NULL),
        (SELECT COUNT(*) FROM trips WHERE DATE(updated_at) = p_date AND status = 'COMPLETED' AND deleted_at IS NULL),
        (SELECT COUNT(*) FROM trips WHERE DATE(updated_at) = p_date AND status = 'CANCELLED' AND deleted_at IS NULL),
        (SELECT COUNT(*) FROM bookings WHERE DATE(created_at) = p_date AND deleted_at IS NULL),
        (SELECT COALESCE(SUM(amount_paid), 0) FROM bookings WHERE DATE(created_at) = p_date AND currency = 'FBU' AND status = 'CONFIRMED'),
        (SELECT COUNT(*) FROM messages WHERE DATE(created_at) = p_date AND deleted_at IS NULL),
        (SELECT COUNT(*) FROM reviews WHERE DATE(created_at) = p_date AND deleted_at IS NULL),
        NOW()
    ON DUPLICATE KEY UPDATE
        new_users = VALUES(new_users), active_users = VALUES(active_users),
        new_trips = VALUES(new_trips), completed_trips = VALUES(completed_trips),
        cancelled_trips = VALUES(cancelled_trips), total_bookings = VALUES(total_bookings),
        total_revenue_fbu = VALUES(total_revenue_fbu), total_messages = VALUES(total_messages),
        total_reviews = VALUES(total_reviews), calculated_at = NOW();
END$$
DELIMITER ;