package com.becommerce.crm.infrastructure.identity.email;

import com.becommerce.crm.application.identity.port.output.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Provedor de e-mail de desenvolvimento: NUNCA envia de verdade, apenas loga.
 *
 * <p>O token de reset é segredo: por padrão o serviço loga apenas o destinatário
 * e um placeholder, sem revelar o token. Em desenvolvimento/teste o token pode
 * ser exibido explicitamente para viabilizar o E2E, ativando o flag
 * {@code app.mail.log-token} (env {@code MAIL_LOG_TOKEN=true}). Sem o flag
 * explícito o token nunca é impresso.
 */
@Service
public class FakeEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(FakeEmailService.class);

    private final boolean logToken;

    public FakeEmailService(@Value("${app.mail.log-token:false}") boolean logToken) {
        this.logToken = logToken;
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        log.info("=== FAKE EMAIL SERVICE (dev/test) ===");
        if (logToken) {
            log.info("Sending password reset email to: {}", to);
            log.info("Reset token: {}", token);
            log.info("Reset link: http://localhost:3000/reset-password?token={}", token);
        } else {
            log.info("Sending password reset email to: {} (token oculto; ative app.mail.log-token para expor em dev/test)", to);
        }
        log.info("=== END FAKE EMAIL ===");
    }
}
