package com.shareway.application.usecase;

import com.shareway.domain.exception.BookingNotFoundException;
import com.shareway.domain.exception.InvalidOperationException;
import com.shareway.domain.exception.NotAuthorizedException;
import com.shareway.domain.model.Booking;
import com.shareway.domain.model.Payment;
import com.shareway.domain.repository.BookingRepository;
import com.shareway.domain.repository.PaymentRepository;
import com.shareway.domain.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.Transfer;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.TransferCreateParams;
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
     * Crée un PaymentIntent avec capture manuelle (escrow).
     * Le paiement est autorisé mais pas capturé immédiatement.
     */
    public String createEscrowPaymentIntent(String bookingId) {
        Stripe.apiKey = stripeSecretKey;

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));

        if (booking.getAmountPaid() == null)
            throw new InvalidOperationException("Booking amount not set");

        try {
            long amount = convertToSmallestUnit(booking.getAmountPaid(), booking.getCurrency().name());
            String currency = booking.getCurrency().name().toLowerCase();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amount)
                    .setCurrency(currency)
                    .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                    .putMetadata("bookingId", bookingId)
                    .putMetadata("userId", booking.getPassenger().getId())
                    .putMetadata("tripId", booking.getTrip().getId())
                    .setDescription("Shareway - Escrow for trip to " + booking.getTrip().getArrivalCity())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true).build())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            booking.setPaymentIntentId(intent.getId());
            booking.setStripeStatus("REQUIRES_CAPTURE");
            bookingRepository.save(booking);

            Payment payment = Payment.builder()
                    .booking(booking)
                    .user(booking.getPassenger())
                    .amount(booking.getAmountPaid())
                    .currency(Payment.PaymentCurrency.valueOf(booking.getCurrency().name()))
                    .stripePaymentIntentId(intent.getId())
                    .status(Payment.PaymentStatus.PROCESSING)
                    .build();
            paymentRepository.save(payment);

            log.info("Escrow PaymentIntent {} created for booking {}", intent.getId(), bookingId);
            return intent.getId();

        } catch (com.stripe.exception.StripeException e) {
            log.error("Stripe error creating escrow payment intent: {}", e.getMessage());
            throw new InvalidOperationException("Escrow payment initialization failed: " + e.getMessage());
        }
    }

    /**
     * Capture un PaymentIntent (escrow -> fonds débloqués au conducteur).
     */
    public void capturePayment(String paymentIntentId) {
        Stripe.apiKey = stripeSecretKey;

        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            PaymentIntentCaptureParams params = PaymentIntentCaptureParams.builder().build();
            intent.capture(params);
            log.info("PaymentIntent {} captured successfully", paymentIntentId);
        } catch (com.stripe.exception.StripeException e) {
            log.error("Stripe error capturing payment intent: {}", e.getMessage());
            throw new InvalidOperationException("Payment capture failed: " + e.getMessage());
        }
    }

    /**
     * Annule/libère un PaymentIntent (escrow -> passager remboursé).
     */
    public void cancelPayment(String paymentIntentId) {
        Stripe.apiKey = stripeSecretKey;

        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            intent.cancel();
            log.info("PaymentIntent {} cancelled/released", paymentIntentId);
        } catch (com.stripe.exception.StripeException e) {
            log.error("Stripe error cancelling payment intent: {}", e.getMessage());
            throw new InvalidOperationException("Payment cancellation failed: " + e.getMessage());
        }
    }

    /**
     * Transfère les fonds du compte plateforme vers le compte Stripe du conducteur.
     */
    public void createTransfer(String paymentIntentId, String driverStripeAccountId, BigDecimal amount) {
        Stripe.apiKey = stripeSecretKey;

        try {
            TransferCreateParams params = TransferCreateParams.builder()
                    .setAmount(convertToSmallestUnit(amount, "EUR"))
                    .setCurrency("eur")
                    .setDestination(driverStripeAccountId)
                    .setTransferGroup(paymentIntentId)
                    .build();

            Transfer transfer = Transfer.create(params);
            log.info("Transfer {} created for driver account {}", transfer.getId(), driverStripeAccountId);
        } catch (com.stripe.exception.StripeException e) {
            log.error("Stripe error creating transfer: {}", e.getMessage());
            throw new InvalidOperationException("Transfer failed: " + e.getMessage());
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
