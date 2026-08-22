package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.response.ApiResponse;
import com.shareway.infrastructure.web.dto.ContactRequest;
import com.shareway.infrastructure.web.entity.ContactMessage;
import com.shareway.infrastructure.web.repository.ContactMessageRepository;
import com.shareway.application.port.out.EmailPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/contact")
@RequiredArgsConstructor
@Tag(name = "Contact", description = "Formulaire de contact public")
public class ContactController {

    private final EmailPort emailPort;
    private final ContactMessageRepository contactMessageRepository;

    @PostMapping
    @Operation(summary = "Envoyer un message via le formulaire de contact")
    public ResponseEntity<ApiResponse<Void>> sendContact(@Valid @RequestBody ContactRequest req) {
        String sujet = switch (req.getSujet()) {
            case "INFO" -> "Demande d'information";
            case "Covoiturage" -> "Question covoiturage";
            case "SIGNALEMENT" -> "Signalement";
            case "PARTENARIAT" -> "Demande de partenariat";
            case "TECHNIQUE" -> "Problème technique";
            default -> req.getSujet();
        };

        boolean emailSent = false;

        try {
            String body = """
                    Nouveau message depuis le formulaire de contact Shareway
                    ────────────────────────────────────────────────────────
                    Nom    : %s
                    Email  : %s
                    Sujet  : %s
                    ────────────────────────────────────────────────────────
                    %s
                    """.formatted(req.getNom(), req.getEmail(), sujet, req.getMessage());

            emailPort.sendGeneral("tangazaburundi@outlook.com", "Contact Shareway : " + sujet, body);
            emailSent = true;
        } catch (Exception e) {
            log.warn("Email send failed for contact from {}: {}", req.getEmail(), e.getMessage());
        }

        ContactMessage msg = ContactMessage.builder()
                .nom(req.getNom())
                .email(req.getEmail())
                .sujet(sujet)
                .message(req.getMessage())
                .emailSent(emailSent)
                .build();
        contactMessageRepository.save(msg);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.noContent("Votre message a été envoyé avec succès."));
    }
}
