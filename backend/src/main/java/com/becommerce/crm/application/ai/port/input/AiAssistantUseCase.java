package com.becommerce.crm.application.ai.port.input;

import com.becommerce.crm.application.ai.dto.AiChatRequest;
import com.becommerce.crm.application.ai.dto.AiChatResponse;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso do assistente de IA (AI-01/AI-02). {@code companyId}, {@code
 * userId} e {@code permissions} vêm do {@code CurrentUser} autenticado — nunca
 * do payload do cliente. As permissões alimentam o Context Engine para decidir
 * quais dados de contexto expor.
 */
public interface AiAssistantUseCase {

    AiChatResponse chat(UUID companyId, UUID userId, List<String> permissions, AiChatRequest request);
}