package com.becommerce.crm.application.ai.port.input;

import com.becommerce.crm.application.ai.dto.AiChatRequest;
import com.becommerce.crm.application.ai.dto.AiChatResponse;

import java.util.UUID;

/**
 * Caso de uso do assistente de IA (AI-01). {@code companyId} e {@code userId}
 * vêm do {@code CurrentUser} autenticado — nunca do payload do cliente.
 */
public interface AiAssistantUseCase {

    AiChatResponse chat(UUID companyId, UUID userId, AiChatRequest request);
}