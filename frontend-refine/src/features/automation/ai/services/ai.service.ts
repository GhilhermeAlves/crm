import { AxiosError } from "axios";
import api from "@/lib/api";
import type {
  AgentConfig,
  AgentConfigRequest,
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

const CONNECTION_ERROR_MESSAGE = "Falha de conexão. Verifique sua internet e tente novamente.";
const FALLBACK_ERROR_MESSAGE = "Não foi possível obter uma resposta da IA. Tente novamente.";

/**
 * Unifica o mapeamento de erros de IA por status HTTP. Os contexts de chat e
 * análise têm mensagens próprias por status, mas compartilham: a checagem de
 * AxiosError, o fallback de conexão (sem response) e o fallback genérico.
 * Nunca expõe stack traces nem detalhes internos do provedor.
 */
function aiErrorByStatus(
  error: unknown,
  statusMessages: Record<number, string>,
  fallback: string,
): string {
  if (error instanceof AxiosError) {
    const status = error.response?.status;
    if (status !== undefined && statusMessages[status]) {
      return statusMessages[status];
    }
    if (status === undefined || !error.response) {
      return CONNECTION_ERROR_MESSAGE;
    }
  }
  return fallback;
}

/**
 * Converte erros do chat em mensagens amigáveis (AI-04 §22). Nunca expõe
 * stack traces nem detalhes internos do provedor.
 */
export function aiErrorMessage(error: unknown): string {
  return aiErrorByStatus(
    error,
    {
      400: "Não foi possível processar sua mensagem. Verifique e tente novamente.",
      401: "Sua sessão expirou. Faça login novamente.",
      403: "Você não tem permissão para usar o assistente Léo.",
      404: "Conversa não encontrada ou sem acesso.",
      429: "Muitas solicitações em sequência. Aguarde um instante e tente novamente.",
      500: FALLBACK_ERROR_MESSAGE,
      502: "O provedor de IA está indisponível no momento. Tente novamente em instantes.",
    },
    FALLBACK_ERROR_MESSAGE,
  );
}

/**
 * Converte erros da análise contextual em mensagens amigáveis (AI-06 §11).
 * Nunca expõe stack traces nem detalhes internos. Distingue 401/403/404/429/500
 * e erros de conexão; parsing inválido cai no fallback controlado.
 */
export function aiAnalysisErrorMessage(error: unknown): string {
  return aiErrorByStatus(
    error,
    {
      401: "Sua sessão expirou. Faça login novamente.",
      403: "Você não tem permissão para acessar o contexto solicitado.",
      404: "Registro ou contexto não encontrado.",
      429: "Muitas solicitações em sequência. Aguarde um instante e tente novamente.",
      500: "Não foi possível realizar a análise. Tente novamente.",
    },
    "Não foi possível realizar a análise. Tente novamente.",
  );
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

  /** Configuração do agente de IA (GET /ai/agent-config) - Sprint 3-A. */
  async getAgentConfig(): Promise<AgentConfig> {
    const response = await api.get<AgentConfig>("/ai/agent-config");
    return response.data;
  },

  /** Salva a configuração do agente de IA (PUT /ai/agent-config) - Sprint 3-A. */
  async updateAgentConfig(request: AgentConfigRequest): Promise<AgentConfig> {
    const response = await api.put<AgentConfig>("/ai/agent-config", request);
    return response.data;
  },
};
