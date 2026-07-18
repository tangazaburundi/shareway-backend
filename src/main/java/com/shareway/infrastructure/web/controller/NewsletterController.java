package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.response.ApiResponse;
import com.shareway.domain.model.NewsletterSubscriber;
import com.shareway.domain.repository.NewsletterSubscriberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/newsletter")
@RequiredArgsConstructor
@Tag(name = "Newsletter", description = "Inscription newsletter")
@Slf4j
public class NewsletterController {

    private final NewsletterSubscriberRepository newsletterSubscriberRepository;

    @PostMapping
    @Operation(summary = "S'inscrire à la newsletter")
    public ResponseEntity<ApiResponse<Void>> subscribe(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("VALIDATION_ERROR", "Email requis."));
        }

        if (newsletterSubscriberRepository.existsByEmail(email.toLowerCase().trim())) {
            return ResponseEntity.ok(ApiResponse.noContent("Déjà inscrit à la newsletter !"));
        }

        newsletterSubscriberRepository.save(
                NewsletterSubscriber.builder().email(email.toLowerCase().trim()).build());

        log.info("Newsletter subscription: {}", email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.noContent("Inscription à la newsletter réussie !"));
    }
}
