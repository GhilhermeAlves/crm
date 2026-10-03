package com.becommerce.crm.analytics.audit.application.port.in;

import com.becommerce.crm.analytics.audit.application.dto.AuditLogResponse;
import com.becommerce.crm.analytics.audit.application.dto.AuditLogSearchRequest;
import com.becommerce.crm.shared.application.dto.PageResponse;

import java.util.UUID;

public interface AuditUseCase {
    AuditLogResponse getById(UUID id, UUID companyId);
    PageResponse<AuditLogResponse> search(UUID companyId, AuditLogSearchRequest request);
}
