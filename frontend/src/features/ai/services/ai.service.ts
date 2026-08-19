import { AxiosError } from "axios";
import api from "@/lib/api";
import type {
  AiAction,
  AiChatRequest,
  AiChatResponse,
  AiConversation,
  AiMessage,
} from "../types/ai.types";

export interface AiSuggestionResponse {
  conversationId: string;
  suggestion: string;
  provider: string;
}

/**
 * Converte erros do chat em mensagens amigáveis (AI-04 §22). Nunca expõe
 * stack traces nem detalhes internos do provedor.
 */
export function aiErrorMessage(error: unknown): string {
  if (error instanceof AxiosError) {
    const status = error.response?.status;
    if (status === 401) {
      return "Sua sessão expirou. Faça login novamente.";
    }
    if (status === 403) {
      return "Você não tem permissão para usar o assistente de IA.";
    }
    if (status === 404) {
      return "Conversa não encontrada ou sem acesso.";
    }
    if (status === 400) {
      return "Não foi possível processar sua mensagem. Verifique e tente novamente.";
    }
    if (status === 429) {
      return "Muitas solicitações em sequência. Aguarde um instante e tente novamente.";
    }
    if (status === 502) {
      return "O provedor de IA está indisponível no momento. Tente novamente em instantes.";
    }
    if (status === 500) {
      return "Não foi possível obter uma resposta da IA. Tente novamente.";
    }
    if (!error.response) {
      return "Falha de conexão. Verifique sua internet e tente novamente.";
    }
  }
  return "Não foi possível obter uma resposta da IA. Tente novamente.";
}

export const AiService = {
  async suggest(conversationId: string): Promise<AiSuggestionResponse> {
    const response = await api.get<AiSuggestionResponse>(`/ai/suggestions/${conversationId}`);
    return response.data;
  },

  async chat(request: AiChatRequest): Promise<AiChatResponse> {
    const response = await api.post<AiChatResponse>("/ai/chat", request);
    return response.data;
  },

  async listConversations(): Promise<AiConversation[]> {
    const response = await api.get<AiConversation[]>("/ai/conversations");
    return response.data;
  },

  async getConversationMessages(conversationId: string): Promise<AiMessage[]> {
    const response = await api.get<AiMessage[]>(`/ai/conversations/${conversationId}/messages`);
    return response.data;
  },

  /** Acoes de escrita de uma conversa (GET /ai/conversations/{id}/actions). */
  async listConversationActions(conversationId: string): Promise<AiAction[]> {
    const response = await api.get<AiAction[]>(`/ai/conversations/${conversationId}/actions`);
    return response.data;
  },

  /** Confirma e executa uma proposta (POST /ai/actions/{id}/confirm). */
  async confirmAction(actionId: string): Promise<AiAction> {
    const response = await api.post<AiAction>(`/ai/actions/${actionId}/confirm`);
    return response.data;
  },

  /** Cancela uma proposta (POST /ai/actions/{id}/cancel). */
  async cancelAction(actionId: string): Promise<AiAction> {
    const response = await api.post<AiAction>(`/ai/actions/${actionId}/cancel`);
    return response.data;
  },
};
