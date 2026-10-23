package com.becommerce.crm.masterdata.contact.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Contato por empresa (Sprint 8.6). Corresponde à tabela {@code contacts} (V015),
 * já protegida por RLS. {@code deletedAt != null} sinaliza exclusão lógica — não
 * conta para {@code max_contacts}.
 *
 * <p>Ficha do paciente (V097): além dos dados básicos, o contato carrega o
 * cadastro clínico/administrativo (endereço, plano de saúde, nº do prontuário,
 * referência e responsável). {@code recordNumber} é único por empresa quando
 * preenchido.</p>
 */
public class Contact {

    private final UUID id;
    private final UUID companyId;
    private UUID photoStorageId;
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
    private String zipCode;
    private String street;
    private String addressNumber;
    private String addressComplement;
    private String neighborhood;
    private String city;
    private String state;
    private String healthPlan;
    private String healthPlanNumber;
    private String recordNumber;
    private String referralSource;
    private boolean hasGuardian;
    private String guardianName;
    private String guardianCpf;
    private String guardianPhone;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private boolean patient;
    private LocalDate patientSince;

    private Contact(UUID id, UUID companyId, UUID photoStorageId, String firstName, String lastName, String email,
                    String phone, String mobile, String notes, LocalDate birthDate,
                    String cpf, String rg, String rgIssuer, String gender, String maritalStatus,
                    String professionalStatus,
                    String zipCode, String street, String addressNumber, String addressComplement,
                    String neighborhood, String city, String state, String healthPlan,
                    String healthPlanNumber, String recordNumber, String referralSource,
                    boolean hasGuardian, String guardianName, String guardianCpf,
                    String guardianPhone,
                    LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.id = id;
        this.companyId = companyId;
        this.photoStorageId = photoStorageId;
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
        this.zipCode = zipCode;
        this.street = street;
        this.addressNumber = addressNumber;
        this.addressComplement = addressComplement;
        this.neighborhood = neighborhood;
        this.city = city;
        this.state = state;
        this.healthPlan = healthPlan;
        this.healthPlanNumber = healthPlanNumber;
        this.recordNumber = recordNumber;
        this.referralSource = referralSource;
        this.hasGuardian = hasGuardian;
        this.guardianName = guardianName;
        this.guardianCpf = guardianCpf;
        this.guardianPhone = guardianPhone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Contact create(UUID companyId, String firstName, String lastName, String email,
                                 String phone, String mobile, String notes, LocalDate birthDate,
                                 String cpf, String rg, String rgIssuer, String gender,
                                 String maritalStatus, String professionalStatus) {
        LocalDateTime now = LocalDateTime.now();
        return new Contact(UUID.randomUUID(), companyId, null, firstName, lastName, email, phone, mobile,
                notes, birthDate, cpf, rg, rgIssuer, gender, maritalStatus, professionalStatus,
                null, null, null, null, null, null, null, null, null, null, null, false,
                null, null, null,
                now, now, null);
    }

    public static Contact reconstitute(UUID id, UUID companyId, String firstName, String lastName,
                                       String email, String phone, String mobile, String notes,
                                       LocalDate birthDate, String cpf, String rg, String rgIssuer,
                                       String gender, String maritalStatus, String professionalStatus,
                                       LocalDateTime createdAt, LocalDateTime updatedAt,
                                       LocalDateTime deletedAt) {
        return reconstitute(id, companyId, firstName, lastName, email, phone, mobile, notes,
                birthDate, cpf, rg, rgIssuer, gender, maritalStatus, professionalStatus,
                null, null, null, null, null, null, null, null, null, null, null, false,
                null, null, null,
                createdAt, updatedAt, deletedAt, null);
    }

    public static Contact reconstitute(UUID id, UUID companyId, String firstName, String lastName,
                                       String email, String phone, String mobile, String notes,
                                       LocalDate birthDate, String cpf, String rg, String rgIssuer,
                                       String gender, String maritalStatus, String professionalStatus,
                                       LocalDateTime createdAt, LocalDateTime updatedAt,
                                       LocalDateTime deletedAt, UUID photoStorageId) {
        return reconstitute(id, companyId, firstName, lastName, email, phone, mobile, notes,
                birthDate, cpf, rg, rgIssuer, gender, maritalStatus, professionalStatus,
                null, null, null, null, null, null, null, null, null, null, null, false,
                null, null, null,
                createdAt, updatedAt, deletedAt, photoStorageId);
    }

    public static Contact reconstitute(UUID id, UUID companyId, String firstName, String lastName,
                                       String email, String phone, String mobile, String notes,
                                       LocalDate birthDate, String cpf, String rg, String rgIssuer,
                                       String gender, String maritalStatus, String professionalStatus,
                                       String zipCode, String street, String addressNumber,
                                       String addressComplement, String neighborhood, String city,
                                       String state, String healthPlan, String healthPlanNumber,
                                       String recordNumber, String referralSource, boolean hasGuardian,
                                       String guardianName, String guardianCpf, String guardianPhone,
                                       LocalDateTime createdAt, LocalDateTime updatedAt,
                                       LocalDateTime deletedAt) {
        return reconstitute(id, companyId, firstName, lastName, email, phone, mobile, notes,
                birthDate, cpf, rg, rgIssuer, gender, maritalStatus, professionalStatus,
                zipCode, street, addressNumber, addressComplement, neighborhood, city, state,
                healthPlan, healthPlanNumber, recordNumber, referralSource, hasGuardian,
                guardianName, guardianCpf, guardianPhone,
                createdAt, updatedAt, deletedAt, null);
    }

    public static Contact reconstitute(UUID id, UUID companyId, String firstName, String lastName,
                                       String email, String phone, String mobile, String notes,
                                       LocalDate birthDate, String cpf, String rg, String rgIssuer,
                                       String gender, String maritalStatus, String professionalStatus,
                                       String zipCode, String street, String addressNumber,
                                       String addressComplement, String neighborhood, String city,
                                       String state, String healthPlan, String healthPlanNumber,
                                       String recordNumber, String referralSource, boolean hasGuardian,
                                       String guardianName, String guardianCpf, String guardianPhone,
                                       LocalDateTime createdAt, LocalDateTime updatedAt,
                                       LocalDateTime deletedAt, UUID photoStorageId) {
        Contact contact = new Contact(id, companyId, photoStorageId, firstName, lastName, email, phone, mobile, notes,
                birthDate, cpf, rg, rgIssuer, gender, maritalStatus, professionalStatus,
                zipCode, street, addressNumber, addressComplement, neighborhood, city, state,
                healthPlan, healthPlanNumber, recordNumber, referralSource, hasGuardian,
                guardianName, guardianCpf, guardianPhone,
                createdAt, updatedAt, deletedAt);
        return contact;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getPhotoStorageId() { return photoStorageId; }
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
    public String getZipCode() { return zipCode; }
    public String getStreet() { return street; }
    public String getAddressNumber() { return addressNumber; }
    public String getAddressComplement() { return addressComplement; }
    public String getNeighborhood() { return neighborhood; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getHealthPlan() { return healthPlan; }
    public String getHealthPlanNumber() { return healthPlanNumber; }
    public String getRecordNumber() { return recordNumber; }
    public String getReferralSource() { return referralSource; }
    public boolean isHasGuardian() { return hasGuardian; }
    public String getGuardianName() { return guardianName; }
    public String getGuardianCpf() { return guardianCpf; }
    public String getGuardianPhone() { return guardianPhone; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public boolean isActive() { return deletedAt == null; }
    public boolean isPatient() { return patient; }
    public LocalDate getPatientSince() { return patientSince; }

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
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    public void setStreet(String street) { this.street = street; }
    public void setAddressNumber(String addressNumber) { this.addressNumber = addressNumber; }
    public void setAddressComplement(String addressComplement) { this.addressComplement = addressComplement; }
    public void setNeighborhood(String neighborhood) { this.neighborhood = neighborhood; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setHealthPlan(String healthPlan) { this.healthPlan = healthPlan; }
    public void setHealthPlanNumber(String healthPlanNumber) { this.healthPlanNumber = healthPlanNumber; }
    public void setRecordNumber(String recordNumber) { this.recordNumber = recordNumber; }
    public void setReferralSource(String referralSource) { this.referralSource = referralSource; }
    public void setHasGuardian(boolean hasGuardian) { this.hasGuardian = hasGuardian; }
    public void setGuardianName(String guardianName) { this.guardianName = guardianName; }
    public void setGuardianCpf(String guardianCpf) { this.guardianCpf = guardianCpf; }
    public void setGuardianPhone(String guardianPhone) { this.guardianPhone = guardianPhone; }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marca o contato como paciente (módulo Pacientes, V104). Idempotente:
     * quem já é paciente mantém a data original.
     */
    public boolean markAsPatient(LocalDate since) {
        if (patient) {
            return false;
        }
        this.patient = true;
        this.patientSince = since;
        return true;
    }

    /** Restaura a marcação persistida (uso exclusivo da camada de persistência). */
    public void restorePatient(boolean patient, LocalDate patientSince) {
        this.patient = patient;
        this.patientSince = patientSince;
    }

    public void setPhotoStorageId(UUID photoStorageId) { this.photoStorageId = photoStorageId; }

    public void delete() {
        if (deletedAt == null) {
            this.deletedAt = LocalDateTime.now();
        }
    }
}
