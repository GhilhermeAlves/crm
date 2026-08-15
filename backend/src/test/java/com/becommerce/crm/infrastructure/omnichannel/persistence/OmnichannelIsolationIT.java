package com.becommerce.crm.infrastructure.omnichannel.persistence;

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
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Teste de integração REAL (Testcontainers PostgreSQL 17 + HikariCP) que prova
 * o módulo omnichannel da Sprint 16:
 * (1) isolamento cross-tenant via RLS FORCE (FASE 16) — canais, conversas e
 *     mensagens de A jamais visíveis para B;
 * (2) idempotência por chave única (FASE 17): (company_id, external_message_id)
 *     e (company_id, client_message_id) bloqueiam duplicação, e o insert
 *     idempotente via ON CONFLICT DO NOTHING não cria registro repetido.
 */
@Testcontainers
class OmnichannelIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("crm_it_omnichannel")
            .withUsername("crm_superuser")
            .withPassword("crm_it_pass");

    static final String APP_USER = "crm_app_user";
    static final String APP_PASSWORD = "crm_app_pass";

    static HikariDataSource rawPool;
    static TenantAwareDataSource tenantAwareDataSource;

    static final UUID TENANT_A = UUID.fromString("11111111-2222-3333-4444-555555555551");
    static final UUID TENANT_B = UUID.fromString("11111111-2222-3333-4444-555555555552");

    static final UUID CHANNEL_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    static final UUID CHANNEL_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    static final UUID CONV_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000011");
    static final UUID CONV_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000012");
    static final String EXTERNAL_MSG = "wamid-ABC-123";

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("omnichannel-rls-bootstrap.sql"));
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
            insertCompany(conn, TENANT_A, "Omni Tenant A LTDA", "13.131.131/0001-13", "a.omni@crm.local");
            insertChannel(conn, CHANNEL_A, TENANT_A, "espaco-a");
            insertConversation(conn, CONV_A, TENANT_A, CHANNEL_A);
            insertMessage(conn, CONV_A, TENANT_A, CHANNEL_A, EXTERNAL_MSG);
        }

        TenantContext.setCompanyId(TENANT_B);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            insertCompany(conn, TENANT_B, "Omni Tenant B LTDA", "24.242.242/0002-24", "b.omni@crm.local");
            insertChannel(conn, CHANNEL_B, TENANT_B, "espaco-b");
            insertConversation(conn, CONV_B, TENANT_B, CHANNEL_B);
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

    private static void insertChannel(Connection conn, UUID id, UUID companyId, String externalId)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO omnichannel_channels (id, company_id, type, provider, name, status, external_id)
                VALUES (?, ?, 'WHATSAPP', 'FAKE', 'WhatsApp ' || ?, 'ACTIVE', ?)
                """)) {
            ps.setObject(1, id);
            ps.setObject(2, companyId);
            ps.setString(3, companyId.toString());
            ps.setString(4, externalId);
            ps.executeUpdate();
        }
    }

    private static void insertConversation(Connection conn, UUID id, UUID companyId, UUID channelId)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO omnichannel_conversations (id, company_id, channel_id, external_phone, status)
                VALUES (?, ?, ?, ?, 'OPEN')
                """)) {
            ps.setObject(1, id);
            ps.setObject(2, companyId);
            ps.setObject(3, channelId);
            ps.setString(4, "+550011112222");
            ps.executeUpdate();
        }
    }

    private static void insertMessage(Connection conn, UUID conversationId, UUID companyId, UUID channelId,
                                      String externalId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO omnichannel_messages
                    (id, company_id, conversation_id, channel_id, direction, sender_phone,
                     recipient_phone, type, body, status, external_message_id, client_message_id)
                VALUES (?, ?, ?, ?, 'INBOUND', '+550011112222', 'espaco-a', 'TEXT',
                    'Ola', 'SENT', ?, gen_random_uuid())
                """)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, companyId);
            ps.setObject(3, conversationId);
            ps.setObject(4, channelId);
            ps.setString(5, externalId);
            ps.executeUpdate();
        }
    }

    // ---------------------- RLS / isolamento ------------------------------

    @Test
    void tenantA_shouldOnlySeeOwnChannelsConversationsAndMessages() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        assertEquals(1, countChannels(), "Tenant A deve ver apenas 1 canal (o seu)");
        assertEquals(1, countConversations(), "Tenant A deve ver apenas 1 conversa (a sua)");
        assertEquals(1, countMessages(), "Tenant A deve ver apenas 1 mensagem (a sua)");
    }

    @Test
    void tenantB_shouldNotSeeTenantAData() throws SQLException {
        TenantContext.setCompanyId(TENANT_B);
        assertEquals(1, countChannels(), "Tenant B deve ver apenas 1 canal (o seu)");
        assertEquals(0, countMessages(), "Tenant B não deve ver mensagem de A");
    }

    @Test
    void noContext_shouldSeeNothing() throws SQLException {
        TenantContext.clear();
        assertEquals(0, countChannels());
        assertEquals(0, countConversations());
        assertEquals(0, countMessages());
    }

    @Test
    void crossTenantChannelInsert_shouldBeBlockedByRls() {
        TenantContext.setCompanyId(TENANT_A);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                         INSERT INTO omnichannel_channels (company_id, type, provider, name, external_id)
                         VALUES (?, 'WHATSAPP', 'FAKE', 'Invasao', 'inv')
                         """)) {
                ps.setObject(1, TENANT_B);
                ps.executeUpdate();
            }
        }, "INSERT cross-tenant de canal deve ser bloqueado por RLS");
    }

    @Test
    void crossTenantMessageInsertIntoOwnConversation_shouldBeBlockedByFkChannelOwnership() {
        TenantContext.setCompanyId(TENANT_A);
        // Tenta inserir mensagem de A apontando para conversa de B
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                         INSERT INTO omnichannel_messages
                             (company_id, conversation_id, channel_id, direction, type, body, client_message_id)
                         VALUES (?, ?, ?, 'INBOUND', 'TEXT', 'x', gen_random_uuid())
                         """)) {
                ps.setObject(1, TENANT_A);
                ps.setObject(2, CONV_B);
                ps.setObject(3, CHANNEL_B);
                ps.executeUpdate();
            }
        }, "Mensagem referenciando conversa/canal de outra empresa deve falhar");
    }

    // ---------------------- Idempotência (FASE 17) ------------------------

    @Test
    void duplicateExternalMessageId_shouldViolateUniqueConstraint() {
        TenantContext.setCompanyId(TENANT_A);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                         INSERT INTO omnichannel_messages
                             (company_id, conversation_id, channel_id, direction, sender_phone,
                              recipient_phone, type, body, status, external_message_id, client_message_id)
                         VALUES (?, ?, ?, 'INBOUND', '+550011112222', 'espaco-a', 'TEXT',
                             'Duplicada', 'SENT', ?, gen_random_uuid())
                         """)) {
                ps.setObject(1, TENANT_A);
                ps.setObject(2, CONV_A);
                ps.setObject(3, CHANNEL_A);
                ps.setString(4, EXTERNAL_MSG);
                ps.executeUpdate();
            }
        }, "Mesmo external_message_id (empresa) deve violar a chave única de idempotência");
    }

    @Test
    void duplicateExternalMessageId_insertOnConflictDoNothing_shouldSkip() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     INSERT INTO omnichannel_messages
                         (company_id, conversation_id, channel_id, direction, sender_phone,
                          recipient_phone, type, body, status, external_message_id, client_message_id)
                     VALUES (?, ?, ?, 'INBOUND', '+550011112222', 'espaco-a', 'TEXT',
                         'Duplicada', 'SENT', ?, gen_random_uuid())
                     ON CONFLICT (company_id, external_message_id) DO NOTHING
                     """)) {
            ps.setObject(1, TENANT_A);
            ps.setObject(2, CONV_A);
            ps.setObject(3, CHANNEL_A);
            ps.setString(4, EXTERNAL_MSG);
            int inserted = ps.executeUpdate();
            assertEquals(0, inserted, "Webhook duplicado deve ser ignorado (ON CONFLICT DO NOTHING)");
        }
        assertEquals(1, countMessages(), "Webhook duplicado não pode gerar mensagem duplicada");
    }

    // ---------------------- helpers ---------------------------------------

    private int countChannels() throws SQLException {
        return countOf("SELECT count(*) FROM omnichannel_channels");
    }

    private int countConversations() throws SQLException {
        return countOf("SELECT count(*) FROM omnichannel_conversations");
    }

    private int countMessages() throws SQLException {
        return countOf("SELECT count(*) FROM omnichannel_messages");
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