package com.becommerce.crm.sales.lead.application.port.in;

import com.becommerce.crm.shared.application.dto.PageResponse;
import com.becommerce.crm.sales.lead.application.dto.CreateLeadRequest;
import com.becommerce.crm.sales.lead.application.dto.LeadResponse;
import com.becommerce.crm.sales.lead.application.dto.UpdateLeadRequest;

import java.util.UUID;

/** Casos de uso de leads (Sprint 10), sempre scoped pela empresa ativa. */
public interface LeadUseCase {

    LeadResponse create(UUID companyId, CreateLeadRequest request, UUID createdBy);

    LeadResponse getById(UUID companyId, UUID leadId);

    LeadResponse update(UUID companyId, UUID leadId, UpdateLeadRequest request);

    void delete(UUID companyId, UUID leadId);

    PageResponse<LeadResponse> list(UUID companyId, String status, String source, String classification,
                                    String search, int page, int pageSize, String sortBy, String sortDirection);
}