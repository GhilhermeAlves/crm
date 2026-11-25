package com.becommerce.crm.masterdata.person;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Contratos da API do cadastro de pessoas (doc seções 9 e 22). */
public final class PersonDtos {

    private PersonDtos() {}

    public record PageResult<T>(List<T> items, long total, int page, int size) {}

    // ---------------------------------------------------------------- pessoa

    public record PersonSummary(UUID id, String personType, String name, String tradeName, String taxId,
                                boolean provisional, boolean active, List<String> roles, String mobile,
                                String email, String city, String uf, OffsetDateTime createdAt) {}

    public record PersonDetail(UUID id, String personType, String name, String tradeName, String taxId,
                               String rgOrIe, String rgIssuer, LocalDate birthOrFoundationDate, String gender,
                               String maritalStatus, String professionalStatus, String nationality,
                               Integer countryId, boolean provisional, boolean active, boolean deleted,
                               String deleteReason, String notes, String avatarUrl, boolean hasContact,
                               boolean healthSegment, List<String> roles, List<Address> addresses,
                               List<ContactChannel> contacts, List<Document> documents,
                               List<Relationship> relationships, CustomerInfo customer, SupplierInfo supplier,
                               EmployeeInfo employee, long openIssues, OffsetDateTime createdAt,
                               OffsetDateTime updatedAt) {}

    public record PersonRequest(String personType, String name, String tradeName, String taxId, String rgOrIe,
                                String rgIssuer, LocalDate birthOrFoundationDate, String gender,
                                String maritalStatus, String professionalStatus, String nationality,
                                Integer countryId, Boolean provisional, String notes,
                                /* somente na inclusão */
                                List<String> roles, AddressRequest address, String mobile, String email,
                                CustomerRequest customer, SupplierRequest supplier, EmployeeRequest employee) {}

    public record StatusRequest(boolean active) {}

    public record DeleteRequest(String reason) {}

    public record Address(UUID id, String addressType, String postalCode, String street, String addressNumber,
                          String complement, UUID neighborhoodId, String neighborhoodName, Integer cityId,
                          String cityName, String uf, Integer countryId, String foreignRegion, boolean primary,
                          boolean active) {}

    public record AddressRequest(String addressType, String postalCode, String street, String addressNumber,
                                 String complement, UUID neighborhoodId, String neighborhoodName, Integer cityId,
                                 String cityName, String uf, Integer countryId, String foreignRegion,
                                 Boolean primary) {}

    public record ContactChannel(UUID id, String contactType, String value, String contactName, boolean whatsapp,
                                 boolean primary, boolean active) {}

    public record ContactChannelRequest(String contactType, String value, String contactName, Boolean whatsapp,
                                        Boolean primary) {}

    public record Document(UUID id, String documentType, String documentNumber, String issuer, String issuerUf,
                           LocalDate issueDate, LocalDate expiryDate, UUID storageObjectId, boolean primary) {}

    public record DocumentRequest(String documentType, String documentNumber, String issuer, String issuerUf,
                                  LocalDate issueDate, LocalDate expiryDate, UUID storageObjectId,
                                  Boolean primary) {}

    public record BankAccount(UUID id, String bankCode, String branchNumber, String accountNumber,
                              String accountDigit, String accountType, String pixKey, String pixKeyType,
                              String holderName, boolean primary) {}

    public record BankAccountRequest(String bankCode, String branchNumber, String accountNumber,
                                     String accountDigit, String accountType, String pixKey, String pixKeyType,
                                     String holderName, Boolean primary) {}

    /**
     * Relacionamento visto da pessoa da ficha. direction OUT: "relatedName é {type} desta pessoa"
     * (ex.: responsável). IN: "esta pessoa é {type} de relatedName" (ex.: responsável por).
     */
    public record Relationship(UUID id, UUID relatedPersonId, String relatedName, String relationshipType,
                               String direction, LocalDate startDate, LocalDate endDate, String notes) {}

    public record RelationshipRequest(UUID relatedPersonId, String relationshipType, String direction,
                                      LocalDate startDate, LocalDate endDate, String notes) {}

    public record HistoryEntry(UUID id, OffsetDateTime occurredAt, String operation, String tableName,
                               Map<String, Object> oldValues, Map<String, Object> newValues, String reason,
                               UUID userId, String userName) {}

    // ---------------------------------------------------------------- papéis

    public record PatientInfo(String recordNumber, String healthPlan, String healthPlanNumber,
                              String referralSource) {}

    public record CustomerInfo(String customerCode, BigDecimal creditLimit, boolean blocked, String blockReason,
                               LocalDate customerSince, boolean active, String notes, PatientInfo patient) {}

    public record CustomerRequest(BigDecimal creditLimit, Boolean blocked, String blockReason,
                                  LocalDate customerSince, Boolean active, String notes, PatientInfo patient) {}

    public record SupplierInfo(String supplierCode, String supplierType, Integer paymentTermsDays,
                               boolean blocked, String blockReason, boolean active, String notes) {}

    public record SupplierRequest(String supplierType, Integer paymentTermsDays, Boolean blocked,
                                  String blockReason, Boolean active, String notes) {}

    public record Specialty(UUID id, String specialty, String council, String councilNumber, String councilUf,
                            boolean primary) {}

    public record SpecialtyRequest(String specialty, String council, String councilNumber, String councilUf,
                                   Boolean primary) {}

    public record EmployeeInfo(String registrationNumber, UUID jobTitleId, String jobTitle, LocalDate hireDate,
                               LocalDate terminationDate, String contractType, String status, BigDecimal salary,
                               boolean salaryVisible, UUID userId, String userName, String notes,
                               List<Specialty> specialties) {}

    public record EmployeeRequest(String registrationNumber, UUID jobTitleId, LocalDate hireDate,
                                  LocalDate terminationDate, String contractType, String status,
                                  BigDecimal salary, UUID userId, String notes) {}

    public record JobTitle(UUID id, String title, boolean active) {}

    public record JobTitleRequest(String title, Boolean active) {}

    public record CustomerRow(UUID id, String name, String taxId, String mobile, String customerCode,
                              LocalDate customerSince, boolean active, boolean blocked, boolean patient,
                              String recordNumber, String healthPlan, LocalDate birthDate) {}

    public record SupplierRow(UUID id, String name, String tradeName, String taxId, String supplierCode,
                              String supplierType, String phone, String email, boolean active, boolean blocked) {}

    public record EmployeeRow(UUID id, String name, String registrationNumber, String jobTitle,
                              List<String> specialties, String council, UUID userId, String userName,
                              LocalDate hireDate, String status) {}

    public record UserOption(UUID id, String name, String email) {}

    // ---------------------------------------------------------------- duplicados

    public record DuplicateMember(UUID id, String name, String taxId, LocalDate birthDate, String mobile,
                                  OffsetDateTime createdAt, UUID issueId, String issueType) {}

    public record DuplicateGroup(String reason, String key, List<DuplicateMember> members) {}

    public record MergeRequest(UUID keepId, UUID removeId, String reason) {}

    public record MergeResult(UUID keptId, UUID removedId, List<String> movedTables, List<String> skippedTables) {}

    // ---------------------------------------------------------------- parâmetros (seção 18)

    public record FieldSetting(String fieldKey, String label, String groupName, boolean systemRequired,
                               int sortOrder, Map<String, Boolean> contexts) {}

    public record FieldSettings(List<FieldSetting> fields, OffsetDateTime updatedAt, String updatedByName) {}

    public record FieldSettingChange(String fieldKey, String context, boolean required) {}

    public record FieldSettingsUpdate(List<FieldSettingChange> changes) {}
}
