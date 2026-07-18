package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.response.ApiResponse;
import com.shareway.infrastructure.web.dto.ContactRequest;
import com.shareway.application.port.out.EmailPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/contact")
@RequiredArgsConstructor
@Tag(name = "Contact", description = "Formulaire de contact public")
public class ContactController {

    private final EmailPort emailPort;

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

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.noContent("Votre message a été envoyé avec succès."));
    }
}
