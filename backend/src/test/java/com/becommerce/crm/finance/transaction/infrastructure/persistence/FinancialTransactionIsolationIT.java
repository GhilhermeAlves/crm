package com.becommerce.crm.finance.transaction.infrastructure.persistence;

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

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Isolamento de tenant dos lançamentos financeiros aplicando a migration REAL
 * {@code V092__financial_transactions.sql} (Testcontainers PostgreSQL 17, role
 * NOBYPASSRLS como o {@code crm_app}): A e B isolados, sem contexto nada visível,
 * escrita cross-tenant barrada e permissões do módulo concedidas aos papéis certos.
 */
@Testcontainers
class FinancialTransactionIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("crm_it_finance")
            .withUsername("crm_superuser")
            .withPassword("crm_it_pass");

    static final String APP_USER = "crm_finance_app_user";
    static final String APP_PASSWORD = "crm_finance_app_pass";

    static HikariDataSource rawPool;
    static TenantAwareDataSource tenantAwareDataSource;

    static final UUID TENANT_A = UUID.fromString("44444444-5555-6666-7777-888888888881");
    static final UUID TENANT_B = UUID.fromString("44444444-5555-6666-7777-888888888882");

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("agent-rls-bootstrap.sql"));
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE ROLE crm_app NOLOGIN NOBYPASSRLS");
                // Tabelas mínimas referenciadas pelas FKs da V092.
                st.execute("CREATE TABLE contacts (id UUID PRIMARY KEY)");
                st.execute("CREATE TABLE users (id UUID PRIMARY KEY)");
                st.execute("CREATE TABLE catalog_items (id UUID PRIMARY KEY)");
                // Mínimo do RBAC que a V092 alimenta (permissões + vínculo papel -> permissão).
                st.execute("""
                        CREATE TABLE permissions (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            name VARCHAR(100) UNIQUE NOT NULL, description TEXT, module VARCHAR(50),
                            resource VARCHAR(50), action VARCHAR(50))""");
                st.execute("CREATE TABLE roles (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), name VARCHAR(50))");
                st.execute("""
                        CREATE TABLE role_permissions (role_id UUID, permission_id UUID,
                            PRIMARY KEY (role_id, permission_id))""");
            }
            insertCompany(conn, TENANT_A, "Finance A LTDA", "51.111.111/0001-11");
            insertCompany(conn, TENANT_B, "Finance B LTDA", "52.222.222/0002-22");
            try (Statement st = conn.createStatement()) {
                st.execute("INSERT INTO roles (name) VALUES ('ADMIN'), ('MANAGER'), ('AGENT'), ('VIEWER')");
            }
            // Arquivo inteiro num único execute (como o Flyway): o ScriptUtils divide em ';'
            // e quebraria o bloco DO $$ ... $$ das permissões.
            try (Statement st = conn.createStatement()) {
                st.execute(new ClassPathResource("db/migration/V092__financial_transactions.sql")
                        .getContentAsString(StandardCharsets.UTF_8));
            }
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE ROLE " + APP_USER + " LOGIN PASSWORD '" + APP_PASSWORD
                        + "' NOSUPERUSER NOBYPASSRLS");
                st.execute("GRANT crm_app TO " + APP_USER);
                st.execute("GRANT USAGE ON SCHEMA public TO " + APP_USER);
                st.execute("GRANT USAGE ON SCHEMA app TO " + APP_USER);
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

        seedTransaction(TENANT_A, "Receita A");
        seedTransaction(TENANT_B, "Despesa B");
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

    private static void insertCompany(Connection conn, UUID id, String name, String cnpj) throws SQLException {
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
            ps.setString(5, id + "@crm.local");
            ps.executeUpdate();
        }
    }

    private static void seedTransaction(UUID tenant, String description) throws SQLException {
        TenantContext.setCompanyId(tenant);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            insertTransaction(conn, tenant, description);
        } finally {
            TenantContext.clear();
        }
    }

    private static void insertTransaction(Connection conn, UUID tenant, String description) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO financial_transactions (company_id, type, description, category, amount_cents,
                    payment_method, due_date) VALUES (?, 'INCOME', ?, 'PROCEDURE', 10000, 'PIX', DATE '2026-01-15')
                """)) {
            ps.setObject(1, tenant);
            ps.setString(2, description);
            ps.executeUpdate();
        }
    }

    private static String visibleDescriptions() throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT string_agg(description, ',' ORDER BY description) FROM financial_transactions")) {
            rs.next();
            return rs.getString(1);
        }
    }

    @Test
    void eachTenantSeesOnlyItsOwnTransactions() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        assertEquals("Receita A", visibleDescriptions());
        TenantContext.setCompanyId(TENANT_B);
        assertEquals("Despesa B", visibleDescriptions());
    }

    @Test
    void noTenantContext_seesNothing() throws SQLException {
        assertEquals(null, visibleDescriptions());
    }

    @Test
    void tenantB_cannotInsertIntoTenantA() {
        TenantContext.setCompanyId(TENANT_B);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection()) {
                insertTransaction(conn, TENANT_A, "Intruso");
            }
        });
    }

    @Test
    void permissionsGrantedToExpectedRoles() throws SQLException {
        try (Connection conn = postgres.createConnection("");
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("""
                     SELECT string_agg(r.name || ':' || p.name, ',' ORDER BY r.name, p.name)
                     FROM role_permissions rp JOIN roles r ON r.id = rp.role_id
                     JOIN permissions p ON p.id = rp.permission_id
                     WHERE p.name LIKE 'finance:%'""")) {
            rs.next();
            assertEquals("ADMIN:finance:manage,ADMIN:finance:read,MANAGER:finance:manage,MANAGER:finance:read",
                    rs.getString(1));
        }
    }
}
