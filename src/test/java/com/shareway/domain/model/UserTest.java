package com.shareway.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private User buildUser() {
        return User.builder()
                .email("john@example.com")
                .passwordHash("hash")
                .firstName("John")
                .lastName("Doe")
                .role(User.UserRole.DRIVER)
                .build();
    }

    @Test
    void shouldLockAfterMaxFailedAttempts() {
        User user = buildUser();
        for (int i = 0; i < 4; i++) {
            user.registerFailedLogin(5, 180);
            assertFalse(user.isLocked(), "ne doit pas être verrouillé avant la 5e tentative");
        }
        user.registerFailedLogin(5, 180);
        assertTrue(user.isLocked());
        assertEquals(5, user.getFailedLoginAttempts());
    }

    @Test
    void shouldNotBeLockedAfterLockExpires() {
        User user = buildUser();
        User expired = User.builder()
                .email("john@example.com").passwordHash("hash")
                .firstName("John").lastName("Doe")
                .failedLoginAttempts(5)
                .lockedUntil(LocalDateTime.now().minusHours(1))
                .lockCount(1)
                .build();
        assertFalse(expired.isLocked());
    }

    @Test
    void shouldAllowLoginAttemptsAgainAfterLockExpires() {
        User user = User.builder()
                .email("john@example.com").passwordHash("hash")
                .firstName("John").lastName("Doe")
                .failedLoginAttempts(5)
                .lockedUntil(LocalDateTime.now().minusHours(1))
                .lockCount(1)
                .build();
        user.registerFailedLogin(5, 180);
        assertFalse(user.isLocked());
        assertEquals(1, user.getFailedLoginAttempts(), "le compteur doit repartir de zéro");
        assertEquals(1, user.getLockCount(), "le nombre de blocages doit être conservé");
    }

    @Test
    void shouldReLockAfterFreshBatchWhenLockExpired() {
        User user = User.builder()
                .email("john@example.com").passwordHash("hash")
                .firstName("John").lastName("Doe")
                .failedLoginAttempts(5)
                .lockedUntil(LocalDateTime.now().minusHours(1))
                .lockCount(1)
                .build();
        for (int i = 0; i < 4; i++) {
            user.registerFailedLogin(5, 180);
            assertFalse(user.isLocked());
        }
        user.registerFailedLogin(5, 180);
        assertTrue(user.isLocked());
        assertEquals(2, user.getLockCount());
    }

    @Test
    void shouldPermanentlyLockOnThirdLockEvent() {
        User user = User.builder()
                .email("john@example.com").passwordHash("hash")
                .firstName("John").lastName("Doe")
                .lockCount(2)
                .failedLoginAttempts(4)
                .build();
        user.registerFailedLogin(5, 180);
        assertTrue(user.isPermanentlyLocked());
        assertTrue(user.isLocked());
        assertNull(user.getLockedUntil());
        assertEquals(3, user.getLockCount());
    }

    @Test
    void shouldNotResetExpiredLockWhenPermanentlyLocked() {
        User user = User.builder()
                .email("john@example.com").passwordHash("hash")
                .firstName("John").lastName("Doe")
                .permanentlyLocked(true)
                .failedLoginAttempts(5)
                .build();
        user.registerFailedLogin(5, 180);
        assertTrue(user.isLocked());
        assertTrue(user.isPermanentlyLocked());
    }

    @Test
    void shouldKeepFailedCountWhileLockStillActive() {
        User user = User.builder()
                .email("john@example.com").passwordHash("hash")
                .firstName("John").lastName("Doe")
                .failedLoginAttempts(5)
                .lockedUntil(LocalDateTime.now().plusHours(1))
                .lockCount(1)
                .build();
        user.registerFailedLogin(5, 180);
        assertEquals(6, user.getFailedLoginAttempts());
        assertTrue(user.isLocked());
    }

    @Test
    void unlockLoginLockShouldClearAllLockoutState() {
        User user = User.builder()
                .email("john@example.com").passwordHash("hash")
                .firstName("John").lastName("Doe")
                .permanentlyLocked(true)
                .lockCount(3)
                .failedLoginAttempts(5)
                .build();
        assertTrue(user.isLocked());
        user.unlockLoginLock();
        assertFalse(user.isLocked());
        assertFalse(user.isPermanentlyLocked());
        assertEquals(0, user.getLockCount());
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());
    }
}
