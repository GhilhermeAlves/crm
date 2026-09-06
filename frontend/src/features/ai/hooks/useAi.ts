import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";
import { aiErrorMessage, AiService } from "../services/ai.service";
import type {
  AgentConfigRequest,
  AiAction,
  AiAnalysisRequest,
  AiAnalysisResponse,
  AiChatRequest,
  AiChatResponse,
} from "../types/ai.types";

export function useSuggestReply() {
  return useMutation({
    mutationFn: (conversationId: string) => AiService.suggest(conversationId),
    onError: (error: Error) => {
      toast.error(error.message || "Não foi possível gerar a sugestão de resposta.");
    },
  });
}

/**
 * Envia uma mensagem ao assistente (POST /api/v1/ai/chat). Retorna a resposta
 * com o conversationId (novo ou continuado) e a resposta da IA. Não faz
 * streaming nesta milestone (AI-04 §30).
 */
export function useAiChat() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: AiChatRequest) => AiService.chat(request),
    onError: (error: Error) => {
      toast.error(aiErrorMessage(error));
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["ai", "conversations"] });
    },
  });
}

/** Lista as conversas do usuário (GET /api/v1/ai/conversations). */
export function useAiConversations(enabled: boolean) {
  return useQuery({
    queryKey: ["ai", "conversations"],
    queryFn: () => AiService.listConversations(),
    enabled,
  });
}

/**
 * Análise contextual (POST /api/v1/ai/analyze) - AI-06. Não emite toast global:
 * o erro é exposto inline no AiAnalysisCard para controle de UX (§11).
 */
export function useAiAnalyze() {
  return useMutation({
    mutationFn: (request: AiAnalysisRequest) => AiService.analyze(request),
  });
}

/** Mensagens de uma conversa (GET /api/v1/ai/conversations/{id}/messages). */
export function useAiConversationMessages(conversationId: string | null, enabled: boolean) {
  return useQuery({
    queryKey: ["ai", "conversations", conversationId, "messages"],
    queryFn: () => AiService.getConversationMessages(conversationId as string),
    enabled: enabled && !!conversationId,
  });
}

/** Acoes de escrita de uma conversa (GET /api/v1/ai/conversations/{id}/actions). */
export function useAiConversationActions(conversationId: string | null, enabled: boolean) {
  return useQuery({
    queryKey: ["ai", "conversations", conversationId, "actions"],
    queryFn: () => AiService.listConversationActions(conversationId as string),
    enabled: enabled && !!conversationId,
  });
}

/** Confirma e executa uma proposta (POST /api/v1/ai/actions/{id}/confirm). */
export function useAiConfirmAction(conversationId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (actionId: string) => AiService.confirmAction(actionId),
    onError: (error: Error) => {
      toast.error(aiErrorMessage(error));
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["ai", "conversations", conversationId, "actions"],
      });
    },
  });
}

/** Cancela uma proposta (POST /api/v1/ai/actions/{id}/cancel). */
export function useAiCancelAction(conversationId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (actionId: string) => AiService.cancelAction(actionId),
    onError: (error: Error) => {
      toast.error(aiErrorMessage(error));
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["ai", "conversations", conversationId, "actions"],
      });
    },
  });
}

export type { AiAction };

export function useAiPermissions() {
  const { can } = useAuthorization();
  return {
    canSuggest: can("ai:suggest"),
    canChat: can("ai:chat"),
    canManageAgentConfig: can("ai:agent-config"),
  };
}

/**
 * Configuração do agente de IA (GET /api/v1/ai/agent-config) - Sprint 3-A.
 * Desabilitada para quem não tem a permissão {@code ai:agent-config}.
 */
export function useAgentConfig(enabled: boolean) {
  return useQuery({
    queryKey: ["ai", "agent-config"],
    queryFn: () => AiService.getAgentConfig(),
    enabled,
  });
}

/** Salva a configuração do agente de IA (PUT /api/v1/ai/agent-config) - Sprint 3-A. */
export function useUpdateAgentConfig() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: AgentConfigRequest) => AiService.updateAgentConfig(request),
    onError: (error: Error) => {
      toast.error(aiErrorMessage(error));
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["ai", "agent-config"] });
      toast.success("Configuração do agente de IA salva.");
    },
  });
}

export type { AiChatRequest, AiChatResponse, AiAnalysisRequest, AiAnalysisResponse };
