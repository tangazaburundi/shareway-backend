/*
package com.shareway.infrastructure.web.advice;

import com.shareway.application.dto.response.ApiResponse;
import com.shareway.domain.exception.AccountBlockedException;
import com.shareway.domain.exception.InsufficientSeatsException;
import com.shareway.domain.exception.InvalidOperationException;
import com.shareway.domain.exception.NotAuthorizedException;
import com.shareway.domain.exception.ResourceAlreadyExistsException;
import com.shareway.domain.exception.TripNotFoundException;
import com.shareway.domain.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(UserNotFoundException e) {
        LOGGER.error("NOT FOUND:", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(TripNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTripNotFound(TripNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(NotAuthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(NotAuthorizedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("UNAUTHORIZED", e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("FORBIDDEN", "Access denied"));
    }

    @ExceptionHandler(AccountBlockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleBlocked(AccountBlockedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("ACCOUNT_BLOCKED", e.getMessage()));
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ResourceAlreadyExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("CONFLICT", e.getMessage()));
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOp(InvalidOperationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("INVALID_OPERATION", e.getMessage()));
    }

    @ExceptionHandler(InsufficientSeatsException.class)
    public ResponseEntity<ApiResponse<Void>> handleSeats(InsufficientSeatsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("INSUFFICIENT_SEATS", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage).collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("VALIDATION_ERROR", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handle404(NoResourceFoundException ex) {
        return ResponseEntity.status(404).body("Route API introuvable");
    }
}
*/

package com.shareway.infrastructure.web.advice;

import com.shareway.application.dto.response.ApiResponse;
import com.shareway.infrastructure.adapter.audit.domain.exception.AccountBlockedException;
import com.shareway.infrastructure.adapter.audit.domain.exception.BookingNotFoundException;
import com.shareway.infrastructure.adapter.audit.domain.exception.InsufficientSeatsException;
import com.shareway.infrastructure.adapter.audit.domain.exception.InvalidOperationException;
import com.shareway.infrastructure.adapter.audit.domain.exception.NotAuthorizedException;
import com.shareway.infrastructure.adapter.audit.domain.exception.ResourceAlreadyExistsException;
import com.shareway.infrastructure.adapter.audit.domain.exception.ReviewNotFoundException;
import com.shareway.infrastructure.adapter.audit.domain.exception.TripNotFoundException;
import com.shareway.infrastructure.adapter.audit.domain.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 404 routes inconnues (remplace NoResourceFoundException) ─────────
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("ROUTE_NOT_FOUND",
                        "Route not found: " + e.getMessage()));
    }

    // ── Content-Type non supporté (ex: avatar en JSON au lieu de multipart) ─
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaType(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error("UNSUPPORTED_MEDIA_TYPE",
                        "Content-Type non supporté: " + e.getContentType() +
                                ". Attendu: " + e.getSupportedMediaTypes()));
    }

    // ── Fichier trop volumineux ───────────────────────────────────────────
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxSize(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("FILE_TOO_LARGE", "Le fichier dépasse la taille maximale autorisée"));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(TripNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTripNotFound(TripNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleBookingNotFound(BookingNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleReviewNotFound(ReviewNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(NotAuthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(NotAuthorizedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("UNAUTHORIZED", e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("FORBIDDEN", "Accès refusé"));
    }

    @ExceptionHandler(AccountBlockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleBlocked(AccountBlockedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("ACCOUNT_BLOCKED", e.getMessage()));
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ResourceAlreadyExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("CONFLICT", e.getMessage()));
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOp(InvalidOperationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("INVALID_OPERATION", e.getMessage()));
    }

    @ExceptionHandler(InsufficientSeatsException.class)
    public ResponseEntity<ApiResponse<Void>> handleSeats(InsufficientSeatsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("INSUFFICIENT_SEATS", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage).collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("VALIDATION_ERROR", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "Une erreur inattendue s'est produite"));
    }
}

