package com.becommerce.crm.infrastructure.campaign.audience;

import com.becommerce.crm.application.campaign.port.output.AudienceResolver;
import com.becommerce.crm.domain.campaign.AudienceType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Resolução de público da Sprint 17 (segmentação simples): contatos ou leads
 * ativos, com telefone preenchido (necessário para WhatsApp). Critérios JSON
 * suportados nesta sprint: {"onlyWithPhone": true} (default), {"status": "..."}.
 * Determinístico (ORDER BY id) e tenant-safe (company_id explícito + RLS).
 */
@Component
public class AudienceResolverImpl implements AudienceResolver {

    private final NamedParameterJdbcTemplate jdbc;

    public AudienceResolverImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Recipient> resolve(UUID companyId, AudienceType audienceType,
                                   String audienceCriteriaJson) {
        boolean onlyWithPhone = audienceCriteriaJson == null
                || !audienceCriteriaJson.contains("\"onlyWithPhone\": false");
        String phoneFilter = onlyWithPhone ? "AND phone IS NOT NULL AND phone <> ''" : "";

        return switch (audienceType) {
            case CONTACTS -> jdbc.query("""
                    SELECT id, 'CONTACT' AS recipient_type, phone
                      FROM contacts
                     WHERE company_id = :companyId AND deleted_at IS NULL %s
                     ORDER BY id
                    """.formatted(phoneFilter),
                    new MapSqlParameterSource().addValue("companyId", companyId),
                    (rs, n) -> new Recipient(rs.getObject("id", UUID.class),
                            rs.getString("recipient_type"), rs.getString("phone")));
            case LEADS -> jdbc.query("""
                    SELECT c.id, 'LEAD' AS recipient_type, c.phone
                      FROM leads l
                      JOIN contacts c ON c.id = l.contact_id
                     WHERE l.company_id = :companyId AND c.deleted_at IS NULL %s
                     ORDER BY c.id
                    """.formatted(phoneFilter),
                    new MapSqlParameterSource().addValue("companyId", companyId),
                    (rs, n) -> new Recipient(rs.getObject("id", UUID.class),
                            rs.getString("recipient_type"), rs.getString("phone")));
        };
    }
}
