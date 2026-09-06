// ---------------------------------------------------------------------------
// Domínio omnichannel (Sprint 16) — tipos espelhando as respostas da API.
// ---------------------------------------------------------------------------

export type ChannelType = "WHATSAPP";

export type ChannelProvider = "WHATSAPP_CLOUD_API" | "FAKE";

export type ChannelStatus = "ACTIVE" | "INACTIVE" | "ERROR";

export type ConversationStatus = "OPEN" | "CLOSED";

/** Modo de atendimento (Sprint 3 - Human Takeover): AUTOMATIC = IA autônoma
 * habilitada; HUMAN = um humano assumiu e a IA está suspensa. */
export type ConversationMode = "AUTOMATIC" | "HUMAN";

export type MessageDirection = "INBOUND" | "OUTBOUND";

export type MessageStatus = "PENDING" | "SENT" | "DELIVERED" | "READ" | "FAILED";

export type MessageType = "TEXT";

export type Channel = {
  id: string;
  companyId: string;
  type: ChannelType;
  provider: ChannelProvider;
  name: string;
  status: ChannelStatus;
  externalId: string | null;
  config: string | null;
  secretsRef: string | null;
  createdAt: string;
  updatedAt: string;
};

export type Conversation = {
  id: string;
  channelId: string;
  contactId: string | null;
  externalPhone: string;
  status: ConversationStatus;
  mode: ConversationMode;
  lastMessageAt: string | null;
  lastMessage: string | null;
  unreadCount: number;
  createdAt: string;
};

export type Message = {
  id: string;
  conversationId: string;
  direction: MessageDirection;
  senderPhone: string | null;
  recipientPhone: string | null;
  type: MessageType;
  body: string;
  status: MessageStatus;
  externalMessageId: string | null;
  providerError: string | null;
  sentAt: string | null;
  createdAt: string;
};

export type ConversationDetail = {
  id: string;
  channelId: string;
  contactId: string | null;
  externalPhone: string;
  status: ConversationStatus;
  mode: ConversationMode;
  lastMessageAt: string | null;
  unreadCount: number;
  messages: Page<Message>;
};

export type Page<T> = {
  content: T[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
};

export type ChannelRequest = {
  name: string;
  type: ChannelType;
  provider: ChannelProvider;
  externalId?: string;
  config?: string;
  secretsRef?: string;
  status?: ChannelStatus;
};

// ---------------------------------------------------------------------------
// Rótulos (UI em pt-BR)
// ---------------------------------------------------------------------------

export const CHANNEL_STATUS_LABELS: Record<ChannelStatus, string> = {
  ACTIVE: "Ativo",
  INACTIVE: "Inativo",
  ERROR: "Erro",
};

export const CHANNEL_PROVIDER_LABELS: Record<ChannelProvider, string> = {
  WHATSAPP_CLOUD_API: "WhatsApp Cloud API",
  FAKE: "Fake (desenvolvimento)",
};

export const CONVERSATION_STATUS_LABELS: Record<ConversationStatus, string> = {
  OPEN: "Aberta",
  CLOSED: "Fechada",
};

export const CONVERSATION_MODE_LABELS: Record<ConversationMode, string> = {
  AUTOMATIC: "IA autônoma",
  HUMAN: "Atendimento humano",
};

export const MESSAGE_STATUS_LABELS: Record<MessageStatus, string> = {
  PENDING: "Pendente",
  SENT: "Enviada",
  DELIVERED: "Entregue",
  READ: "Lida",
  FAILED: "Falhou",
};

// ---------------------------------------------------------------------------
// Follow-ups (Sprint 4 - automação de retorno no WhatsApp)
// ---------------------------------------------------------------------------

export type FollowUpStatus = "PENDING" | "PROCESSING" | "SENT" | "CANCELLED" | "FAILED";

export type FollowUpAction = "SEND_MESSAGE";

export type FollowUpCancellationReason =
  | "USER"
  | "HUMAN_MODE"
  | "SUPERSEDED_BY_NEW_MESSAGE";

export type FollowUp = {
  id: string;
  conversationId: string;
  status: FollowUpStatus;
  actionType: FollowUpAction;
  actionContent: string | null;
  executeAt: string;
  attempts: number;
  lastError: string | null;
  resultText: string | null;
  cancelledAt: string | null;
  cancelledReason: FollowUpCancellationReason | null;
  createdAt: string;
  updatedAt: string;
};

export type FollowUpRequest = {
  conversationId: string;
  content: string;
  executeAt: string;
  idempotencyKey?: string;
};

export const FOLLOW_UP_STATUS_LABELS: Record<FollowUpStatus, string> = {
  PENDING: "Pendente",
  PROCESSING: "Processando",
  SENT: "Enviado",
  CANCELLED: "Cancelado",
  FAILED: "Falhou",
};
