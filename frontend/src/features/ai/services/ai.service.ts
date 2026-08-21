import { AxiosError } from "axios";
import api from "@/lib/api";
import type {
  AiAction,
  AiAnalysisRequest,
  AiAnalysisResponse,
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
      return "Você não tem permissão para usar o assistente Léo.";
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

/**
 * Converte erros da análise contextual em mensagens amigáveis (AI-06 §11).
 * Nunca expõe stack traces nem detalhes internos. Distingue 401/403/404/429/500
 * e erros de conexão; parsing inválido cai no fallback controlado.
 */
export function aiAnalysisErrorMessage(error: unknown): string {
  if (error instanceof AxiosError) {
    const status = error.response?.status;
    if (status === 401) {
      return "Sua sessão expirou. Faça login novamente.";
    }
    if (status === 403) {
      return "Você não tem permissão para acessar o contexto solicitado.";
    }
    if (status === 404) {
      return "Registro ou contexto não encontrado.";
    }
    if (status === 429) {
      return "Muitas solicitações em sequência. Aguarde um instante e tente novamente.";
    }
    if (status === 500) {
      return "Não foi possível realizar a análise. Tente novamente.";
    }
    if (!error.response) {
      return "Falha de conexão. Verifique sua internet e tente novamente.";
    }
  }
  return "Não foi possível realizar a análise. Tente novamente.";
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

  /** Análise contextual (POST /api/v1/ai/analyze) - AI-06. Envia apenas pergunta
   * + contexto da tela/registro; identidade/permissões ficam com o backend. */
  async analyze(request: AiAnalysisRequest): Promise<AiAnalysisResponse> {
    const response = await api.post<AiAnalysisResponse>("/ai/analyze", request);
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
