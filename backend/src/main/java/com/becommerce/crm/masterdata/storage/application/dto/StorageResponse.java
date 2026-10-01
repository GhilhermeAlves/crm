package com.becommerce.crm.masterdata.storage.application.dto;

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