// ---------------------------------------------------------------------------
// Domínio omnichannel (Sprint 16) — tipos espelhando as respostas da API.
// ---------------------------------------------------------------------------

export type ChannelType = "WHATSAPP";

export type ChannelProvider = "WHATSAPP_CLOUD_API" | "FAKE";

export type ChannelStatus = "ACTIVE" | "INACTIVE" | "ERROR";

export type ConversationStatus = "OPEN" | "CLOSED";

export type MessageDirection = "INBOUND" | "OUTBOUND";

export type MessageStatus =
  "PENDING" | "SENT" | "DELIVERED" | "READ" | "FAILED";

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

export const MESSAGE_STATUS_LABELS: Record<MessageStatus, string> = {
  PENDING: "Pendente",
  SENT: "Enviada",
  DELIVERED: "Entregue",
  READ: "Lida",
  FAILED: "Falhou",
};
