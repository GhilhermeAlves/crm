package com.becommerce.crm.application.identity.port.output;

/**
 * Porta de saída (driven) para entrega do código OTP (Sprint 7.3).
 *
 * <p>Abstrai o provedor de envio (SMS, e-mail ou mock) do fluxo de
 * verificação por telefone. A implementação concreta é selecionada pela
 * configuração ({@code app.phone-otp.sender}).
 *
 * <p>Invariante do roadmap (7.3): o código OTP nunca deve ser persistido,
 * transmitido ou registrado fora deste canal; cada implementação deve
 * registrar apenas metadados (telefone mascarado), nunca o código em claro
 * em produção.
 */
public interface OtpSender {

    /**
     * Envia o código OTP para o destinatário (telefone E.164).
     *
     * @param phoneE164 telefone em formato E.164
     * @param otpCode   código OTP em claro (6 dígitos)
     */
    void send(String phoneE164, String otpCode);

    /**
     * Nome lógico do canal/implementação (ex.: "console", "twilio", "noop").
     */
    default String name() { return getClass().getSimpleName(); }
}