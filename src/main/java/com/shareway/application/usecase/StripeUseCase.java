package com.shareway.application.usecase;

import com.shareway.infrastructure.adapter.audit.domain.exception.BookingNotFoundException;
import com.shareway.infrastructure.adapter.audit.domain.exception.InvalidOperationException;
import com.shareway.infrastructure.adapter.audit.domain.exception.NotAuthorizedException;
import com.shareway.infrastructure.adapter.audit.domain.model.Booking;
import com.shareway.infrastructure.adapter.audit.domain.model.Payment;
import com.shareway.infrastructure.adapter.audit.domain.repository.BookingRepository;
import com.shareway.infrastructure.adapter.audit.domain.repository.PaymentRepository;
import com.shareway.infrastructure.adapter.audit.domain.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StripeUseCase {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @Value("${shareway.stripe.secret-key}")
    private String stripeSecretKey;

    /**
     * Crée un PaymentIntent Stripe pour une réservation.
     * Retourne le client_secret à envoyer au frontend.
     */
    public Map<String, String> createPaymentIntent(String bookingId, String userId) {
        Stripe.apiKey = stripeSecretKey;

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));

        if (!booking.getPassenger().getId().equals(userId))
            throw new NotAuthorizedException("Not your booking");

        if (booking.getAmountPaid() == null)
            throw new InvalidOperationException("Booking amount not set");

        try {
            // Convert to smallest currency unit (centimes for EUR, no cents for FBU)
            long amount = convertToSmallestUnit(booking.getAmountPaid(), booking.getCurrency().name());

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amount)
                    .setCurrency(booking.getCurrency().name().toLowerCase())
                    .putMetadata("bookingId", bookingId)
                    .putMetadata("userId", userId)
                    .putMetadata("tripId", booking.getTrip().getId())
                    .setDescription("Shareway - Trip to " + booking.getTrip().getArrivalCity())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true).build())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            // Update booking with payment intent
            booking.setStripePaymentIntentId(intent.getId());
            booking.setStripeStatus("PENDING");
            bookingRepository.save(booking);

            // Create payment record
            Payment payment = Payment.builder()
                    .booking(booking)
                    .user(booking.getPassenger())
                    .amount(booking.getAmountPaid())
                    .currency(Payment.PaymentCurrency.valueOf(booking.getCurrency().name()))
                    .stripePaymentIntentId(intent.getId())
                    .status(Payment.PaymentStatus.PENDING)
                    .build();
            paymentRepository.save(payment);

            Map<String, String> result = new HashMap<>();
            result.put("clientSecret", intent.getClientSecret());
            result.put("paymentIntentId", intent.getId());
            result.put("amount", booking.getAmountPaid().toString());
            result.put("currency", booking.getCurrency().name());
            return result;

        } catch (com.stripe.exception.StripeException e) {
            log.error("Stripe error creating payment intent: {}", e.getMessage());
            throw new InvalidOperationException("Payment initialization failed: " + e.getMessage());
        }
    }

    /**
     * Webhook Stripe - confirme le paiement côté serveur
     */
    public void handleWebhook(String payload, String sigHeader) {
        Stripe.apiKey = stripeSecretKey;
        // In production: verify signature with Stripe.webhookEndpointSecret
        // This is a simplified version
        log.info("Stripe webhook received (signature check should be added in production)");
    }

    /**
     * Remboursement partiel ou total
     */
    public void refund(String bookingId, BigDecimal amount, String reason, String adminId) {
        Stripe.apiKey = stripeSecretKey;

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));

        if (booking.getStripePaymentIntentId() == null)
            throw new InvalidOperationException("No Stripe payment for this booking");

        try {
            long refundAmount = convertToSmallestUnit(amount, booking.getCurrency().name());

            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(booking.getStripePaymentIntentId())
                    .setAmount(refundAmount)
                    .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                    .build();

            Refund refund = Refund.create(params);
            log.info("Refund {} created for booking {}: {}", refund.getId(), bookingId, refund.getStatus());

        } catch (com.stripe.exception.StripeException e) {
            log.error("Stripe refund error: {}", e.getMessage());
            throw new InvalidOperationException("Refund failed: " + e.getMessage());
        }
    }

    private long convertToSmallestUnit(BigDecimal amount, String currency) {
        // FBU has no cents, EUR/USD multiply by 100
        if ("FBU".equals(currency)) return amount.longValue();
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }
}
