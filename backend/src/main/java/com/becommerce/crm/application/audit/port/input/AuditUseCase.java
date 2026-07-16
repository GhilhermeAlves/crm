package com.becommerce.crm.application.audit.port.input;

import com.becommerce.crm.application.audit.dto.AuditLogResponse;
import com.becommerce.crm.application.audit.dto.AuditLogSearchRequest;
import com.becommerce.crm.application.identity.dto.PageResponse;

import java.util.UUID;

public interface AuditUseCase {
    AuditLogResponse getById(UUID id, UUID companyId);
    PageResponse<AuditLogResponse> search(UUID companyId, AuditLogSearchRequest request);
}
