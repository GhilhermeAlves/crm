package com.becommerce.crm.domain.omnichannel;

/**
 * Conversa (thread) entre uma Company e um telefone externo, vinculada a um
 * canal. O {@code contactId} referencia a entidade de contato do CRM quando o
 * matching for bem-sucedido (sem duplicar o cadastro de contato).
 */
public class Conversation {

    private final java.util.UUID id;
    private final java.util.UUID companyId;
    private final java.util.UUID channelId;
    private java.util.UUID contactId;
    private final String externalPhone;
    private ConversationStatus status;
    private java.time.LocalDateTime lastMessageAt;
    private int unreadCount;
    private final java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    private Conversation(java.util.UUID id, java.util.UUID companyId, java.util.UUID channelId,
                         java.util.UUID contactId, String externalPhone, ConversationStatus status,
                         java.time.LocalDateTime lastMessageAt, int unreadCount,
                         java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.channelId = channelId;
        this.contactId = contactId;
        this.externalPhone = externalPhone;
        this.status = status;
        this.lastMessageAt = lastMessageAt;
        this.unreadCount = unreadCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Conversation create(java.util.UUID companyId, java.util.UUID channelId,
                                      java.util.UUID contactId, String externalPhone) {
        return new Conversation(java.util.UUID.randomUUID(), companyId, channelId, contactId,
                externalPhone, ConversationStatus.OPEN, null, 0,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
    }

    public static Conversation reconstitute(java.util.UUID id, java.util.UUID companyId,
                                            java.util.UUID channelId, java.util.UUID contactId,
                                            String externalPhone, ConversationStatus status,
                                            java.time.LocalDateTime lastMessageAt, int unreadCount,
                                            java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {
        return new Conversation(id, companyId, channelId, contactId, externalPhone, status,
                lastMessageAt, unreadCount, createdAt, updatedAt);
    }

    public void touch(java.time.LocalDateTime at, boolean inbound) {
        this.lastMessageAt = at;
        if (inbound) {
            this.unreadCount++;
        }
        this.updatedAt = java.time.LocalDateTime.now();
    }

    public void markRead() {
        this.unreadCount = 0;
        this.updatedAt = java.time.LocalDateTime.now();
    }

    public void close() {
        this.status = ConversationStatus.CLOSED;
        this.updatedAt = java.time.LocalDateTime.now();
    }

    public void reopen() {
        this.status = ConversationStatus.OPEN;
        this.updatedAt = java.time.LocalDateTime.now();
    }

    public void assignContact(java.util.UUID contactId) {
        this.contactId = contactId;
        this.updatedAt = java.time.LocalDateTime.now();
    }

    public java.util.UUID getId() { return id; }
    public java.util.UUID getCompanyId() { return companyId; }
    public java.util.UUID getChannelId() { return channelId; }
    public java.util.UUID getContactId() { return contactId; }
    public String getExternalPhone() { return externalPhone; }
    public ConversationStatus getStatus() { return status; }
    public java.time.LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public int getUnreadCount() { return unreadCount; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
}
