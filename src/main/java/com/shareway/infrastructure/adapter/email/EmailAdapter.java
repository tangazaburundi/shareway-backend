package com.shareway.infrastructure.adapter.email;

import com.shareway.application.port.out.EmailPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailAdapter implements EmailPort {

    private final JavaMailSender mailSender;

    @Value("${shareway.app.frontend-url}")
    private String frontendUrl;
    @Value("${app.mail-from:${MAIL_FROM:lemarchebdi@gmail.com}}")
    private String from;
    @Value("${shareway.app.name}")
    private String appName;

    @Override
    @Async
    public void sendVerificationEmail(String to, String firstName, String token) {
        String url = frontendUrl + "/auth/verify-email?token=" + token;
        String html = """
                <h2>Bonjour %s !</h2>
                <p>Merci de vous être inscrit sur <strong>%s</strong>.</p>
                <p>Cliquez sur le lien ci-dessous pour vérifier votre email :</p>
                <a href="%s" style="background:#667eea;color:white;padding:12px 24px;
                   border-radius:6px;text-decoration:none;display:inline-block;">
                  Vérifier mon email
                </a>
                <p>Ce lien expire dans 24 heures.</p>
                """.formatted(firstName, appName, url);
        sendHtml(to, "Vérifiez votre email - " + appName, html);
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String firstName, String token) {
        String url = frontendUrl + "/auth/reset-password?token=" + token;
        String html = """
                <h2>Réinitialisation de mot de passe</h2>
                <p>Bonjour %s, vous avez demandé à réinitialiser votre mot de passe.</p>
                <a href="%s" style="background:#e53e3e;color:white;padding:12px 24px;
                   border-radius:6px;text-decoration:none;display:inline-block;">
                  Réinitialiser mon mot de passe
                </a>
                <p>Ce lien expire dans 1 heure. Si vous n'avez pas fait cette demande, ignorez ce message.</p>
                """.formatted(firstName, url);
        sendHtml(to, "Réinitialisation de mot de passe - " + appName, html);
    }

    @Override
    @Async
    public void sendBookingConfirmation(String to, String firstName, String tripInfo) {
        String html = "<h2>Réservation confirmée !</h2><p>Bonjour %s,</p><p>%s</p>".formatted(firstName, tripInfo);
        sendHtml(to, "Votre réservation est confirmée - " + appName, html);
    }

    @Override
    @Async
    public void sendTripCancellation(String to, String firstName, String tripInfo, String reason) {
        String html = "<h2>Trajet annulé</h2><p>Bonjour %s,</p><p>%s</p><p>Raison : %s</p>"
                .formatted(firstName, tripInfo, reason);
        sendHtml(to, "Trajet annulé - " + appName, html);
    }

    @Override
    @Async
    public void sendGeneral(String to, String subject, String body) {
        sendHtml(to, subject, "<p>" + body + "</p>");
    }

    private void sendHtml(String to, String subject, String htmlBody) {
        try {
            var msg = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(from, appName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(wrapHtml(htmlBody), true);
            mailSender.send(msg);
            log.debug("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            //log.error("Failed to send email to {}: {}", to, e.getMessage());
            log.error("Failed to send email to {}", to, e);
        }
    }

    private String wrapHtml(String content) {
        String logoUrl = frontendUrl + "/assets/images/shareway-logo.jpg";
        String whatsappLink = "https://wa.me/33780739384";
        return """
                <!DOCTYPE html><html><head><meta charset="UTF-8"></head>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;background:#f5f5f5;">
                <div style="background:white;border-radius:8px;overflow:hidden;">
                  <div style="background:#1a8b82;padding:20px;text-align:center;">
                    <img src="%s" alt="%s" style="height:50px;max-width:200px;">
                  </div>
                  <div style="padding:24px;color:#374151;font-size:15px;line-height:1.6;">%s</div>
                  <div style="border-top:1px solid #e5e7eb;padding:20px;text-align:center;background:#f9fafb;">
                    <p style="margin:0 0 8px;color:#6b7280;font-size:13px;">
                      <a href="%s" style="color:#25D366;text-decoration:none;font-weight:bold;">
                        &#128242; WhatsApp : +33 7 80 73 93 84
                      </a>
                    </p>
                    <p style="margin:0 0 8px;color:#6b7280;font-size:13px;">
                      &#9993; <a href="mailto:sharewaybdi&#64;gmail.com" style="color:#1a8b82;text-decoration:none;">sharewaybdi&#64;gmail.com</a>
                    </p>
                    <p style="margin:12px 0 0;color:#9ca3af;font-size:11px;">
                      <a href="%s/mentions-legales" style="color:#9ca3af;text-decoration:none;">Mentions l&#233;gales</a> &middot;
                      <a href="%s/confidentialite" style="color:#9ca3af;text-decoration:none;">Confidentialit&#233;</a> &middot;
                      <a href="%s/cgu" style="color:#9ca3af;text-decoration:none;">CGU</a>
                    </p>
                    <p style="margin:12px 0 0;color:#9ca3af;font-size:12px;">&copy; 2026 %s &mdash; Tous droits r&#233;serv&#233;s</p>
                    <p style="margin:16px 0 0;color:#9ca3af;font-size:11px;font-style:italic;">
                      Si vous n'attendiez pas cet email, ignorez-le.
                    </p>
                  </div>
                </div>
                </body></html>""".formatted(logoUrl, appName, content, whatsappLink, frontendUrl, frontendUrl, frontendUrl, appName);
    }
}
