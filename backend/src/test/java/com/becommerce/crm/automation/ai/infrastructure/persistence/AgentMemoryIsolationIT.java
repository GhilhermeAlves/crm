package com.becommerce.crm.automation.ai.infrastructure.persistence;

import com.becommerce.crm.shared.tenant.context.TenantContext;
import com.becommerce.crm.shared.tenant.datasource.TenantAwareDataSource;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de integração REAL (Testcontainers PostgreSQL 17) da V088: a migração é
 * executada sobre o bootstrap do agente e prova que {@code agent_memory} tem
 * RLS FORCE — memórias da empresa A jamais visíveis/graváveis pela empresa B —
 * e que as novas colunas de perfil do {@code agent_config} nascem com defaults
 * seguros (memória e transferência desligadas, listas vazias).
 */
@Testcontainers
class AgentMemoryIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("crm_it_agent_memory")
            .withUsername("crm_superuser")
            .withPassword("crm_it_pass");

    static final String APP_USER = "crm_memory_app_user";
    static final String APP_PASSWORD = "crm_memory_app_pass";

    static HikariDataSource rawPool;
    static TenantAwareDataSource tenantAwareDataSource;

    static final UUID TENANT_A = UUID.fromString("21111111-2222-3333-4444-555555555551");
    static final UUID TENANT_B = UUID.fromString("21111111-2222-3333-4444-555555555552");
    static final UUID CONTACT_A1 = UUID.fromString("a1000000-0000-0000-0000-000000000001");
    static final UUID CONTACT_A2 = UUID.fromString("a2000000-0000-0000-0000-000000000002");
    static final UUID CONTACT_B1 = UUID.fromString("b1000000-0000-0000-0000-000000000001");

    static UUID agentA;
    static UUID agentB;

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("agent-rls-bootstrap.sql"));
            try (Statement st = conn.createStatement()) {
                st.execute("""
                        CREATE TABLE IF NOT EXISTS contacts (
                            id UUID PRIMARY KEY,
                            company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
                            first_name VARCHAR(100) NOT NULL)
                        """);
                st.execute("CREATE ROLE crm_app NOLOGIN");
            }
            ScriptUtils.executeSqlScript(conn,
                    new ClassPathResource("db/migration/V088__agent_profile_and_memory.sql"));
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
            insertCompany(conn, TENANT_A, "Memory Tenant A", "31.111.111/0001-11", "a.memory@crm.local");
            insertContact(conn, CONTACT_A1, TENANT_A);
            insertContact(conn, CONTACT_A2, TENANT_A);
            agentA = insertAgentConfig(conn, TENANT_A);
            insertMemory(conn, TENANT_A, agentA, CONTACT_A1, "Prefere atendimento pela manhã");
            insertMemory(conn, TENANT_A, agentA, CONTACT_A2, "Interesse em clareamento");
        }
        TenantContext.setCompanyId(TENANT_B);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            insertCompany(conn, TENANT_B, "Memory Tenant B", "32.222.222/0002-22", "b.memory@crm.local");
            insertContact(conn, CONTACT_B1, TENANT_B);
            agentB = insertAgentConfig(conn, TENANT_B);
            insertMemory(conn, TENANT_B, agentB, CONTACT_B1, "Memória de B");
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

    private static void insertContact(Connection conn, UUID id, UUID companyId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO contacts (id, company_id, first_name) VALUES (?, ?, 'Paciente')")) {
            ps.setObject(1, id);
            ps.setObject(2, companyId);
            ps.executeUpdate();
        }
    }

    private static UUID insertAgentConfig(Connection conn, UUID companyId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO agent_config (company_id, ai_enabled, allow_auto_reply, system_prompt,
                    cooldown_minutes, max_chars)
                VALUES (?, TRUE, TRUE, 'Prompt legado', 60, 1000) RETURNING id
                """)) {
            ps.setObject(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getObject(1, UUID.class);
            }
        }
    }

    private static void insertMemory(Connection conn, UUID companyId, UUID agentId, UUID contactId,
                                     String content) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO agent_memory (company_id, agent_config_id, contact_id, memory_type, content)
                VALUES (?, ?, ?, 'PREFERENCE', ?)
                """)) {
            ps.setObject(1, companyId);
            ps.setObject(2, agentId);
            ps.setObject(3, contactId);
            ps.setString(4, content);
            ps.executeUpdate();
        }
    }

    @Test
    void tenantA_seesOnlyOwnMemories() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        assertEquals(2, count("SELECT count(*) FROM agent_memory"));
        assertEquals(0, count("SELECT count(*) FROM agent_memory WHERE contact_id = '" + CONTACT_B1 + "'"));
    }

    @Test
    void tenantB_doesNotSeeTenantAMemories() throws SQLException {
        TenantContext.setCompanyId(TENANT_B);
        assertEquals(1, count("SELECT count(*) FROM agent_memory"));
        assertEquals(0, count("SELECT count(*) FROM agent_memory WHERE contact_id = '" + CONTACT_A1 + "'"));
    }

    @Test
    void noTenant_seesNothing() throws SQLException {
        TenantContext.clear();
        assertEquals(0, count("SELECT count(*) FROM agent_memory"));
    }

    @Test
    void crossTenantInsert_isBlockedByRls() {
        TenantContext.setCompanyId(TENANT_A);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection()) {
                insertMemory(conn, TENANT_B, agentB, CONTACT_B1, "Invasão");
            }
        });
    }

    @Test
    void crossTenantUpdateAndDelete_affectNothing() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        assertEquals(0, execute("UPDATE agent_memory SET content = 'x' WHERE company_id = '" + TENANT_B + "'"));
        assertEquals(0, execute("DELETE FROM agent_memory WHERE company_id = '" + TENANT_B + "'"));
        TenantContext.setCompanyId(TENANT_B);
        assertEquals(1, count("SELECT count(*) FROM agent_memory WHERE content = 'Memória de B'"));
    }

    @Test
    void invalidTypeOrImportance_isRejectedByConstraints() {
        TenantContext.setCompanyId(TENANT_A);
        assertThrows(SQLException.class, () -> execute("INSERT INTO agent_memory (company_id, agent_config_id, "
                + "contact_id, memory_type, content) VALUES ('" + TENANT_A + "', '" + agentA + "', '" + CONTACT_A1
                + "', 'HISTORY', 'x')"));
        assertThrows(SQLException.class, () -> execute("INSERT INTO agent_memory (company_id, agent_config_id, "
                + "contact_id, memory_type, content, importance) VALUES ('" + TENANT_A + "', '" + agentA + "', '"
                + CONTACT_A1 + "', 'FACT', 'x', 9)"));
    }

    @Test
    void agentConfigProfileColumns_haveSafeDefaults() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT memory_enabled, human_transfer_enabled, rules::text, "
                     + "instructions::text, persona, system_prompt FROM agent_config")) {
            assertTrue(rs.next());
            assertEquals(false, rs.getBoolean(1));
            assertEquals(false, rs.getBoolean(2));
            assertEquals("[]", rs.getString(3));
            assertEquals("[]", rs.getString(4));
            assertEquals(null, rs.getString(5));
            assertEquals("Prompt legado", rs.getString(6), "o prompt legado é preservado");
        }
    }

    private int count(String sql) throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int execute(String sql) throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement()) {
            return st.executeUpdate(sql);
        }
    }
}
