package com.becommerce.crm.masterdata.contact.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contacts")
public class ContactJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "notes")
    private String notes;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "cpf")
    private String cpf;

    @Column(name = "rg")
    private String rg;

    @Column(name = "rg_issuer")
    private String rgIssuer;

    @Column(name = "gender")
    private String gender;

    @Column(name = "marital_status")
    private String maritalStatus;

    @Column(name = "professional_status")
    private String professionalStatus;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "street")
    private String street;

    @Column(name = "address_number")
    private String addressNumber;

    @Column(name = "address_complement")
    private String addressComplement;

    @Column(name = "neighborhood")
    private String neighborhood;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "health_plan")
    private String healthPlan;

    @Column(name = "health_plan_number")
    private String healthPlanNumber;

    @Column(name = "record_number")
    private String recordNumber;

    @Column(name = "referral_source")
    private String referralSource;

    @Column(name = "has_guardian")
    private boolean hasGuardian;

    @Column(name = "guardian_name")
    private String guardianName;

    @Column(name = "guardian_cpf")
    private String guardianCpf;

    @Column(name = "guardian_phone")
    private String guardianPhone;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getRg() { return rg; }
    public void setRg(String rg) { this.rg = rg; }
    public String getRgIssuer() { return rgIssuer; }
    public void setRgIssuer(String rgIssuer) { this.rgIssuer = rgIssuer; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }
    public String getProfessionalStatus() { return professionalStatus; }
    public void setProfessionalStatus(String professionalStatus) { this.professionalStatus = professionalStatus; }
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getAddressNumber() { return addressNumber; }
    public void setAddressNumber(String addressNumber) { this.addressNumber = addressNumber; }
    public String getAddressComplement() { return addressComplement; }
    public void setAddressComplement(String addressComplement) { this.addressComplement = addressComplement; }
    public String getNeighborhood() { return neighborhood; }
    public void setNeighborhood(String neighborhood) { this.neighborhood = neighborhood; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getHealthPlan() { return healthPlan; }
    public void setHealthPlan(String healthPlan) { this.healthPlan = healthPlan; }
    public String getHealthPlanNumber() { return healthPlanNumber; }
    public void setHealthPlanNumber(String healthPlanNumber) { this.healthPlanNumber = healthPlanNumber; }
    public String getRecordNumber() { return recordNumber; }
    public void setRecordNumber(String recordNumber) { this.recordNumber = recordNumber; }
    public String getReferralSource() { return referralSource; }
    public void setReferralSource(String referralSource) { this.referralSource = referralSource; }
    public boolean isHasGuardian() { return hasGuardian; }
    public void setHasGuardian(boolean hasGuardian) { this.hasGuardian = hasGuardian; }
    public String getGuardianName() { return guardianName; }
    public void setGuardianName(String guardianName) { this.guardianName = guardianName; }
    public String getGuardianCpf() { return guardianCpf; }
    public void setGuardianCpf(String guardianCpf) { this.guardianCpf = guardianCpf; }
    public String getGuardianPhone() { return guardianPhone; }
    public void setGuardianPhone(String guardianPhone) { this.guardianPhone = guardianPhone; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
