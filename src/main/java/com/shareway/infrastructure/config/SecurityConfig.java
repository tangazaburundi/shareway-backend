/*
package com.shareway.infrastructure.config;

import com.shareway.infrastructure.security.JwtAuthFilter;
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
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(c -> c.disable())
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ── Auth (public) ─────────────────────────────────────────────
                        .requestMatchers(HttpMethod.POST,
                                "/auth/login", "/auth/register", "/auth/forgot-password").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/auth/verify-email/**").permitAll()

                        // ── Trajets publics ────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,
                                "/trips", "/trips/{id}", "/trips/share/**").permitAll()

                        // ── Fichiers statiques ─────────────────────────────────────────
                        .requestMatchers("/static-files/**").permitAll()

                        // ── Infra ──────────────────────────────────────────────────────
                        .requestMatchers(
                                "/actuator/health",
                                "/swagger-ui/**", "/swagger-ui.html",
                                "/api-docs/**",
                                "/ws/**").permitAll()

                        // ── Admin ──────────────────────────────────────────────────────
                        .requestMatchers("/admin/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "MODERATOR")

                        // ── Tout le reste : authentifié ────────────────────────────────
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    */
/**
 * Désactiver le gestionnaire de ressources statiques par défaut de Spring MVC.
 * Sans ça, Spring essaie de servir "trips/xxx/share" comme un fichier
 * et lance NoResourceFoundException avant même d'atteindre les controllers.
 *//*

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Aucun handler de ressource statique — tout passe par les controllers REST.
        // Le FileController (/static-files/**) gère lui-même ses fichiers.
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:4200",
                "http://localhost:4201",
                "https://*.shareway.bi"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
*/


package com.shareway.infrastructure.config;

import com.shareway.infrastructure.security.JwtAuthFilter;
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

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(c -> c.disable())
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ── Auth publique ──────────────────────────────────────────────
                        .requestMatchers(HttpMethod.POST,
                                "/auth/login", "/auth/register", "/auth/forgot-password").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/auth/verify-email/**").permitAll()

                        // ── Trajets publics ────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,
                                "/trips", "/trips/{id}", "/trips/share/**").permitAll()

                        // ── Avis publics ───────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/reviews/user/**").permitAll()

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
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:4200",
                "http://localhost:4201",
                "https://*.shareway.bi"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

