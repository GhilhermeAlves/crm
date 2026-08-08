package com.becommerce.crm.infrastructure.identity.sms;

import com.becommerce.crm.application.identity.port.output.OtpSender;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fallback de produção (Sprint 7.0) — NÃO envia o OTP (nenhum provedor de
 * SMS real está configurado) e NUNCA registra o código em claro.
 *
 * <p>Relacionado ao roadmap 7.3: "Nenhum OTP em log; nunca logar o código".
 * Quando um provedor real SMS for integrado, substituir esta implementação por
 * um adapter (ex.: Twilio) ativo no perfil {@code prod}.
 */
@Component
@Profile("prod")
public class DisabledOtpSender implements OtpSender {

    @Override
    public void send(String phoneE164, String otpCode) {
        // Canal de entrega não configurado em produção — nenhuma ação e nenhum log.
    }

    @Override
    public String name() { return "noop"; }
}