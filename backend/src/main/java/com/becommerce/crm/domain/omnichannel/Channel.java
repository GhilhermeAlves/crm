package com.becommerce.crm.domain.omnichannel;

/**
 * Canal de comunicação pertencente a uma Company (ex.: WhatsApp).
 *
 * <p>Segurança: esta entidade armazena apenas {@link #secretsRef}, uma
 * referência a um secret externo (nome no cofre/ambiente) — nunca o valor
 * do token. Configuração operacional (não sensível) fica em {@link #config}.
 */
public class Channel {

    private final java.util.UUID id;
    private final java.util.UUID companyId;
    private final ChannelType type;
    private final ChannelProvider provider;
    private String name;
    private ChannelStatus status;
    private String externalId;
    private String config;
    private String secretsRef;
    private final java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    private Channel(java.util.UUID id, java.util.UUID companyId, ChannelType type,
                    ChannelProvider provider, String name, ChannelStatus status,
                    String externalId, String config, String secretsRef,
                    java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.type = type;
        this.provider = provider;
        this.name = name;
        this.status = status;
        this.externalId = externalId;
        this.config = config;
        this.secretsRef = secretsRef;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Channel create(java.util.UUID companyId, ChannelType type, ChannelProvider provider,
                                 String name, String externalId, String config, String secretsRef) {
        return new Channel(java.util.UUID.randomUUID(), companyId, type, provider, name,
                ChannelStatus.ACTIVE, externalId, config, secretsRef,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
    }

    public static Channel reconstitute(java.util.UUID id, java.util.UUID companyId, ChannelType type,
                                       ChannelProvider provider, String name, ChannelStatus status,
                                       String externalId, String config, String secretsRef,
                                       java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {
        return new Channel(id, companyId, type, provider, name, status, externalId, config,
                secretsRef, createdAt, updatedAt);
    }

    public void update(String name, ChannelStatus status, String externalId, String config, String secretsRef) {
        this.name = name;
        this.status = status;
        this.externalId = externalId;
        this.config = config;
        this.secretsRef = secretsRef;
        this.updatedAt = java.time.LocalDateTime.now();
    }

    public java.util.UUID getId() { return id; }
    public java.util.UUID getCompanyId() { return companyId; }
    public ChannelType getType() { return type; }
    public ChannelProvider getProvider() { return provider; }
    public String getName() { return name; }
    public ChannelStatus getStatus() { return status; }
    public String getExternalId() { return externalId; }
    public String getConfig() { return config; }
    public String getSecretsRef() { return secretsRef; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
}
