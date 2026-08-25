package com.becommerce.crm.application.template.port.output;

import com.becommerce.crm.domain.template.MessageTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída para templates ({@code message_templates}, V055). RLS FORCE isola o tenant. */
public interface TemplateRepository {

    MessageTemplate save(MessageTemplate template);

    Optional<MessageTemplate> findById(UUID id);

    void delete(MessageTemplate template);

    PageResult findByCompanyWithFilters(UUID companyId, String channelType, String status,
                                        int page, int pageSize);

    record PageResult(List<MessageTemplate> content, long totalElements) {}
}
