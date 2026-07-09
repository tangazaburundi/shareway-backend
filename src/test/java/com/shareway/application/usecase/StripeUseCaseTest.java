package com.shareway.application.usecase;

import com.shareway.domain.exception.BookingNotFoundException;
import com.shareway.domain.exception.InvalidOperationException;
import com.shareway.domain.exception.NotAuthorizedException;
import com.shareway.domain.model.Booking;
import com.shareway.domain.model.Payment;
import com.shareway.domain.model.Trip;
import com.shareway.domain.model.User;
import com.shareway.domain.repository.BookingRepository;
import com.shareway.domain.repository.PaymentRepository;
import com.shareway.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeUseCaseTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private UserRepository userRepository;

    @Captor private ArgumentCaptor<Booking> bookingCaptor;
    @Captor private ArgumentCaptor<Payment> paymentCaptor;

    private StripeUseCase stripeUseCase;

    @BeforeEach
    void setUp() {
        stripeUseCase = new StripeUseCase(bookingRepository, paymentRepository, userRepository);
        ReflectionTestUtils.setField(stripeUseCase, "stripeSecretKey", "sk_test_dummy");
    }

    @Test
    void createPaymentIntent_withNonExistentBooking_shouldThrow() {
        when(bookingRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class,
                () -> stripeUseCase.createPaymentIntent("bad-id", "user-1"));
    }

    @Test
    void createPaymentIntent_withWrongUser_shouldThrow() {
        Booking booking = Booking.builder()
                .id("booking-1")
                .passenger(User.builder().id("other-user").build())
                .build();

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));

        assertThrows(NotAuthorizedException.class,
                () -> stripeUseCase.createPaymentIntent("booking-1", "user-1"));
    }

    @Test
    void createPaymentIntent_withNoAmount_shouldThrow() {
        Booking booking = Booking.builder()
                .id("booking-1")
                .passenger(User.builder().id("user-1").build())
                .amountPaid(null)
                .build();

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));

        assertThrows(InvalidOperationException.class,
                () -> stripeUseCase.createPaymentIntent("booking-1", "user-1"));
    }

    @Test
    void createEscrowPaymentIntent_withMissingBooking_shouldThrow() {
        when(bookingRepository.findById("bad-id")).thenReturn(Optional.empty());
        assertThrows(BookingNotFoundException.class,
                () -> stripeUseCase.createEscrowPaymentIntent("bad-id"));
    }

    @Test
    void createEscrowPaymentIntent_withNoAmount_shouldThrow() {
        Booking booking = Booking.builder()
                .id("booking-1")
                .passenger(User.builder().id("user-1").build())
                .amountPaid(null)
                .build();

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));
        assertThrows(InvalidOperationException.class,
                () -> stripeUseCase.createEscrowPaymentIntent("booking-1"));
    }

    @Test
    void refund_withNoPaymentIntent_shouldThrow() {
        Booking booking = Booking.builder()
                .id("booking-1")
                .stripePaymentIntentId(null)
                .build();

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));

        assertThrows(InvalidOperationException.class,
                () -> stripeUseCase.refund("booking-1", BigDecimal.TEN, "test", "admin-1"));
    }

    @Test
    void convertToSmallestUnit_shouldMultiplyBy100ForEUR() {
        BigDecimal amount = new BigDecimal("15.50");
        long result = ReflectionTestUtils.invokeMethod(stripeUseCase, "convertToSmallestUnit", amount, "EUR");
        assertEquals(1550L, result);
    }

    @Test
    void convertToSmallestUnit_shouldNotMultiplyForFBU() {
        BigDecimal amount = new BigDecimal("5000");
        long result = ReflectionTestUtils.invokeMethod(stripeUseCase, "convertToSmallestUnit", amount, "FBU");
        assertEquals(5000L, result);
    }
}
