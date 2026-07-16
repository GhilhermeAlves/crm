package com.becommerce.crm.domain.company;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Company {

    private final UUID id;
    private String legalName;
    private String tradingName;
    private String cnpj;
    private String stateRegistration;
    private String municipalRegistration;
    private String email;
    private String phone;
    private String website;
    private String addressZipCode;
    private String addressStreet;
    private String addressNumber;
    private String addressComplement;
    private String addressNeighborhood;
    private String addressCity;
    private String addressState;
    private String addressCountry;
    private CompanyPlan plan;
    private CompanyStatus status;
    private int maxUsers;
    private int maxStorageMb;
    private String logoUrl;
    private String notes;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Company(
            UUID id,
            String legalName,
            String tradingName,
            String cnpj,
            String stateRegistration,
            String municipalRegistration,
            String email,
            String phone,
            String website,
            String addressZipCode,
            String addressStreet,
            String addressNumber,
            String addressComplement,
            String addressNeighborhood,
            String addressCity,
            String addressState,
            String addressCountry,
            CompanyPlan plan,
            CompanyStatus status,
            int maxUsers,
            int maxStorageMb,
            String logoUrl,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.legalName = legalName;
        this.tradingName = tradingName;
        this.cnpj = cnpj;
        this.stateRegistration = stateRegistration;
        this.municipalRegistration = municipalRegistration;
        this.email = email;
        this.phone = phone;
        this.website = website;
        this.addressZipCode = addressZipCode;
        this.addressStreet = addressStreet;
        this.addressNumber = addressNumber;
        this.addressComplement = addressComplement;
        this.addressNeighborhood = addressNeighborhood;
        this.addressCity = addressCity;
        this.addressState = addressState;
        this.addressCountry = addressCountry;
        this.plan = plan;
        this.status = status;
        this.maxUsers = maxUsers;
        this.maxStorageMb = maxStorageMb;
        this.logoUrl = logoUrl;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Company create(
            String legalName,
            String tradingName,
            String cnpj,
            String stateRegistration,
            String municipalRegistration,
            String email,
            String phone,
            String website,
            String addressZipCode,
            String addressStreet,
            String addressNumber,
            String addressComplement,
            String addressNeighborhood,
            String addressCity,
            String addressState,
            String addressCountry,
            CompanyPlan plan,
            int maxUsers,
            int maxStorageMb,
            String logoUrl,
            String notes
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new Company(
                UUID.randomUUID(),
                legalName, tradingName, cnpj,
                stateRegistration, municipalRegistration,
                email, phone, website,
                addressZipCode, addressStreet, addressNumber,
                addressComplement, addressNeighborhood,
                addressCity, addressState, addressCountry,
                plan, CompanyStatus.ACTIVE,
                maxUsers, maxStorageMb,
                logoUrl, notes,
                now, now
        );
    }

    public static Company reconstitute(
            UUID id,
            String legalName,
            String tradingName,
            String cnpj,
            String stateRegistration,
            String municipalRegistration,
            String email,
            String phone,
            String website,
            String addressZipCode,
            String addressStreet,
            String addressNumber,
            String addressComplement,
            String addressNeighborhood,
            String addressCity,
            String addressState,
            String addressCountry,
            CompanyPlan plan,
            CompanyStatus status,
            int maxUsers,
            int maxStorageMb,
            String logoUrl,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new Company(
                id, legalName, tradingName, cnpj,
                stateRegistration, municipalRegistration,
                email, phone, website,
                addressZipCode, addressStreet, addressNumber,
                addressComplement, addressNeighborhood,
                addressCity, addressState, addressCountry,
                plan, status,
                maxUsers, maxStorageMb,
                logoUrl, notes,
                createdAt, updatedAt
        );
    }

    public void update(
            String legalName,
            String tradingName,
            String email,
            String phone,
            String website,
            String addressZipCode,
            String addressStreet,
            String addressNumber,
            String addressComplement,
            String addressNeighborhood,
            String addressCity,
            String addressState,
            String addressCountry,
            CompanyPlan plan,
            CompanyStatus status,
            int maxUsers,
            int maxStorageMb,
            String logoUrl,
            String notes
    ) {
        this.legalName = legalName;
        this.tradingName = tradingName;
        this.email = email;
        this.phone = phone;
        this.website = website;
        this.addressZipCode = addressZipCode;
        this.addressStreet = addressStreet;
        this.addressNumber = addressNumber;
        this.addressComplement = addressComplement;
        this.addressNeighborhood = addressNeighborhood;
        this.addressCity = addressCity;
        this.addressState = addressState;
        this.addressCountry = addressCountry;
        this.plan = plan;
        this.status = status;
        this.maxUsers = maxUsers;
        this.maxStorageMb = maxStorageMb;
        this.logoUrl = logoUrl;
        this.notes = notes;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getLegalName() { return legalName; }
    public String getTradingName() { return tradingName; }
    public String getCnpj() { return cnpj; }
    public String getStateRegistration() { return stateRegistration; }
    public String getMunicipalRegistration() { return municipalRegistration; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getWebsite() { return website; }
    public String getAddressZipCode() { return addressZipCode; }
    public String getAddressStreet() { return addressStreet; }
    public String getAddressNumber() { return addressNumber; }
    public String getAddressComplement() { return addressComplement; }
    public String getAddressNeighborhood() { return addressNeighborhood; }
    public String getAddressCity() { return addressCity; }
    public String getAddressState() { return addressState; }
    public String getAddressCountry() { return addressCountry; }
    public CompanyPlan getPlan() { return plan; }
    public CompanyStatus getStatus() { return status; }
    public int getMaxUsers() { return maxUsers; }
    public int getMaxStorageMb() { return maxStorageMb; }
    public String getLogoUrl() { return logoUrl; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Company company = (Company) o;
        return Objects.equals(id, company.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
