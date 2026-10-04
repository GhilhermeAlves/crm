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
        LocalDateTime createdAt
) {}
