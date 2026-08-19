/**
 * Tipos do Assistente de IA (AI-04). Espelham o contrato REST do backend
 * (POST /api/v1/ai/chat + histórico de conversas). O frontend NUNCA envia
 * userId/companyId/permissions/tenantId como autoridade - o backend resolve
 * essas informações do usuário autenticado.
 */

/** Papéis exibidos no chat (o backend também persiste system/tool internamente). */
export type AiChatRole = "user" | "assistant";

/** Tipos de registro que o Context Engine (AI-02) é capaz de resolver. */
export type AiRecordType = "CUSTOMER" | "CONTACT" | "OPPORTUNITY" | "ACTIVITY" | "TASK";

/**
 * Contexto de aplicação enviado ao backend como DICA de onde o usuário está.
 * Nunca carrega identidade/permissões - somente tela, rota e registro em foco.
 */
export type AiContextPayload = {
  screen?: string | null;
  route?: string | null;
  recordType?: AiRecordType | null;
  recordId?: string | null;
};

/** Payload de POST /api/v1/ai/chat. conversationId null cria uma nova conversa. */
export type AiChatRequest = {
  message: string;
  conversationId?: string | null;
  context?: AiContextPayload | null;
};

/** Resposta de POST /api/v1/ai/chat. */
export type AiChatResponse = {
  conversationId: string;
  message: string;
  provider: string;
  /** Propostas de escrita criadas nesta chamada (AI-05). Vazio em mensagens normais. */
  actions?: AiAction[];
};

/** Conversa do assistente (GET /api/v1/ai/conversations). */
export type AiConversation = {
  id: string;
  title: string;
  screen: string | null;
  recordId: string | null;
  createdAt: string;
  updatedAt: string;
};

/** Mensagem de uma conversa (GET /api/v1/ai/conversations/{id}/messages). */
export type AiMessage = {
  id: string;
  conversationId: string;
  role: AiChatRole;
  content: string;
  createdAt: string;
};

/** Estados obrigatórios da UX do chat (AI-04 §12). */
export type AiChatState = "idle" | "sending" | "processing" | "success" | "error";

/** Ciclo de vida de uma acao de escrita do assistente (AI-05). */
export type AiActionStatus =
  "PROPOSED" | "CONFIRMED" | "EXECUTING" | "EXECUTED" | "FAILED" | "CANCELLED";

/**
 * Proposta de escrita do assistente (AI-05). Espelha AiActionResponse do
 * backend. Usada no cartao de confirmacao e na reconstrucao de historico.
 */
export type AiAction = {
  id: string;
  conversationId: string;
  tool: string;
  entityType: string | null;
  entityId: string | null;
  description: string | null;
  status: AiActionStatus;
  parameters: Record<string, unknown>;
  result: unknown;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
};
