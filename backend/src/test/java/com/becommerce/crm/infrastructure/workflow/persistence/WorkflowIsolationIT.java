package com.becommerce.crm.infrastructure.workflow.persistence;

import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import com.becommerce.crm.infrastructure.tenant.datasource.TenantAwareDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Teste de integração REAL (Testcontainers PostgreSQL 17 + HikariCP) que prova:
 * (1) isolamento cross-tenant via RLS FORCE nas tabelas de workflow (V041);
 * (2) idempotência por chave única (company_id, workflow_action_id, event_id),
 * garante que o mesmo evento + mesma ação não gera execução duplicada (Item 6).
 */
@Testcontainers
class WorkflowIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("crm_it_workflow")
            .withUsername("crm_superuser")
            .withPassword("crm_it_pass");

    static final String APP_USER = "crm_app_user";
    static final String APP_PASSWORD = "crm_app_pass";

    static HikariDataSource rawPool;
    static TenantAwareDataSource tenantAwareDataSource;

    static final UUID TENANT_A = UUID.fromString("11111111-2222-3333-4444-555555555551");
    static final UUID TENANT_B = UUID.fromString("11111111-2222-3333-4444-555555555552");

    static final UUID CONTACT_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    static final UUID WORKFLOW_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000061");
    static final UUID ACTION_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000071");
    static final UUID EVENT_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000081");
    static final UUID EXEC_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000091");

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("workflow-rls-bootstrap.sql"));
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
            insertCompany(conn, TENANT_A, "Workflow Tenant A LTDA", "13.131.131/0001-13", "a.wf@crm.local");
            insertContact(conn, CONTACT_A, TENANT_A, "Ana", "Fluxo", "ana.wf@a.com");
            insertWorkflow(conn, WORKFLOW_A, TENANT_A, "Follow-up proposta A", "OPPORTUNITY_STAGE_CHANGED");
            insertAction(conn, ACTION_A, TENANT_A, WORKFLOW_A);
            insertExecution(conn, EXEC_A, TENANT_A, WORKFLOW_A, ACTION_A, EVENT_ID);
        }

        TenantContext.setCompanyId(TENANT_B);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            insertCompany(conn, TENANT_B, "Workflow Tenant B LTDA", "24.242.242/0002-24", "b.wf@crm.local");
            insertContact(conn, UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002"), TENANT_B, "Bruno", "Fluxo", "bruno.wf@b.com");
            insertWorkflow(conn, UUID.fromString("bbbbbbbb-0000-0000-0000-000000000061"), TENANT_B, "Follow-up proposta B", "OPPORTUNITY_STAGE_CHANGED");
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

    private static void insertContact(Connection conn, UUID id, UUID companyId, String first, String last,
                                      String email) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO contacts (id, company_id, first_name, last_name, email)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            ps.setObject(1, id);
            ps.setObject(2, companyId);
            ps.setString(3, first);
            ps.setString(4, last);
            ps.setString(5, email);
            ps.executeUpdate();
        }
    }

    private static void insertWorkflow(Connection conn, UUID id, UUID companyId, String name, String trigger)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO workflows (id, company_id, name, trigger, active)
                VALUES (?, ?, ?, ?, TRUE)
                """)) {
            ps.setObject(1, id);
            ps.setObject(2, companyId);
            ps.setString(3, name);
            ps.setString(4, trigger);
            ps.executeUpdate();
        }
    }

    private static void insertAction(Connection conn, UUID id, UUID companyId, UUID workflowId)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO workflow_actions (id, company_id, workflow_id, action_type, sort_order, config)
                VALUES (?, ?, ?, 'CREATE_TASK', 0, '{"title":"Follow-up"}')
                """)) {
            ps.setObject(1, id);
            ps.setObject(2, companyId);
            ps.setObject(3, workflowId);
            ps.executeUpdate();
        }
    }

    private static void insertExecution(Connection conn, UUID id, UUID companyId, UUID workflowId,
                                        UUID actionId, UUID eventId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO workflow_executions
                    (id, company_id, workflow_id, workflow_action_id, event_id,
                     event_type, action_type, status, result_text)
                VALUES (?, ?, ?, ?, ?, 'OPPORTUNITY_STAGE_CHANGED', 'CREATE_TASK', 'SUCCESS', 'Task criada')
                """)) {
            ps.setObject(1, id);
            ps.setObject(2, companyId);
            ps.setObject(3, workflowId);
            ps.setObject(4, actionId);
            ps.setObject(5, eventId);
            ps.executeUpdate();
        }
    }

    @Test
    void tenantA_shouldOnlySeeOwnWorkflows() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        assertEquals(1, countWorkflows(), "Tenant A deve ver apenas 1 workflow (o seu)");
    }

    @Test
    void tenantB_shouldOnlySeeOwnActivity() throws SQLException {
        TenantContext.setCompanyId(TENANT_B);
        assertEquals(1, countWorkflows(), "Tenant B deve ver apenas 1 workflow (o seu)");
    }

    @Test
    void noContext_shouldSeeNoWorkflowsOrExecutions() throws SQLException {
        TenantContext.clear();
        assertEquals(0, countWorkflows(), "Sem contexto não deve ver workflows");
        assertEquals(0, countExecutions(), "Sem contexto não deve ver execuções");
    }

    @Test
    void crossTenantWorkflowInsert_shouldBeBlockedByRLS() {
        TenantContext.setCompanyId(TENANT_A);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                         INSERT INTO workflows (company_id, name, trigger)
                         VALUES (?, 'Invasão', 'OPPORTUNITY_WON')
                         """)) {
                ps.setObject(1, TENANT_B);
                ps.executeUpdate();
            }
        }, "INSERT cross-tenant de workflow deve lançar exceção de RLS");
    }

    @Test
    void duplicateExecution_sameActionAndEvent_shouldThrowUniqueViolation() {
        TenantContext.setCompanyId(TENANT_A);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                         INSERT INTO workflow_executions
                             (id, company_id, workflow_action_id, event_id,
                              event_type, action_type, status)
                         VALUES (?, ?, ?, ?, 'OPPORTUNITY_WON', 'CREATE_TASK', 'SUCCESS')
                         """)) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, TENANT_A);
                ps.setObject(3, ACTION_A);
                ps.setObject(4, EVENT_ID);
                ps.executeUpdate();
            }
        }, "Mesmo (company, action, event) deve violar a chave única de idempotência");
    }

    @Test
    void duplicateExecution_insertOnConflictDoNothing_shouldSkip() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     INSERT INTO workflow_executions
                         (id, company_id, workflow_action_id, event_id,
                          event_type, action_type, status)
                     VALUES (?, ?, ?, ?, 'OPPORTUNITY_WON', 'CREATE_TASK', 'SUCCESS')
                     ON CONFLICT (company_id, workflow_action_id, event_id) DO NOTHING
                     """)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, TENANT_A);
            ps.setObject(3, ACTION_A);
            ps.setObject(4, EVENT_ID);
            int inserted = ps.executeUpdate();
            assertEquals(0, inserted, "ON CONFLICT DO NOTHING deve pular (não inserir duplicada)");
        }
    }

    private int countWorkflows() throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM workflows")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int countExecutions() throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM workflow_executions")) {
            rs.next();
            return rs.getInt(1);
        }
    }
}