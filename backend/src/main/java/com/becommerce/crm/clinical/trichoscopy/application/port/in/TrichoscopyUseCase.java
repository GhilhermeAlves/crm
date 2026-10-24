package com.becommerce.crm.clinical.trichoscopy.application.port.in;

import com.becommerce.crm.clinical.trichoscopy.application.dto.TrichoscopyImageResponse;

import java.util.List;
import java.util.UUID;

public interface TrichoscopyUseCase {
    TrichoscopyImageResponse capture(UUID companyId, UUID contactId, String fileName, String contentType,
                                     byte[] data, String notes, UUID userId);

    List<TrichoscopyImageResponse> list(UUID companyId, UUID contactId);

    void delete(UUID companyId, UUID contactId, UUID imageId, UUID userId);
}
