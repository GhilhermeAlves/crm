package com.becommerce.crm.masterdata.contact.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ContactResponse(
        UUID id,
        UUID companyId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String mobile,
        String notes,
        LocalDate birthDate,
        String cpf,
        String rg,
        String rgIssuer,
        String gender,
        String maritalStatus,
        String professionalStatus,
        String zipCode,
        String street,
        String addressNumber,
        String addressComplement,
        String neighborhood,
        String city,
        String state,
        String healthPlan,
        String healthPlanNumber,
        String recordNumber,
        String referralSource,
        boolean hasGuardian,
        String guardianName,
        String guardianCpf,
        String guardianPhone,
        LocalDateTime createdAt,
        boolean isPatient,
        LocalDate patientSince,
        UUID photoStorageId
) {

    /** Contato sem marcação de paciente (compatibilidade com chamadas anteriores à V104). */
    public ContactResponse(UUID id, UUID companyId, String firstName, String lastName, String email,
                           String phone, String mobile, String notes, LocalDate birthDate, String cpf,
                           String rg, String rgIssuer, String gender, String maritalStatus,
                           String professionalStatus, String zipCode, String street,
                           String addressNumber, String addressComplement, String neighborhood,
                           String city, String state, String healthPlan, String healthPlanNumber,
                           String recordNumber, String referralSource, boolean hasGuardian,
                           String guardianName, String guardianCpf, String guardianPhone,
                           LocalDateTime createdAt) {
        this(id, companyId, firstName, lastName, email, phone, mobile, notes, birthDate, cpf, rg,
                rgIssuer, gender, maritalStatus, professionalStatus, zipCode, street, addressNumber,
                addressComplement, neighborhood, city, state, healthPlan, healthPlanNumber,
                recordNumber, referralSource, hasGuardian, guardianName, guardianCpf, guardianPhone,
                createdAt, false, null, null);
    }

    /** Compatibilidade com chamadas antigas. */
    public ContactResponse(UUID id, UUID companyId, String firstName, String lastName, String email,
                           String phone, String mobile, String notes, LocalDate birthDate, String cpf,
                           String rg, String rgIssuer, String gender, String maritalStatus,
                           String professionalStatus, String zipCode, String street,
                           String addressNumber, String addressComplement, String neighborhood,
                           String city, String state, String healthPlan, String healthPlanNumber,
                           String recordNumber, String referralSource, boolean hasGuardian,
                           String guardianName, String guardianCpf, String guardianPhone,
                           LocalDateTime createdAt, boolean isPatient, LocalDate patientSince) {
        this(id, companyId, firstName, lastName, email, phone, mobile, notes, birthDate, cpf, rg,
                rgIssuer, gender, maritalStatus, professionalStatus, zipCode, street, addressNumber,
                addressComplement, neighborhood, city, state, healthPlan, healthPlanNumber,
                recordNumber, referralSource, hasGuardian, guardianName, guardianCpf, guardianPhone,
                createdAt, isPatient, patientSince, null);
    }
}
