package com.becommerce.crm.infrastructure.campaign.persistence;

import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import com.becommerce.crm.infrastructure.tenant.datasource.TenantAwareDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 17 — Tenant isolation de Campanhas (RLS FORCE) + idempotência.
 * Padrão da Sprint 16: Testcontainers PostgreSQL 17, usuário NÃO-superuser,
 * pool Hikari pequeno envolto por {@link TenantAwareDataSource}.
 */
@Testcontainers
class CampaignIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("campaign_it")
            .withUsername("crm_superuser")
            .withPassword("campaign_it_pass");

    static final String APP_USER = "crm_app_user";
    static final String APP_PASSWORD = "crm_app_pass";

    static HikariDataSource rawPool;
    static TenantAwareDataSource tenantDs;

    static final UUID TENANT_A = UUID.fromString("aaaaaaa1-0000-0000-0000-000000000001");
    static final UUID TENANT_B = UUID.fromString("bbbbbbb2-0000-0000-0000-000000000002");

    UUID campaignA;
    UUID executionA;
    UUID recipientId;

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("tenant-rls-bootstrap.sql"));
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("campaign-rls-bootstrap.sql"));

            try (Statement st = conn.createStatement()) {
                st.execute("CREATE ROLE " + APP_USER + " LOGIN PASSWORD '" + APP_PASSWORD
                        + "' NOSUPERUSER NOBYPASSRLS");
                st.execute("GRANT USAGE ON SCHEMA public TO " + APP_USER);
                st.execute("GRANT USAGE ON SCHEMA app TO " + APP_USER);
                st.execute("GRANT ALL ON ALL TABLES IN SCHEMA public TO " + APP_USER);
                st.execute("GRANT EXECUTE ON FUNCTION app.current_tenant_id() TO " + APP_USER);
            }
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(APP_USER);
        config.setPassword(APP_PASSWORD);
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(2);
        rawPool = new HikariDataSource(config);
        tenantDs = new TenantAwareDataSource(rawPool);

        seedCompanies();
    }

    @AfterAll
    static void tearDown() {
        if (rawPool != null) {
            rawPool.close();
        }
    }

    @BeforeEach
    void seedTenantData() throws SQLException {
        campaignA = insertCampaign(TENANT_A, "Campanha A");
        executionA = insertExecution(campaignA, TENANT_A);
        recipientId = insertContact(TENANT_A, "+5511999990001");
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private static void seedCompanies() throws SQLException {
        try (Connection conn = tenantDs.getConnection()) {
            insertCompany(conn, TENANT_A, "Campaign A LTDA", "11.111.111/0001-11", "ca@crm.local");
            insertCompany(conn, TENANT_B, "Campaign B LTDA", "22.222.222/0002-22", "cb@crm.local");
        }
    }

    // =========================================================================
    // Tenant isolation: Tenant B não vê nem modifica dados da campanha do A
    // =========================================================================

    @Test
    void tenantBCannotReadCampaignOfTenantA() throws SQLException {
        try (Connection conn = tenantDs.getConnection()) {
            TenantContext.setCompanyId(TENANT_B);
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM campaigns WHERE id = ?")) {
                ps.setObject(1, campaignA);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    assertEquals(0, rs.getInt(1), "Tenant B não deve enxergar campanha do Tenant A");
                }
            }
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void tenantBUpdateOnCampaignOfTenantAAffectsZeroRows() throws SQLException {
        try (Connection conn = tenantDs.getConnection()) {
            TenantContext.setCompanyId(TENANT_B);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE campaigns SET name = 'HACKED' WHERE id = ?")) {
                ps.setObject(1, campaignA);
                assertEquals(0, ps.executeUpdate(), "UPDATE cross-tenant deve afetar 0 linhas");
            }
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void tenantBDeleteOnEventsOfTenantAAffectsZeroRows() throws SQLException {
        insertEvent(executionA, campaignA, TENANT_A, recipientId);
        try (Connection conn = tenantDs.getConnection()) {
            TenantContext.setCompanyId(TENANT_B);
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM campaign_message_events WHERE campaign_id = ?")) {
                ps.setObject(1, campaignA);
                assertEquals(0, ps.executeUpdate(), "DELETE cross-tenant deve afetar 0 linhas");
            }
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void tenantACannotInsertEventForOtherTenantCompany() {
        // WITH CHECK bloqueia inserir evento apontando para company de outro tenant
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantDs.getConnection()) {
                TenantContext.setCompanyId(TENANT_B);
                try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT INTO campaign_message_events
                            (company_id, execution_id, campaign_id, recipient_id)
                        VALUES (?, ?, ?, ?)
                        """)) {
                    ps.setObject(1, TENANT_A); // company alheia ao contexto (TENANT_B)
                    ps.setObject(2, executionA);
                    ps.setObject(3, campaignA);
                    ps.setObject(4, UUID.randomUUID());
                    ps.executeUpdate();
                } finally {
                    TenantContext.clear();
                }
            }
        });
    }

    // =========================================================================
    // Idempotência: UNIQUE (execution_id, recipient_id)
    // =========================================================================

    @Test
    void duplicateRecipientEventIsRejectedByUniqueConstraint() throws SQLException {
        insertEvent(executionA, campaignA, TENANT_A, recipientId);
        SQLException thrown = assertThrows(SQLException.class,
                () -> insertEvent(executionA, campaignA, TENANT_A, recipientId));
        assertTrue(thrown.getMessage() != null
                        && (thrown.getMessage().contains("uq_campaign_message_events")
                        || thrown.getMessage().contains("duplicate key")),
                "Esperada violação de UNIQUE, recebido: " + thrown.getMessage());
    }

    @Test
    void onConflictDoNothingIsIdempotent() throws SQLException {
        int first = insertEventOnConflict(executionA, campaignA, TENANT_A, recipientId);
        int second = insertEventOnConflict(executionA, campaignA, TENANT_A, recipientId);
        assertTrue(first >= 1);
        assertFalse(second > 0, "Segundo insert com ON CONFLICT não deve inserir nova linha");

        try (Connection conn = tenantDs.getConnection()) {
            TenantContext.setCompanyId(TENANT_A);
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM campaign_message_events WHERE execution_id = ?")) {
                ps.setObject(1, executionA);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    assertEquals(1, rs.getInt(1), "Deve existir exatamente 1 evento por destinatário");
                }
            }
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void claimForExecutionIsAtomic() throws SQLException {
        try (Connection conn = tenantDs.getConnection()) {
            TenantContext.setCompanyId(TENANT_A);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE campaigns SET status = 'SCHEDULED' WHERE id = ?")) {
                ps.setObject(1, campaignA);
                ps.executeUpdate();
            }
        } finally {
            TenantContext.clear();
        }

        int claimedTwice = 0;
        for (int i = 0; i < 2; i++) {
            try (Connection conn = tenantDs.getConnection()) {
                TenantContext.setCompanyId(TENANT_A);
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE campaigns SET status = 'RUNNING' WHERE id = ? AND status = 'SCHEDULED'")) {
                    ps.setObject(1, campaignA);
                    claimedTwice += ps.executeUpdate();
                }
            } finally {
                TenantContext.clear();
            }
        }
        assertEquals(1, claimedTwice, "Claim atômico só pode ser bem-sucedido uma vez");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static void insertCompany(Connection conn, UUID id, String legal, String cnpj,
                                      String email) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO companies (id, legal_name, trading_name, cnpj, email, phone,
                    address_zip_code, address_street, address_number, address_neighborhood,
                    address_city, address_state, address_country, plan, status, max_users, max_storage_mb)
                VALUES (?, ?, ?, ?, ?, '11999990000', '00000-000', 'Rua', '1', 'Centro',
                    'São Paulo', 'SP', 'Brasil', 'FREE', 'ACTIVE', 10, 100)
                ON CONFLICT (id) DO NOTHING
                """)) {
            ps.setObject(1, id);
            ps.setString(2, legal);
            ps.setString(3, legal);
            ps.setString(4, cnpj);
            ps.setString(5, email);
            ps.executeUpdate();
        }
    }

    private UUID insertCampaign(UUID companyId, String name) throws SQLException {
        UUID id = UUID.randomUUID();
        try (Connection conn = tenantDs.getConnection()) {
            TenantContext.setCompanyId(companyId);
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO campaigns (id, company_id, name) VALUES (?, ?, ?)
                    """)) {
                ps.setObject(1, id);
                ps.setObject(2, companyId);
                ps.setString(3, name);
                ps.executeUpdate();
            } finally {
                TenantContext.clear();
            }
        }
        return id;
    }

    private UUID insertExecution(UUID campaignId, UUID companyId) throws SQLException {
        UUID id = UUID.randomUUID();
        try (Connection conn = tenantDs.getConnection()) {
            TenantContext.setCompanyId(companyId);
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO campaign_executions (id, company_id, campaign_id) VALUES (?, ?, ?)
                    """)) {
                ps.setObject(1, id);
                ps.setObject(2, companyId);
                ps.setObject(3, campaignId);
                ps.executeUpdate();
            } finally {
                TenantContext.clear();
            }
        }
        return id;
    }

    private UUID insertContact(UUID companyId, String phone) throws SQLException {
        UUID id = UUID.randomUUID();
        try (Connection conn = tenantDs.getConnection()) {
            TenantContext.setCompanyId(companyId);
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO contacts (id, company_id, first_name, phone) VALUES (?, ?, 'Contato', ?)
                    """)) {
                ps.setObject(1, id);
                ps.setObject(2, companyId);
                ps.setString(3, phone);
                ps.executeUpdate();
            } finally {
                TenantContext.clear();
            }
        }
        return id;
    }

    private void insertEvent(UUID executionId, UUID campaignId, UUID companyId,
                             UUID contactId) throws SQLException {
        try (Connection conn = tenantDs.getConnection()) {
            TenantContext.setCompanyId(companyId);
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO campaign_message_events
                        (company_id, execution_id, campaign_id, recipient_id)
                    VALUES (?, ?, ?, ?)
                    """)) {
                ps.setObject(1, companyId);
                ps.setObject(2, executionId);
                ps.setObject(3, campaignId);
                ps.setObject(4, contactId);
                ps.executeUpdate();
            } finally {
                TenantContext.clear();
            }
        }
    }

    private int insertEventOnConflict(UUID executionId, UUID campaignId, UUID companyId,
                                      UUID contactId) throws SQLException {
        try (Connection conn = tenantDs.getConnection()) {
            TenantContext.setCompanyId(companyId);
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO campaign_message_events
                        (company_id, execution_id, campaign_id, recipient_id)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (execution_id, recipient_id) DO NOTHING
                    """)) {
                ps.setObject(1, companyId);
                ps.setObject(2, executionId);
                ps.setObject(3, campaignId);
                ps.setObject(4, contactId);
                return ps.executeUpdate();
            } finally {
                TenantContext.clear();
            }
        }
    }
}
