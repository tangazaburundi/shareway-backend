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
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.Transfer;
import com.stripe.net.Webhook;
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

    @Value("${shareway.stripe.webhook-secret}")
    private String webhookEndpointSecret;

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
     * Webhook Stripe - confirme le paiement côté serveur.
     * <p>
     * Sécurité :
     * 1. La signature Stripe-Signature est vérifiée cryptographiquement
     *    (Webhook.constructEvent). Un payload forgé est rejeté.
     * 2. Le traitement est IDEMPOTENT : un événement reçu plusieurs fois
     *    ne modifie pas l'état plus d'une fois.
     * 3. Seuls les événements relatifs à nos PaymentIntents sont traités.
     */
    public void handleWebhook(String payload, String sigHeader) {
        Stripe.apiKey = stripeSecretKey;

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookEndpointSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook rejected: invalid signature");
            throw new InvalidOperationException("Invalid Stripe signature");
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                PaymentIntent intent = extractPaymentIntent(event);
                if (intent == null) return;
                markPaymentSucceeded(intent);
            }
            case "payment_intent.payment_failed" -> {
                PaymentIntent intent = extractPaymentIntent(event);
                if (intent == null) return;
                markPaymentFailed(intent);
            }
            case "charge.refunded" -> {
                com.stripe.model.Charge charge = extractCharge(event);
                if (charge == null) return;
                markPaymentRefunded(charge);
            }
            default -> log.debug("Stripe webhook ignored event type: {}", event.getType());
        }
    }

    private PaymentIntent extractPaymentIntent(Event event) {
        Object data = event.getData().getObject();
        if (data instanceof PaymentIntent intent) return intent;
        log.warn("Stripe webhook: unexpected payload for {}", event.getType());
        return null;
    }

    private com.stripe.model.Charge extractCharge(Event event) {
        Object data = event.getData().getObject();
        if (data instanceof com.stripe.model.Charge charge) return charge;
        log.warn("Stripe webhook: unexpected payload for {}", event.getType());
        return null;
    }

    private void markPaymentSucceeded(PaymentIntent intent) {
        String intentId = intent.getId();
        paymentRepository.findByStripePaymentIntentId(intentId)
                .filter(p -> p.getStatus() != Payment.PaymentStatus.SUCCEEDED)
                .ifPresent(payment -> {
                    payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
                    paymentRepository.save(payment);
                });

        bookingRepository.findByStripePaymentIntentId(intentId)
                .filter(b -> !"SUCCEEDED".equalsIgnoreCase(b.getStripeStatus()))
                .ifPresent(booking -> {
                    booking.setStripeStatus("SUCCEEDED");
                    bookingRepository.save(booking);
                    log.info("Payment confirmed for booking {}", booking.getId());
                });
    }

    private void markPaymentFailed(PaymentIntent intent) {
        paymentRepository.findByStripePaymentIntentId(intent.getId())
                .filter(p -> p.getStatus() != Payment.PaymentStatus.FAILED)
                .ifPresent(payment -> {
                    payment.setStatus(Payment.PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                });
    }

    private void markPaymentRefunded(com.stripe.model.Charge charge) {
        String intentId = charge.getPaymentIntent();
        if (intentId == null) return;

        Long amountRefunded = charge.getAmountRefunded();
        Long amount = charge.getAmount();

        paymentRepository.findByStripePaymentIntentId(intentId)
                .filter(p -> p.getStatus() != Payment.PaymentStatus.REFUNDED)
                .ifPresent(payment -> {
                    Payment.PaymentStatus newStatus = (amountRefunded != null && amount != null
                            && amountRefunded < amount)
                            ? Payment.PaymentStatus.PARTIALLY_REFUNDED
                            : Payment.PaymentStatus.REFUNDED;
                    payment.setStatus(newStatus);
                    if (amountRefunded != null) {
                        boolean hasCents = !"fbu".equalsIgnoreCase(charge.getCurrency());
                        payment.setRefundAmount(
                                java.math.BigDecimal.valueOf(amountRefunded).movePointLeft(hasCents ? 2 : 0));
                    }
                    paymentRepository.save(payment);
                });
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
        if ("FBU".equals(currency)) {
            return amount.setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();
    }
}
