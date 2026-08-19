package com.becommerce.crm.domain.ai;

/**
 * Ciclo de vida de uma acao de escrita proposta pelo assistente de IA (AI-05).
 * Uma proposta comeca como {@code PROPOSED} (criada durante o chat, sem efeito
 * de escrita), aguardando confirmacao explicita do usuario. A confirmacao
 * executa a acao ({@code EXECUTING}) e termina em {@code EXECUTED} ou
 * {@code FAILED}. {@code CANCELLED} e terminal (usuario recusou a proposta).
 *
 * <p>Os estados terminais ({@code EXECUTED}, {@code FAILED},
 * {@code CANCELLED}) sao idempotentes: confirmar novamente nao reexecuta.</p>
 */
public enum AiActionStatus {
    PROPOSED,
    CONFIRMED,
    EXECUTING,
    EXECUTED,
    FAILED,
    CANCELLED
}