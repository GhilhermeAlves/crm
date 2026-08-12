package com.becommerce.crm.application.contact.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ContactResponse(
        UUID id,
        UUID companyId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String notes,
        LocalDateTime createdAt
) {}