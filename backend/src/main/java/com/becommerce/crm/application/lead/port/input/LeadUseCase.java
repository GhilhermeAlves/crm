package com.becommerce.crm.application.lead.port.input;

import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.lead.dto.CreateLeadRequest;
import com.becommerce.crm.application.lead.dto.LeadResponse;
import com.becommerce.crm.application.lead.dto.UpdateLeadRequest;
import com.becommerce.crm.domain.lead.LeadClassification;
import com.becommerce.crm.domain.lead.LeadSource;
import com.becommerce.crm.domain.lead.LeadStatus;

import java.util.UUID;

/** Casos de uso de leads (Sprint 10), sempre scoped pela empresa ativa. */
public interface LeadUseCase {

    LeadResponse create(UUID companyId, CreateLeadRequest request, UUID createdBy);

    LeadResponse getById(UUID companyId, UUID leadId);

    LeadResponse update(UUID companyId, UUID leadId, UpdateLeadRequest request);

    void delete(UUID companyId, UUID leadId);

    PageResponse<LeadResponse> list(UUID companyId, String status, String source, String classification,
                                    int page, int pageSize, String sortBy, String sortDirection);
}