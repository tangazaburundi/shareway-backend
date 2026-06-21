package com.shareway.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyStatsScheduler {

    private final JdbcTemplate jdbcTemplate;

    /** Recalcule les stats de la veille chaque nuit à 00:05 */
    @Scheduled(cron = "0 5 0 * * *")
    public void computeDailyStats() {
        String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_DATE);
        log.info("Computing daily stats for {}", yesterday);
        try {
            jdbcTemplate.execute("CALL calculate_daily_stats('" + yesterday + "')");
            log.info("Daily stats computed for {}", yesterday);
        } catch (Exception e) {
            log.error("Failed to compute daily stats: {}", e.getMessage());
        }
    }

    /** Nettoyage des tokens expirés chaque dimanche à 3h */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void cleanExpiredTokens() {
        log.info("Cleaning expired tokens...");
        jdbcTemplate.update("DELETE FROM refresh_tokens WHERE expires_at < NOW() OR is_revoked = 1");
        jdbcTemplate.update("DELETE FROM password_reset_tokens WHERE expires_at < NOW() OR is_used = 1");
        log.info("Token cleanup done");
    }

    /** Auto-complétion des trajets passés à 1h du matin */
    @Scheduled(cron = "0 0 1 * * *")
    public void autoCompleteTrips() {
        log.info("Auto-completing past trips...");
        int updated = jdbcTemplate.update(
            "UPDATE trips SET status = 'COMPLETED' " +
            "WHERE status = 'OPEN' AND departure_time < DATE_SUB(NOW(), INTERVAL 2 HOUR) AND deleted_at IS NULL"
        );
        log.info("Auto-completed {} trips", updated);
    }
}
