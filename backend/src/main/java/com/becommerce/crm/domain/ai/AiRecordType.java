package com.becommerce.crm.domain.ai;

/**
 * Tipos de registro que o Context Engine (AI-02) é capaz de resolver. Mapeia a
 * tela/rota do frontend para o dado de CRM correspondente. O valor é enviado
 * como dica pelo frontend em {@code AiContextPayload.recordType}; o backend
 * decide qual resolver executar (e se o usuário tem permissão de leitura).
 *
 * <p>{@link #CUSTOMER} e {@link #CONTACT} resolvem o mesmo dado subjacente
 * (Customer 360 de um contato); mantemos tipos distintos para que o texto de
 * contexto possa diferenciar a perspectiva ("cliente" vs "contato").</p>
 */
public enum AiRecordType {

    CUSTOMER,
    CONTACT,
    OPPORTUNITY,
    ACTIVITY,
    TASK;

    /**
     * Converte o valor enviado pelo frontend (case-insensitive). Retorna
     * {@code null} para valores vazios ou desconhecidos — nesse caso não há
     * contexto de registro a resolver.
     */
    public static AiRecordType from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return AiRecordType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
