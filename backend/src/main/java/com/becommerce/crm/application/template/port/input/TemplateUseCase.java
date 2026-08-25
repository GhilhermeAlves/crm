package com.becommerce.crm.application.template.port.input;

import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.template.dto.CreateTemplateRequest;
import com.becommerce.crm.application.template.dto.TemplateResponse;
import com.becommerce.crm.application.template.dto.UpdateTemplateRequest;

import java.util.UUID;

/** Casos de uso de Templates de mensagem (Sprint 17). */
public interface TemplateUseCase {

    TemplateResponse create(UUID companyId, CreateTemplateRequest request);

    TemplateResponse getById(UUID companyId, UUID templateId);

    TemplateResponse update(UUID companyId, UUID templateId, UpdateTemplateRequest request);

    void delete(UUID companyId, UUID templateId);

    PageResponse<TemplateResponse> list(UUID companyId, String channelType, String status,
                                        int page, int pageSize);
}
