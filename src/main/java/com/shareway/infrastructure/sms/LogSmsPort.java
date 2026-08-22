package com.shareway.infrastructure.sms;

import com.shareway.application.port.out.SmsPort;
import com.shareway.domain.repository.SmsConfigRepository;
import com.shareway.domain.model.SmsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogSmsPort implements SmsPort {

    private final SmsConfigRepository smsConfigRepository;

    @Override
    public void sendSms(String to, String message) {
        SmsConfig config = smsConfigRepository.findTopByOrderByIdAsc().orElse(null);

        if (config == null || !config.isEnabled() || config.getProvider() == SmsConfig.SmsProvider.DISABLED) {
            log.warn("[SMS-NOOP] SMS disabled — to={}, message={}", to, message);
            return;
        }

        log.info("[SMS-{}] to={}\n{}", config.getProvider(), to, message);

        // Placeholder: replace this log-only implementation with real SDK when provider is configured.
        // - TwilioRestClient for TWILIO
        // - ATSender for AFRICAS_TALKING
    }

    @Override
    public boolean isAvailable() {
        return smsConfigRepository.findTopByOrderByIdAsc()
                .map(c -> c.isEnabled() && c.getProvider() != SmsConfig.SmsProvider.DISABLED)
                .orElse(false);
    }
}
