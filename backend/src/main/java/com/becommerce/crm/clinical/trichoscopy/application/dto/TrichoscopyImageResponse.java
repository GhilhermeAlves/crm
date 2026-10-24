package com.becommerce.crm.clinical.trichoscopy.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TrichoscopyImageResponse(
        UUID id,
        UUID contactId,
        UUID storageObjectId,
        UUID takenBy,
        LocalDateTime takenAt,
        String notes,
        String downloadUrl
) {
}
