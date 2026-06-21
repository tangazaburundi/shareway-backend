package com.shareway.application.port.out;
public interface TwoFaPort {
    String generateSecret();
    String generateQrCodeUri(String secret, String email, String issuer);
    boolean verify(String secret, String code);
    String generateQrCodeBase64(String uri);
}
