package com.becommerce.crm.masterdata.company.mail;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/** Conta de e-mail (SMTP) da clínica para enviar formulários (V116). */
@Entity
@Table(name = "company_mail_settings")
public class CompanyMailSettingsJpaEntity {

    @Id
    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "email")
    private String email;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "host")
    private String host;

    @Column(name = "port")
    private int port;

    @Column(name = "password_cipher")
    private String passwordCipher;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected CompanyMailSettingsJpaEntity() {
    }

    public CompanyMailSettingsJpaEntity(UUID companyId) {
        this.companyId = companyId;
    }

    public void update(String email, String senderName, String host, int port, String passwordCipher,
                       UUID updatedBy) {
        this.email = email;
        this.senderName = senderName;
        this.host = host;
        this.port = port;
        if (passwordCipher != null) {
            this.passwordCipher = passwordCipher;
        }
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getCompanyId() { return companyId; }
    public String getEmail() { return email; }
    public String getSenderName() { return senderName; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getPasswordCipher() { return passwordCipher; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
