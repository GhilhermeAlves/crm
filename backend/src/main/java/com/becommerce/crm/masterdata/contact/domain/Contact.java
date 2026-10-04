package com.becommerce.crm.masterdata.contact.domain;

import java.time.LocalDate;
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
    private String mobile;
    private String notes;
    private LocalDate birthDate;
    private String cpf;
    private String rg;
    private String rgIssuer;
    private String gender;
    private String maritalStatus;
    private String professionalStatus;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private Contact(UUID id, UUID companyId, String firstName, String lastName, String email,
                    String phone, String mobile, String notes, LocalDate birthDate,
                    String cpf, String rg, String rgIssuer, String gender, String maritalStatus,
                    String professionalStatus,
                    LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.id = id;
        this.companyId = companyId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.mobile = mobile;
        this.notes = notes;
        this.birthDate = birthDate;
        this.cpf = cpf;
        this.rg = rg;
        this.rgIssuer = rgIssuer;
        this.gender = gender;
        this.maritalStatus = maritalStatus;
        this.professionalStatus = professionalStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Contact create(UUID companyId, String firstName, String lastName, String email,
                                 String phone, String mobile, String notes, LocalDate birthDate,
                                 String cpf, String rg, String rgIssuer, String gender,
                                 String maritalStatus, String professionalStatus) {
        LocalDateTime now = LocalDateTime.now();
        return new Contact(UUID.randomUUID(), companyId, firstName, lastName, email, phone, mobile,
                notes, birthDate, cpf, rg, rgIssuer, gender, maritalStatus, professionalStatus,
                now, now, null);
    }

    public static Contact reconstitute(UUID id, UUID companyId, String firstName, String lastName,
                                       String email, String phone, String mobile, String notes,
                                       LocalDate birthDate, String cpf, String rg, String rgIssuer,
                                       String gender, String maritalStatus, String professionalStatus,
                                       LocalDateTime createdAt, LocalDateTime updatedAt,
                                       LocalDateTime deletedAt) {
        return new Contact(id, companyId, firstName, lastName, email, phone, mobile, notes,
                birthDate, cpf, rg, rgIssuer, gender, maritalStatus, professionalStatus,
                createdAt, updatedAt, deletedAt);
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getMobile() { return mobile; }
    public String getNotes() { return notes; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getCpf() { return cpf; }
    public String getRg() { return rg; }
    public String getRgIssuer() { return rgIssuer; }
    public String getGender() { return gender; }
    public String getMaritalStatus() { return maritalStatus; }
    public String getProfessionalStatus() { return professionalStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public boolean isActive() { return deletedAt == null; }

    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public void setRg(String rg) { this.rg = rg; }
    public void setRgIssuer(String rgIssuer) { this.rgIssuer = rgIssuer; }
    public void setGender(String gender) { this.gender = gender; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }
    public void setProfessionalStatus(String professionalStatus) { this.professionalStatus = professionalStatus; }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        if (deletedAt == null) {
            this.deletedAt = LocalDateTime.now();
        }
    }
}