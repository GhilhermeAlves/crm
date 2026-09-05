package com.becommerce.crm.infrastructure.ai.persistence;

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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de integração REAL (Testcontainers PostgreSQL 17 + HikariCP) que prova o
 * isolamento de tenant da IA autônoma (Sprint 2):
 * (1) RLS FORCE: {@code agent_config} e {@code agent_auto_replies} de A jamais
 *     visíveis para B — sem desabilitar RLS, sem bypass de tenant;
 * (2) idempotência da reserva: {@code (company_id, inbound_message_id)} único;
 * (3) persistência dos campos de geração da V071 (model/temperature/max_tokens).
 */
@Testcontainers
class AgentConfigIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("crm_it_agent")
            .withUsername("crm_superuser")
            .withPassword("crm_it_pass");

    static final String APP_USER = "crm_agent_app_user";
    static final String APP_PASSWORD = "crm_agent_app_pass";

    static HikariDataSource rawPool;
    static TenantAwareDataSource tenantAwareDataSource;

    static final UUID TENANT_A = UUID.fromString("11111111-2222-3333-4444-555555555551");
    static final UUID TENANT_B = UUID.fromString("11111111-2222-3333-4444-555555555552");

    static final UUID CONV_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000011");
    static final UUID CONV_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000012");
    static final UUID INBOUND_MSG = UUID.fromString("cccccccc-0000-0000-0000-000000000021");

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("agent-rls-bootstrap.sql"));
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
        config.setConnectionTimeout(5000);
        rawPool = new HikariDataSource(config);
        tenantAwareDataSource = new TenantAwareDataSource(rawPool);

        seedData();
    }

    @AfterAll
    static void tearDown() {
        if (rawPool != null) {
            rawPool.close();
        }
    }

    @BeforeEach
    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private static void seedData() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            insertCompany(conn, TENANT_A, "Agent Tenant A LTDA", "11.111.111/0001-11", "a.agent@crm.local");
            insertConversation(conn, CONV_A, TENANT_A);
            insertAgentConfig(conn, TENANT_A, "Você responde como Léo.", "gpt-4o", "0.7", "300");
            insertAutoReply(conn, TENANT_A, CONV_A, INBOUND_MSG);
        }

        TenantContext.setCompanyId(TENANT_B);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            insertCompany(conn, TENANT_B, "Agent Tenant B LTDA", "22.222.222/0002-22", "b.agent@crm.local");
            insertConversation(conn, CONV_B, TENANT_B);
            insertAgentConfig(conn, TENANT_B, "Prompt de B.", null, null, null);
        }

        TenantContext.clear();
    }

    private static void insertCompany(Connection conn, UUID id, String name, String cnpj, String email)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO companies (id, legal_name, trading_name, cnpj, email, phone,
                    address_zip_code, address_street, address_number, address_neighborhood,
                    address_city, address_state, address_country, plan, status,
                    max_users, max_storage_mb)
                VALUES (?, ?, ?, ?, ?, '0000-0000', '00000-000', 'Rua Teste', '0', 'Centro',
                    'Sao Paulo', 'SP', 'Brasil', 'STARTER', 'ACTIVE', 10, 1024)
                """)) {
            ps.setObject(1, id);
            ps.setString(2, name);
            ps.setString(3, name);
            ps.setString(4, cnpj);
            ps.setString(5, email);
            ps.executeUpdate();
        }
    }

    private static void insertConversation(Connection conn, UUID id, UUID companyId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO omnichannel_conversations (id, company_id, external_phone, status)
                VALUES (?, ?, '+550011112222', 'OPEN')
                """)) {
            ps.setObject(1, id);
            ps.setObject(2, companyId);
            ps.setString(3, "+550011112222");
            ps.executeUpdate();
        }
    }

    private static void insertAgentConfig(Connection conn, UUID companyId, String prompt,
                                          String model, String temperature, String maxTokens)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO agent_config (company_id, ai_enabled, allow_auto_reply, system_prompt,
                    model, temperature, max_tokens, cooldown_minutes, max_chars)
                VALUES (?, TRUE, TRUE, ?, ?, ?, ?, 60, 1000)
                """)) {
            ps.setObject(1, companyId);
            ps.setString(2, prompt);
            ps.setString(3, model);
            ps.setObject(4, temperature != null ? new BigDecimal(temperature) : null);
            ps.setObject(5, maxTokens != null ? Integer.valueOf(maxTokens) : null);
            ps.executeUpdate();
        }
    }

    private static void insertAutoReply(Connection conn, UUID companyId, UUID conversationId,
                                        UUID inboundMessageId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO agent_auto_replies (company_id, conversation_id, inbound_message_id)
                VALUES (?, ?, ?)
                """)) {
            ps.setObject(1, companyId);
            ps.setObject(2, conversationId);
            ps.setObject(3, inboundMessageId);
            ps.executeUpdate();
        }
    }

    // ---------------------- RLS / isolamento ------------------------------

    @Test
    void tenantA_shouldOnlySeeOwnAgentConfig() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        assertEquals(1, countAgentConfig(), "Tenant A deve ver apenas 1 agent_config (o seu)");
        assertEquals(1, countAutoReplies(), "Tenant A deve ver apenas 1 reserva (a sua)");
    }

    @Test
    void tenantB_shouldNotSeeTenantAData() throws SQLException {
        TenantContext.setCompanyId(TENANT_B);
        assertEquals(1, countAgentConfig(), "Tenant B deve ver apenas o agent_config de B");
        assertEquals(0, countAutoReplies(), "Tenant B não deve ver reservas de A");
    }

    @Test
    void noContext_shouldSeeNothing() throws SQLException {
        TenantContext.clear();
        assertEquals(0, countAgentConfig());
        assertEquals(0, countAutoReplies());
    }

    @Test
    void crossTenantConfigInsert_shouldBeBlockedByRls() {
        TenantContext.setCompanyId(TENANT_A);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                         INSERT INTO agent_config (company_id, ai_enabled, allow_auto_reply,
                             system_prompt, cooldown_minutes, max_chars)
                         VALUES (?, TRUE, TRUE, 'Invasao', 60, 1000)
                         """)) {
                ps.setObject(1, TENANT_B);
                ps.executeUpdate();
            }
        }, "INSERT cross-tenant de agent_config deve ser bloqueado por RLS");
    }

    @Test
    void crossTenantAutoReplyInsert_shouldBeBlockedByRls() {
        TenantContext.setCompanyId(TENANT_A);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                         INSERT INTO agent_auto_replies (company_id, conversation_id, inbound_message_id)
                         VALUES (?, ?, gen_random_uuid())
                         """)) {
                ps.setObject(1, TENANT_B);
                ps.setObject(2, CONV_A);
                ps.executeUpdate();
            }
        }, "Reserva de auto-resposta cross-tenant deve ser bloqueada por RLS");
    }

    // ---------------------- Idempotência (V070) ----------------------------

    @Test
    void duplicateInboundReserve_shouldViolateUniqueConstraint() {
        TenantContext.setCompanyId(TENANT_A);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                         INSERT INTO agent_auto_replies (company_id, conversation_id, inbound_message_id)
                         VALUES (?, ?, ?)
                         """)) {
                ps.setObject(1, TENANT_A);
                ps.setObject(2, CONV_A);
                ps.setObject(3, INBOUND_MSG);
                ps.executeUpdate();
            }
        }, "Mesmo (company_id, inbound_message_id) deve violar a chave única de idempotência");
    }

    // ---------------------- V071: campos de geração -------------------------

    @Test
    void generationFields_shouldPersistAndRoundTrip() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT model, temperature, max_tokens FROM agent_config WHERE company_id = ?");
             ResultSet rs = execute(ps, TENANT_A)) {
            assertTrue(rs.next());
            assertEquals("gpt-4o", rs.getString("model"));
            assertEquals(0, rs.getBigDecimal("temperature").compareTo(new BigDecimal("0.7")));
            assertEquals(300, rs.getInt("max_tokens"));
        }
    }

    // ---------------------- helpers ---------------------------------------

    private ResultSet execute(PreparedStatement ps, UUID companyId) throws SQLException {
        ps.setObject(1, companyId);
        return ps.executeQuery();
    }

    private int countAgentConfig() throws SQLException {
        return countOf("SELECT count(*) FROM agent_config");
    }

    private int countAutoReplies() throws SQLException {
        return countOf("SELECT count(*) FROM agent_auto_replies");
    }

    private int countOf(String sql) throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}