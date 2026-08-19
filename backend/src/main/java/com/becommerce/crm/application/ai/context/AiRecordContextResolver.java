package com.becommerce.crm.application.ai.context;

import com.becommerce.crm.domain.ai.AiRecordType;

import java.util.UUID;

/**
 * Estratégia de resolução de contexto de um tipo de registro (AI-02). Cada
 * implementação sabe montar o bloco textual de contexto para um
 * {@link AiRecordType} a partir do dado REAL do CRM (nunca duplica regra de
 * negócio; reutiliza repositories/services existentes).
 *
 * <p>O dispatcher ({@code AiContextResolver}) escolhe o resolver pelo tipo e
 * só o executa se o usuário possui {@link #requiredPermission()}.</p>
 */
public interface AiRecordContextResolver {

    /** Tipo de registro que este resolver suporta. */
    AiRecordType type();

    /** Permissão de leitura exigida para expor este dado. */
    String requiredPermission();

    /**
     * Monta o contexto textual do registro, ou {@code null} se não encontrado
     * ou sem acesso. {@code companyId} orienta o RLS/escopo da consulta.
     */
    String resolve(UUID companyId, UUID recordId);
}