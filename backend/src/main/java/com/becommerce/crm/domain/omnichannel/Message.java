package com.becommerce.crm.domain.omnichannel;

/**
 * Mensagem de uma conversa. {@code clientMessageId} é gerado pelo CRM (UUID)
 * e serve de chave de idempotência para envios; {@code externalMessageId}
 * (wamid) é o identificador do provedor e serve de chave de idempotência para
 * recebimento/status.
 */
public class Message {

    private final java.util.UUID id;
    private final java.util.UUID companyId;
    private final java.util.UUID conversationId;
    private final java.util.UUID channelId;
    private final MessageDirection direction;
    private String senderPhone;
    private String recipientPhone;
    private MessageType type;
    private String body;
    private MessageStatus status;
    private String externalMessageId;
    private final java.util.UUID clientMessageId;
    private String providerError;
    private java.time.LocalDateTime sentAt;
    private java.time.LocalDateTime receivedAt;
    private final java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    private Message(java.util.UUID id, java.util.UUID companyId, java.util.UUID conversationId,
                    java.util.UUID channelId, MessageDirection direction, String senderPhone,
                    String recipientPhone, MessageType type, String body, MessageStatus status,
                    String externalMessageId, java.util.UUID clientMessageId, String providerError,
                    java.time.LocalDateTime sentAt, java.time.LocalDateTime receivedAt,
                    java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.conversationId = conversationId;
        this.channelId = channelId;
        this.direction = direction;
        this.senderPhone = senderPhone;
        this.recipientPhone = recipientPhone;
        this.type = type;
        this.body = body;
        this.status = status;
        this.externalMessageId = externalMessageId;
        this.clientMessageId = clientMessageId;
        this.providerError = providerError;
        this.sentAt = sentAt;
        this.receivedAt = receivedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Message createInbound(java.util.UUID companyId, java.util.UUID conversationId,
                                        java.util.UUID channelId, String senderPhone, String recipientPhone,
                                        String body, String externalMessageId) {
        return new Message(java.util.UUID.randomUUID(), companyId, conversationId, channelId,
                MessageDirection.INBOUND, senderPhone, recipientPhone, MessageType.TEXT, body,
                MessageStatus.SENT, externalMessageId, java.util.UUID.randomUUID(), null,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
    }

    public static Message createOutbound(java.util.UUID companyId, java.util.UUID conversationId,
                                         java.util.UUID channelId, String senderPhone, String recipientPhone,
                                         String body, java.util.UUID clientMessageId) {
        return new Message(java.util.UUID.randomUUID(), companyId, conversationId, channelId,
                MessageDirection.OUTBOUND, senderPhone, recipientPhone, MessageType.TEXT, body,
                MessageStatus.PENDING, null, clientMessageId, null, null, null,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
    }

    public static Message reconstitute(java.util.UUID id, java.util.UUID companyId,
                                       java.util.UUID conversationId, java.util.UUID channelId,
                                       MessageDirection direction, String senderPhone, String recipientPhone,
                                       MessageType type, String body, MessageStatus status,
                                       String externalMessageId, java.util.UUID clientMessageId,
                                       String providerError, java.time.LocalDateTime sentAt,
                                       java.time.LocalDateTime receivedAt,
                                       java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {
        return new Message(id, companyId, conversationId, channelId, direction, senderPhone,
                recipientPhone, type, body, status, externalMessageId, clientMessageId, providerError,
                sentAt, receivedAt, createdAt, updatedAt);
    }

    public void markSent(String externalMessageId) {
        this.externalMessageId = externalMessageId;
        this.status = MessageStatus.SENT;
        this.sentAt = java.time.LocalDateTime.now();
        this.updatedAt = java.time.LocalDateTime.now();
    }

    public void markStatus(MessageStatus status, String error) {
        this.status = status;
        if (status == MessageStatus.FAILED) {
            this.providerError = error;
        }
        this.updatedAt = java.time.LocalDateTime.now();
    }

    public java.util.UUID getId() { return id; }
    public java.util.UUID getCompanyId() { return companyId; }
    public java.util.UUID getConversationId() { return conversationId; }
    public java.util.UUID getChannelId() { return channelId; }
    public MessageDirection getDirection() { return direction; }
    public String getSenderPhone() { return senderPhone; }
    public String getRecipientPhone() { return recipientPhone; }
    public MessageType getType() { return type; }
    public String getBody() { return body; }
    public MessageStatus getStatus() { return status; }
    public String getExternalMessageId() { return externalMessageId; }
    public java.util.UUID getClientMessageId() { return clientMessageId; }
    public String getProviderError() { return providerError; }
    public java.time.LocalDateTime getSentAt() { return sentAt; }
    public java.time.LocalDateTime getReceivedAt() { return receivedAt; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
}
