package com.becommerce.crm.infrastructure.identity.sms;

import com.becommerce.crm.application.identity.port.output.OtpSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Mock local de envio de OTP (Sprint 7.3) — escreve o código no log do
 * console para desenvolvimento e testes E2E, SOMENTE quando o perfil ativo é
 * {@code dev} ou {@code test}.
 *
 * <p>Nunca deve ser selecionado em produção (perfil {@code prod}): o roadmap
 * 7.3 proíbe registrar o OTP em claro fora do canal de entrega real.
 */
@Component
@Profile("dev | test")
public class ConsoleOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleOtpSender.class);

    @Override
    public void send(String phoneE164, String otpCode) {
        log.info("[DEV-ONLY] OTP mock para {} (NÃO UTILIZAR EM PRODUÇÃO) -> {}", mask(phoneE164), otpCode);
    }

    private String mask(String phoneE164) {
        if (phoneE164 == null || phoneE164.length() < 6) {
            return "***";
        }
        return phoneE164.substring(0, 3) + "***" + phoneE164.substring(phoneE164.length() - 2);
    }
}