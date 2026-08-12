package com.becommerce.crm.domain.contact;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Contato por empresa (Sprint 8.6). Corresponde à tabela {@code contacts} (V015),
 * já protegida por RLS. {@code deletedAt != null} sinaliza exclusão lógica — não
 * conta para {@code max_contacts}.
 */
public class Contact {

    private final UUID id;
    private final UUID companyId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String notes;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private Contact(UUID id, UUID companyId, String firstName, String lastName, String email,
                    String phone, String notes, LocalDateTime createdAt, LocalDateTime updatedAt,
                    LocalDateTime deletedAt) {
        this.id = id;
        this.companyId = companyId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Contact create(UUID companyId, String firstName, String lastName, String email,
                                 String phone, String notes) {
        LocalDateTime now = LocalDateTime.now();
        return new Contact(UUID.randomUUID(), companyId, firstName, lastName, email, phone, notes,
                now, now, null);
    }

    public static Contact reconstitute(UUID id, UUID companyId, String firstName, String lastName,
                                       String email, String phone, String notes,
                                       LocalDateTime createdAt, LocalDateTime updatedAt,
                                       LocalDateTime deletedAt) {
        return new Contact(id, companyId, firstName, lastName, email, phone, notes,
                createdAt, updatedAt, deletedAt);
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public boolean isActive() { return deletedAt == null; }
}