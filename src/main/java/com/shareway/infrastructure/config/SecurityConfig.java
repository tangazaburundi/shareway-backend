package com.shareway.infrastructure.config;

import com.shareway.infrastructure.security.JwtAuthFilter;
import com.shareway.infrastructure.security.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.beans.factory.annotation.Value;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitingFilter rateLimitingFilter;

    @Value("${shareway.app.frontend-url}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(c -> c.disable())
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ── Auth publique ──────────────────────────────────────────────
                        .requestMatchers(HttpMethod.POST,
                                "/auth/login", "/auth/register", "/auth/forgot-password",
                                "/auth/reset-password", "/auth/refresh-token",
                                "/auth/resend-verification",
                                "/auth/admin/login").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/auth/verify-email/**").permitAll()

                        // ── Trajets publics ────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,
                                "/trips", "/trips/{id}", "/trips/share/**").permitAll()

                        // ── Géocodage public ────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/geocoding/**").permitAll()

                        // ── Avis publics ───────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/reviews/user/**").permitAll()

                        // ── Contact & Newsletter ─────────────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/contact", "/newsletter").permitAll()

                        // ── Publicités ──────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/public/ads/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/public/ads/{id}/click").permitAll()

                        // ── Stats publiques ────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/public/stats").permitAll()

                        // ── Visitors analytics ─────────────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/visits").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/visits/cookies").permitAll()

                        // ── Fichiers statiques ─────────────────────────────────────────
                        .requestMatchers("/static-files/**").permitAll()

                        // ── Swagger / Actuator ─────────────────────────────────────────
                        .requestMatchers(
                                "/actuator/health",
                                "/swagger-ui/**", "/swagger-ui.html",
                                "/api-docs/**", "/ws/**").permitAll()

                        // ── Admin ──────────────────────────────────────────────────────
                        .requestMatchers("/admin/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "MODERATOR")
                        // .requestMatchers("/api/v1/admin/**").permitAll()

                        // ── Tout le reste : authentifié ────────────────────────────────
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitingFilter, JwtAuthFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        String frontendUrlClean = frontendUrl != null ? frontendUrl.trim() : "";
        config.setAllowedOriginPatterns(List.of(frontendUrlClean));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

