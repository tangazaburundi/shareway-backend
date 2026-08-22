package com.shareway.application.port.out;

public interface SmsPort {

    void sendSms(String to, String message);

    boolean isAvailable();
}
