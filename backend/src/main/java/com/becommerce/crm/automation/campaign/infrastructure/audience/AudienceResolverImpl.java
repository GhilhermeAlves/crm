package com.becommerce.crm.automation.campaign.infrastructure.audience;

import com.becommerce.crm.automation.campaign.application.port.output.AudienceResolver;
import com.becommerce.crm.automation.campaign.domain.AudienceType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import com.becommerce.crm.automation.campaign.domain.CampaignMessageRenderer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolução de público da Sprint 17 (segmentação simples): contatos ou leads
 * ativos, com telefone preenchido (necessário para WhatsApp). Critérios JSON
 * suportados nesta sprint: {"onlyWithPhone": true} (default), {"status": "..."}.
 * SELECTED usa {"contactIds": [...]}: só os contatos escolhidos (com telefone).
 * Determinístico (ORDER BY id) e tenant-safe (company_id explícito + RLS).
 */
@Component
public class AudienceResolverImpl implements AudienceResolver {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final NamedParameterJdbcTemplate jdbc;

    public AudienceResolverImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Recipient> resolve(UUID companyId, AudienceType audienceType,
                                   String audienceCriteriaJson) {
        boolean onlyWithPhone = audienceCriteriaJson == null
                || !audienceCriteriaJson.contains("\"onlyWithPhone\": false");
        // WhatsApp usa o celular (V080); telefone fixo é fallback para cadastros antigos.
        String phoneFilter = onlyWithPhone ? "AND COALESCE(NULLIF(c.mobile, ''), NULLIF(c.phone, '')) IS NOT NULL" : "";

        return switch (audienceType) {
            case CONTACTS -> jdbc.query("""
                    SELECT c.id, 'CONTACT' AS recipient_type, COALESCE(NULLIF(c.mobile, ''), NULLIF(c.phone, '')) AS phone
                      FROM contacts c
                     WHERE c.company_id = :companyId AND c.deleted_at IS NULL %s
                     ORDER BY c.id
                    """.formatted(phoneFilter),
                    new MapSqlParameterSource().addValue("companyId", companyId),
                    (rs, n) -> new Recipient(rs.getObject("id", UUID.class),
                            rs.getString("recipient_type"), rs.getString("phone")));
            case LEADS -> jdbc.query("""
                    SELECT c.id, 'LEAD' AS recipient_type, COALESCE(NULLIF(c.mobile, ''), NULLIF(c.phone, '')) AS phone
                      FROM leads l
                      JOIN contacts c ON c.id = l.contact_id
                     WHERE l.company_id = :companyId AND c.deleted_at IS NULL %s
                     ORDER BY c.id
                    """.formatted(phoneFilter),
                    new MapSqlParameterSource().addValue("companyId", companyId),
                    (rs, n) -> new Recipient(rs.getObject("id", UUID.class),
                            rs.getString("recipient_type"), rs.getString("phone")));
            case SELECTED -> {
                List<UUID> ids = selectedContactIds(audienceCriteriaJson);
                if (ids.isEmpty()) {
                    yield List.of();
                }
                yield jdbc.query("""
                        SELECT c.id, 'CONTACT' AS recipient_type, COALESCE(NULLIF(c.mobile, ''), NULLIF(c.phone, '')) AS phone
                          FROM contacts c
                         WHERE c.company_id = :companyId AND c.deleted_at IS NULL
                           AND c.id IN (:ids) %s
                         ORDER BY c.id
                        """.formatted(phoneFilter),
                        new MapSqlParameterSource().addValue("companyId", companyId).addValue("ids", ids),
                        (rs, n) -> new Recipient(rs.getObject("id", UUID.class),
                                rs.getString("recipient_type"), rs.getString("phone")));
            }
        };
    }

    @Override
    public Optional<CampaignMessageRenderer.Profile> findProfile(UUID companyId, UUID contactId) {
        List<CampaignMessageRenderer.Profile> rows = jdbc.query("""
                SELECT c.first_name, c.last_name, c.email,
                       COALESCE(NULLIF(c.mobile, ''), NULLIF(c.phone, '')) AS phone
                  FROM contacts c
                 WHERE c.company_id = :companyId AND c.id = :id
                """,
                new MapSqlParameterSource().addValue("companyId", companyId).addValue("id", contactId),
                (rs, n) -> new CampaignMessageRenderer.Profile(rs.getString("first_name"),
                        rs.getString("last_name"), rs.getString("email"), rs.getString("phone")));
        return rows.stream().findFirst();
    }

    /** IDs inválidos ou JSON malformado são ignorados (público vazio barra a execução). */
    static List<UUID> selectedContactIds(String audienceCriteriaJson) {
        List<UUID> ids = new ArrayList<>();
        if (audienceCriteriaJson == null || audienceCriteriaJson.isBlank()) {
            return ids;
        }
        try {
            JsonNode node = JSON.readTree(audienceCriteriaJson).path("contactIds");
            for (JsonNode id : node) {
                try {
                    ids.add(UUID.fromString(id.asText()));
                } catch (IllegalArgumentException ignored) {
                    // id inválido: fica fora do público
                }
            }
        } catch (Exception e) {
            return List.of();
        }
        return ids;
    }
}
