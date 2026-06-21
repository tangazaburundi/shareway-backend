package com.shareway.application.port.out;
public interface EmailPort {
    void sendVerificationEmail(String to, String firstName, String token);
    void sendPasswordResetEmail(String to, String firstName, String token);
    void sendBookingConfirmation(String to, String firstName, String tripInfo);
    void sendTripCancellation(String to, String firstName, String tripInfo, String reason);
    void sendGeneral(String to, String subject, String body);
}
