package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.request.LoginRequest;
import com.shareway.application.dto.request.RegisterRequest;
import com.shareway.application.dto.response.ApiResponse;
import com.shareway.application.dto.response.AuthResponse;
import com.shareway.application.usecase.AuthUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Inscription, connexion, 2FA, vérifications")
public class AuthController {

    private final AuthUseCase authUseCase;

    @PostMapping("/register")
    @Operation(summary = "Créer un compte")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(authUseCase.register(req), "Account created successfully"));
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion (supporte 2FA)")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest req, HttpServletRequest httpReq) {
        String ip = httpReq.getRemoteAddr();
        String ua = httpReq.getHeader("User-Agent");
        return ResponseEntity.ok(ApiResponse.ok(authUseCase.login(req, ip, ua)));
    }

    @GetMapping("/verify-email/{token}")
    @Operation(summary = "Vérifier l'email via token")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@PathVariable String token) {
        authUseCase.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.noContent("Email verified successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody java.util.Map<String, String> body) {
        authUseCase.forgotPassword(body.get("email"));
        return ResponseEntity.ok(ApiResponse.noContent("If the email exists, a reset link was sent"));
    }
}
