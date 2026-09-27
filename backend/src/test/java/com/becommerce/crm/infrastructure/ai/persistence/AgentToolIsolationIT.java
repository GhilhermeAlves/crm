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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Isolamento de tenant das tabelas do agente com ferramentas, aplicando a
 * migration REAL {@code V077__CreateAgentToolsAndAudit.sql} (Testcontainers
 * PostgreSQL 17, role NOBYPASSRLS como o {@code crm_app} de produção):
 * (1) {@code agent_tool} e {@code agent_action_audit} de A invisíveis para B;
 * (2) {@code tool_permission} herda o tenant da ferramenta;
 * (3) sem contexto de tenant nada é visível; B não grava linha com company_id de A.
 */
@Testcontainers
class AgentToolIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("crm_it_agent_tools")
            .withUsername("crm_superuser")
            .withPassword("crm_it_pass");

    static final String APP_USER = "crm_tools_app_user";
    static final String APP_PASSWORD = "crm_tools_app_pass";

    static HikariDataSource rawPool;
    static TenantAwareDataSource tenantAwareDataSource;

    static final UUID TENANT_A = UUID.fromString("22222222-3333-4444-5555-666666666661");
    static final UUID TENANT_B = UUID.fromString("22222222-3333-4444-5555-666666666662");
    static final UUID CONV_A = UUID.fromString("aaaaaaaa-1111-0000-0000-000000000031");
    static final UUID CONV_B = UUID.fromString("bbbbbbbb-1111-0000-0000-000000000032");
    static final UUID TOOL_A = UUID.fromString("cccccccc-1111-0000-0000-000000000041");
    static final UUID TOOL_B = UUID.fromString("dddddddd-1111-0000-0000-000000000042");

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("agent-rls-bootstrap.sql"));
            try (Statement st = conn.createStatement()) {
                // A V077 concede privilégios a crm_app (role de produção).
                st.execute("CREATE ROLE crm_app NOLOGIN NOBYPASSRLS");
            }
            ScriptUtils.executeSqlScript(conn,
                    new ClassPathResource("db/migration/V077__CreateAgentToolsAndAudit.sql"));
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE ROLE " + APP_USER + " LOGIN PASSWORD '" + APP_PASSWORD
                        + "' NOSUPERUSER NOBYPASSRLS");
                st.execute("GRANT crm_app TO " + APP_USER);
                st.execute("GRANT USAGE ON SCHEMA public TO " + APP_USER);
                st.execute("GRANT USAGE ON SCHEMA app TO " + APP_USER);
                st.execute("GRANT SELECT, INSERT ON companies, omnichannel_conversations TO " + APP_USER);
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

        seed(TENANT_A, CONV_A, TOOL_A, "Tools Tenant A LTDA", "31.111.111/0001-11");
        seed(TENANT_B, CONV_B, TOOL_B, "Tools Tenant B LTDA", "32.222.222/0002-22");
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

    private static void seed(UUID tenant, UUID conv, UUID tool, String name, String cnpj) throws SQLException {
        TenantContext.setCompanyId(tenant);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            exec(conn, """
                    INSERT INTO companies (id, legal_name, trading_name, cnpj, email, phone,
                        address_zip_code, address_street, address_number, address_neighborhood,
                        address_city, address_state, address_country, plan, status,
                        max_users, max_storage_mb)
                    VALUES (?, ?, ?, ?, ?, '0000-0000', '00000-000', 'Rua Teste', '0', 'Centro',
                        'Sao Paulo', 'SP', 'Brasil', 'STARTER', 'ACTIVE', 10, 1024)
                    """, tenant, name, name, cnpj, tenant + "@crm.local");
            exec(conn, """
                    INSERT INTO omnichannel_conversations (id, company_id, external_phone, status)
                    VALUES (?, ?, '+550011112222', 'OPEN')
                    """, conv, tenant);
            exec(conn, "INSERT INTO agent_tool (id, company_id, tool_name, tool_type) VALUES (?, ?, 'fetchContact', 'READ_ONLY')",
                    tool, tenant);
            exec(conn, "INSERT INTO tool_permission (tool_id, role, allowed) VALUES (?, 'ADMIN', TRUE)", tool);
            exec(conn, """
                    INSERT INTO agent_action_audit (company_id, conversation_id, tool_name, input, outcome)
                    VALUES (?, ?, 'fetchContact', '{"phone":"+5511"}'::jsonb, 'SUCCESS')
                    """, tenant, conv);
        } finally {
            TenantContext.clear();
        }
    }

    private static void exec(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        }
    }

    private static int count(String table) throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Test
    void eachTenantSeesOnlyItsOwnRows() throws SQLException {
        for (UUID tenant : new UUID[] {TENANT_A, TENANT_B}) {
            TenantContext.setCompanyId(tenant);
            assertEquals(1, count("agent_tool"), "agent_tool de " + tenant);
            assertEquals(1, count("tool_permission"), "tool_permission de " + tenant);
            assertEquals(1, count("agent_action_audit"), "agent_action_audit de " + tenant);
        }
    }

    @Test
    void noTenantContext_seesNothing() throws SQLException {
        assertEquals(0, count("agent_tool"));
        assertEquals(0, count("tool_permission"));
        assertEquals(0, count("agent_action_audit"));
    }

    @Test
    void tenantB_cannotWriteRowsForTenantA() {
        TenantContext.setCompanyId(TENANT_B);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection()) {
                exec(conn, "INSERT INTO agent_tool (company_id, tool_name, tool_type) VALUES (?, 'x', 'READ_ONLY')",
                        TENANT_A);
            }
        });
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection()) {
                exec(conn, "INSERT INTO tool_permission (tool_id, role) VALUES (?, 'AGENT')", TOOL_A);
            }
        });
    }

    @Test
    void auditIsAppendOnlyForAppRole() {
        TenantContext.setCompanyId(TENANT_A);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection()) {
                exec(conn, "UPDATE agent_action_audit SET outcome = 'TAMPERED'");
            }
        });
    }
}
