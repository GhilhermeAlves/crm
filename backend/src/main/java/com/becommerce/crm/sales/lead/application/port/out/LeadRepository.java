package com.becommerce.crm.sales.lead.application.port.out;

import com.becommerce.crm.sales.lead.domain.Lead;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para leads por empresa (tabela {@code leads}, V016).
 * O isolamento por tenant é garantido pelo RLS FORCE (V021); a listagem com
 * filtros NÃO filtra por company explicitamente — o RLS entrega só a empresa
 * do TenantContext.
 */
public interface LeadRepository {

    Lead save(Lead lead);

    Optional<Lead> findById(UUID id);

    void delete(Lead lead);

    boolean existsByContactIdAndCompanyId(UUID contactId, UUID companyId);

    PageResult findByCompanyWithFilters(UUID companyId, String status, String source, String classification,
                                        String search, int page, int pageSize, String sortBy, String sortDirection);

    record PageResult(List<Lead> content, long totalElements) {}
}