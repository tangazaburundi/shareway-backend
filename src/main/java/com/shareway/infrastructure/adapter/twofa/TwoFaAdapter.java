package com.shareway.infrastructure.adapter.twofa;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.shareway.application.port.out.TwoFaPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base32;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.*;
import java.util.Base64;

@Slf4j
@Component
public class TwoFaAdapter implements TwoFaPort {

    @Value("${shareway.two-fa.issuer:Shareway}") private String issuer;

    private final Base32 base32 = new Base32();
    private final TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator();

    @Override
    public String generateSecret() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        return base32.encodeToString(bytes);
    }

    @Override
    public String generateQrCodeUri(String secret, String email, String issuerName) {
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String encodedIssuer = URLEncoder.encode(issuerName != null ? issuerName : issuer, StandardCharsets.UTF_8);
        return "otpauth://totp/%s:%s?secret=%s&issuer=%s&digits=6&period=30"
            .formatted(encodedIssuer, encodedEmail, secret, encodedIssuer);
    }

    @Override
    public boolean verify(String secret, String code) {
        try {
            byte[] decodedKey = base32.decode(secret);
            SecretKey key = new SecretKeySpec(decodedKey, "HmacSHA1");
            Instant now = Instant.now();
            // Allow 1 step tolerance (±30s)
            for (int i = -1; i <= 1; i++) {
                Instant check = now.plusSeconds(i * 30L);
                String expected = String.format("%06d", totp.generateOneTimePassword(key, check));
                if (expected.equals(code)) return true;
            }
            return false;
        } catch (Exception e) {
            log.error("2FA verification error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String generateQrCodeBase64(String uri) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(uri, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", stream);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(stream.toByteArray());
        } catch (Exception e) {
            log.error("QR code generation error: {}", e.getMessage());
            return "";
        }
    }
}
