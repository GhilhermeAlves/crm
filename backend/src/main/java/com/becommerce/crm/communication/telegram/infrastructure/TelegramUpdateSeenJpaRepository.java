package com.becommerce.crm.communication.telegram.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Idempotência de updates do Telegram. Não há entidade: um {@code Repository<Object, Long>}
 * do Spring Data falharia (tipo não gerenciado), então usa JdbcTemplate.
 */
@Repository
public class TelegramUpdateSeenJpaRepository {

    private final JdbcTemplate jdbc;

    public TelegramUpdateSeenJpaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Retorna 1 se gravou (update novo) e 0 se já existia. */
    @Transactional
    public int markSeen(UUID companyId, long updateId) {
        return jdbc.update(
                "INSERT INTO telegram_update_seen(company_id, update_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                companyId, updateId);
    }

    @Transactional
    public int deleteOlderThan(LocalDateTime before) {
        return jdbc.update("DELETE FROM telegram_update_seen WHERE seen_at < ?", Timestamp.valueOf(before));
    }
}
