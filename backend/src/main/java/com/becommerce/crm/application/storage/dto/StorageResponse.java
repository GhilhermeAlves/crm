package com.becommerce.crm.application.storage.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record StorageResponse(
        UUID id,
        String objectKey,
        String fileName,
        String contentType,
        long sizeBytes,
        UUID companyId,
        LocalDateTime createdAt
) {}