package com.becommerce.crm.infrastructure.activity.persistence;

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
 * Teste de integração REAL (Testcontainers PostgreSQL 17 + HikariCP) que prova
 * o isolamento cross-tenant via RLS FORCE nas tabelas {@code activities} e
 * {@code tasks} (Sprint 12, V039). Mesmo fluxo do {@code LeadIsolationIT} /
 * {@code PipelineIsolationIT}: usuário não-bypass + {@code TenantAwareDataSource}
 * + {@code TenantContext}, garantia de segurança equivalente ao runtime.
 */
@Testcontainers
class ActivityTaskIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("crm_it_activity")
            .withUsername("crm_superuser")
            .withPassword("crm_it_pass");

    static final String APP_USER = "crm_app_user";
    static final String APP_PASSWORD = "crm_app_pass";

    static HikariDataSource rawPool;
    static TenantAwareDataSource tenantAwareDataSource;

    static final UUID TENANT_A = UUID.fromString("11111111-2222-3333-4444-555555555551");
    static final UUID TENANT_B = UUID.fromString("11111111-2222-3333-4444-555555555552");

    static final UUID CONTACT_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    static final UUID CONTACT_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    static final UUID ACTIVITY_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000041");
    static final UUID ACTIVITY_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000041");
    static final UUID TASK_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000051");
    static final UUID TASK_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000051");

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("activity-task-rls-bootstrap.sql"));

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
            insertCompany(conn, TENANT_A, "Activity Tenant A LTDA", "11.111.111/0001-11", "a.act@crm.local");
            insertContact(conn, CONTACT_A, TENANT_A, "Ana", "Atividade", "ana@a.com");
            insertActivity(conn, ACTIVITY_A, TENANT_A, CONTACT_A, "Ligação A");
            insertTask(conn, TASK_A, TENANT_A, CONTACT_A, "Follow-up A");
        }

        TenantContext.setCompanyId(TENANT_B);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            insertCompany(conn, TENANT_B, "Activity Tenant B LTDA", "22.222.222/0002-22", "b.act@crm.local");
            insertContact(conn, CONTACT_B, TENANT_B, "Bruno", "Atividade", "bruno@b.com");
            insertActivity(conn, ACTIVITY_B, TENANT_B, CONTACT_B, "Ligação B");
            insertTask(conn, TASK_B, TENANT_B, CONTACT_B, "Follow-up B");
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

    private static void insertActivity(Connection conn, UUID id, UUID companyId, UUID contactId, String subject)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO activities (id, company_id, contact_id, type, subject)
                VALUES (?, ?, ?, 'CALL', ?)
                """)) {
            ps.setObject(1, id);
            ps.setObject(2, companyId);
            ps.setObject(3, contactId);
            ps.setString(4, subject);
            ps.executeUpdate();
        }
    }

    private static void insertTask(Connection conn, UUID id, UUID companyId, UUID contactId, String title)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO tasks (id, company_id, contact_id, title)
                VALUES (?, ?, ?, ?)
                """)) {
            ps.setObject(1, id);
            ps.setObject(2, companyId);
            ps.setObject(3, contactId);
            ps.setString(4, title);
            ps.executeUpdate();
        }
    }

    @Test
    void tenantA_shouldOnlySeeOwnActivities() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        assertEquals(1, countActivities(), "Tenant A deve ver apenas 1 activity (a sua)");
    }

    @Test
    void tenantB_shouldOnlySeeOwnTasks() throws SQLException {
        TenantContext.setCompanyId(TENANT_B);
        assertEquals(1, countTasks(), "Tenant B deve ver apenas 1 task (a sua)");
    }

    @Test
    void noContext_shouldSeeNoActivitiesOrTasks() throws SQLException {
        TenantContext.clear();
        assertEquals(0, countActivities(), "Sem contexto não deve ver activities");
        assertEquals(0, countTasks(), "Sem contexto não deve ver tasks");
    }

    @Test
    void crossTenantActivityInsert_shouldBeBlockedByRLS() {
        TenantContext.setCompanyId(TENANT_A);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                         INSERT INTO activities (company_id, contact_id, type, subject)
                         VALUES (?, ?, 'CALL', 'Invasão')
                         """)) {
                ps.setObject(1, TENANT_B);
                ps.setObject(2, CONTACT_B);
                ps.executeUpdate();
            }
        }, "INSERT cross-tenant de activity deve lançar exceção de RLS");
    }

    @Test
    void crossTenantTaskUpdate_shouldAffectZeroRows() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE tasks SET status = 'COMPLETED' WHERE company_id = ?")) {
            ps.setObject(1, TENANT_B);
            int updated = ps.executeUpdate();
            assertEquals(0, updated, "UPDATE cross-tenant de task deve afetar 0 linhas");
        }
    }

    @Test
    void crossTenantTaskDelete_shouldAffectZeroRows() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM tasks WHERE company_id = ?")) {
            ps.setObject(1, TENANT_B);
            int deleted = ps.executeUpdate();
            assertEquals(0, deleted, "DELETE cross-tenant de task deve afetar 0 linhas");
        }
    }

    @Test
    void sameTenantInsert_shouldWork() throws SQLException {
        UUID extraActivity = UUID.fromString("cccccccc-0000-0000-0000-000000000006");
        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            insertActivity(conn, extraActivity, TENANT_A, CONTACT_A, "Nova atividade");
        }
        assertEquals(2, countActivities(), "Tenant A deve ver sua nova activity na mesma empresa");
    }

    private int countActivities() throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM activities")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int countTasks() throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM tasks")) {
            rs.next();
            return rs.getInt(1);
        }
    }
}