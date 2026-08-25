package com.becommerce.crm.infrastructure.campaign.persistence;

import com.becommerce.crm.application.campaign.port.output.CampaignEventRepository;
import com.becommerce.crm.domain.campaign.CampaignChannel;
import com.becommerce.crm.domain.campaign.CampaignMessageEvent;
import com.becommerce.crm.domain.campaign.MessageEventStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistência JDBC dos eventos por destinatário e do canal da campanha.
 * JDBC (em vez de JPA) para usar INSERT ... ON CONFLICT DO NOTHING — a
 * UNIQUE (execution_id, recipient_id) é a garantia real de idempotência.
 */
@Repository
public class CampaignEventRepositoryImpl implements CampaignEventRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public CampaignEventRepositoryImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int insertAllIgnoringConflicts(List<CampaignMessageEvent> events) {
        int inserted = 0;
        for (CampaignMessageEvent event : events) {
            try {
                inserted += jdbc.update("""
                        INSERT INTO campaign_message_events
                            (id, company_id, execution_id, campaign_id, recipient_id,
                             recipient_type, recipient_phone, status, attempts)
                        VALUES
                            (:id, :companyId, :executionId, :campaignId, :recipientId,
                             :recipientType, :recipientPhone, :status, :attempts)
                        ON CONFLICT (execution_id, recipient_id) DO NOTHING
                        """, toParams(event));
            } catch (DataIntegrityViolationException ignored) {
                // corrida extrema: outro worker inseriu primeiro — evento ignorado (idempotente)
            }
        }
        return inserted;
    }

    @Override
    public void saveEvent(CampaignMessageEvent event) {
        jdbc.update("""
                UPDATE campaign_message_events
                   SET status = :status,
                       attempts = :attempts,
                       error_reason = :errorReason,
                       provider_message_id = :providerMessageId,
                       occurred_at = :occurredAt,
                       updated_at = CURRENT_TIMESTAMP
                 WHERE id = :id AND company_id = :companyId
                """, toParams(event));
    }

    @Override
    public Optional<CampaignMessageEvent> findById(UUID id) {
        List<CampaignMessageEvent> result = jdbc.query(
                "SELECT * FROM campaign_message_events WHERE id = :id",
                java.util.Map.of("id", id), new CampaignEventRowMapper());
        return result.stream().findFirst();
    }

    @Override
    public List<CampaignMessageEvent> findPendingBatch(UUID executionId, int limit) {
        return jdbc.query("""
                SELECT * FROM campaign_message_events
                 WHERE execution_id = :executionId AND status = 'PENDING'
                 ORDER BY created_at, recipient_id
                 LIMIT :limit
                """, java.util.Map.of("executionId", executionId, "limit", limit),
                new CampaignEventRowMapper());
    }

    @Override
    public long countByExecutionAndStatus(UUID executionId, MessageEventStatus status) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_message_events WHERE execution_id = :executionId AND status = :status",
                new MapSqlParameterSource()
                        .addValue("executionId", executionId)
                        .addValue("status", status.name()),
                Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public long countNotPendingByExecution(UUID executionId) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_message_events WHERE execution_id = :executionId AND status <> 'PENDING'",
                new MapSqlParameterSource().addValue("executionId", executionId),
                Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public int cancelPendingByExecution(UUID executionId) {
        return jdbc.update("""
                UPDATE campaign_message_events
                   SET status = 'CANCELLED', occurred_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                 WHERE execution_id = :executionId AND status = 'PENDING'
                """, java.util.Map.of("executionId", executionId));
    }

    @Override
    public PageResult findByCampaignWithFilters(UUID companyId, UUID campaignId, String status,
                                                int page, int pageSize) {
        String where = "WHERE campaign_id = :campaignId AND company_id = :companyId " +
                "AND (:status IS NULL OR :status::varchar = '' OR status = :status)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("campaignId", campaignId)
                .addValue("companyId", companyId)
                .addValue("status", status);

        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_message_events " + where, params, Long.class);
        params.addValue("limit", pageSize).addValue("offset", page * pageSize);
        List<CampaignMessageEvent> content = jdbc.query(
                "SELECT * FROM campaign_message_events " + where +
                        " ORDER BY created_at LIMIT :limit OFFSET :offset",
                params, new CampaignEventRowMapper());
        return new PageResult(content, total != null ? total : 0L);
    }

    @Override
    public Optional<CampaignChannel> findChannelByCampaignId(UUID campaignId) {
        List<CampaignChannel> result = jdbc.query("""
                SELECT * FROM campaign_channels WHERE campaign_id = :campaignId
                """, java.util.Map.of("campaignId", campaignId),
                (rs, rowNum) -> mapChannel(rs));
        return result.stream().findFirst();
    }

    @Override
    public CampaignChannel saveChannel(CampaignChannel channel) {
        jdbc.update("""
                INSERT INTO campaign_channels
                    (id, company_id, campaign_id, channel_type, provider_channel_id,
                     template_id, template_version)
                VALUES
                    (:id, :companyId, :campaignId, :channelType, :providerChannelId,
                     :templateId, :templateVersion)
                ON CONFLICT (campaign_id) DO UPDATE SET
                    channel_type = EXCLUDED.channel_type,
                    provider_channel_id = EXCLUDED.provider_channel_id,
                    template_id = EXCLUDED.template_id,
                    template_version = EXCLUDED.template_version
                """, new MapSqlParameterSource()
                .addValue("id", channel.getId())
                .addValue("companyId", channel.getCompanyId())
                .addValue("campaignId", channel.getCampaignId())
                .addValue("channelType", channel.getChannelType())
                .addValue("providerChannelId", channel.getProviderChannelId())
                .addValue("templateId", channel.getTemplateId())
                .addValue("templateVersion", channel.getTemplateVersion()));
        return channel;
    }

    private static MapSqlParameterSource toParams(CampaignMessageEvent e) {
        return new MapSqlParameterSource()
                .addValue("id", e.getId())
                .addValue("companyId", e.getCompanyId())
                .addValue("executionId", e.getExecutionId())
                .addValue("campaignId", e.getCampaignId())
                .addValue("recipientId", e.getRecipientId())
                .addValue("recipientType", e.getRecipientType())
                .addValue("recipientPhone", e.getRecipientPhone())
                .addValue("status", e.getStatus() != null ? e.getStatus().name()
                        : MessageEventStatus.PENDING.name())
                .addValue("attempts", e.getAttempts())
                .addValue("errorReason", e.getErrorReason())
                .addValue("providerMessageId", e.getProviderMessageId())
                .addValue("occurredAt", e.getOccurredAt() != null
                        ? Timestamp.valueOf(e.getOccurredAt()) : null);
    }

    private static class CampaignEventRowMapper implements RowMapper<CampaignMessageEvent> {
        @Override
        public CampaignMessageEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp occurred = rs.getTimestamp("occurred_at");
            return CampaignMessageEvent.reconstitute(
                    rs.getObject("id", UUID.class),
                    rs.getObject("company_id", UUID.class),
                    rs.getObject("execution_id", UUID.class),
                    rs.getObject("campaign_id", UUID.class),
                    rs.getObject("recipient_id", UUID.class),
                    rs.getString("recipient_type"),
                    rs.getString("recipient_phone"),
                    MessageEventStatus.valueOf(rs.getString("status")),
                    rs.getInt("attempts"),
                    rs.getString("error_reason"),
                    rs.getString("provider_message_id"),
                    occurred != null ? occurred.toLocalDateTime() : null,
                    rs.getTimestamp("created_at").toLocalDateTime());
        }
    }

    private static CampaignChannel mapChannel(ResultSet rs) throws SQLException {
        return CampaignChannel.reconstitute(
                rs.getObject("id", UUID.class),
                rs.getObject("company_id", UUID.class),
                rs.getObject("campaign_id", UUID.class),
                rs.getString("channel_type"),
                rs.getObject("provider_channel_id", UUID.class),
                rs.getObject("template_id", UUID.class),
                rs.getInt("template_version"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
