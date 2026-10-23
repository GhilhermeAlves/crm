package com.becommerce.crm.clinical.budget.infrastructure.persistence;

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
 * Isolamento de tenant dos orçamentos aplicando as migrations REAIS
 * {@code V106__patient_procedures.sql} e {@code V107__budgets.sql} (Testcontainers
 * PostgreSQL 17, role NOBYPASSRLS como o {@code crm_app}): A e B isolados nas três
 * tabelas, sem contexto nada visível, escrita cross-tenant barrada e CHECKs de
 * status, parcelas e aprovação.
 */
@Testcontainers
class BudgetIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("crm_it_budget")
            .withUsername("crm_superuser")
            .withPassword("crm_it_pass");

    static final String APP_USER = "crm_budget_app_user";
    static final String APP_PASSWORD = "crm_budget_app_pass";

    static HikariDataSource rawPool;
    static TenantAwareDataSource tenantAwareDataSource;

    static final UUID TENANT_A = UUID.fromString("55555555-6666-7777-8888-999999999991");
    static final UUID TENANT_B = UUID.fromString("55555555-6666-7777-8888-999999999992");

    static final UUID CONTACT_A = UUID.fromString("88888888-0000-0000-0000-00000000000a");
    static final UUID CONTACT_B = UUID.fromString("88888888-0000-0000-0000-00000000000b");
    static final UUID ITEM_A = UUID.fromString("99999999-0000-0000-0000-00000000000a");
    static final UUID ITEM_B = UUID.fromString("99999999-0000-0000-0000-00000000000b");
    static final UUID TX_A = UUID.fromString("aaaaaaaa-0000-0000-0000-00000000000a");
    static final UUID TX_B = UUID.fromString("aaaaaaaa-0000-0000-0000-00000000000b");

    static UUID budgetA;
    static UUID budgetB;

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("agent-rls-bootstrap.sql"));
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE ROLE crm_app NOLOGIN NOBYPASSRLS");
                // Tabelas mínimas referenciadas pelas FKs da V106/V107.
                st.execute("CREATE TABLE contacts (id UUID PRIMARY KEY)");
                st.execute("CREATE TABLE users (id UUID PRIMARY KEY)");
                st.execute("CREATE TABLE catalog_items (id UUID PRIMARY KEY)");
                st.execute("CREATE TABLE financial_transactions (id UUID PRIMARY KEY)");
            }
            insertCompany(conn, TENANT_A, "Orcamento A LTDA", "63.111.111/0001-11");
            insertCompany(conn, TENANT_B, "Orcamento B LTDA", "64.222.222/0002-22");
            try (Statement st = conn.createStatement()) {
                st.execute("INSERT INTO contacts (id) VALUES ('" + CONTACT_A + "'), ('" + CONTACT_B + "')");
                st.execute("INSERT INTO catalog_items (id) VALUES ('" + ITEM_A + "'), ('" + ITEM_B + "')");
                st.execute("INSERT INTO financial_transactions (id) VALUES ('" + TX_A + "'), ('" + TX_B + "')");
            }
            // Cada arquivo inteiro num único execute (como o Flyway): o ScriptUtils divide em ';'
            // e quebraria o bloco DO $$ ... $$ de RLS/GRANT.
            for (String migration : new String[] {"V106__patient_procedures.sql", "V107__budgets.sql"}) {
                try (Statement st = conn.createStatement()) {
                    st.execute(new ClassPathResource("db/migration/" + migration)
                            .getContentAsString(StandardCharsets.UTF_8));
                }
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

        budgetA = seedBudget(TENANT_A, CONTACT_A, ITEM_A, TX_A, "Orçamento A");
        budgetB = seedBudget(TENANT_B, CONTACT_B, ITEM_B, TX_B, "Orçamento B");
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

    private static UUID seedBudget(UUID tenant, UUID contact, UUID item, UUID tx, String title)
            throws SQLException {
        TenantContext.setCompanyId(tenant);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            UUID procedure = insertProcedure(conn, tenant, contact, item, title);
            UUID budget = insertBudget(conn, tenant, contact, title);
            insertBudgetItem(conn, tenant, budget, procedure);
            insertBudgetTransaction(conn, tenant, budget, tx);
            return budget;
        } finally {
            TenantContext.clear();
        }
    }

    private static UUID insertProcedure(Connection conn, UUID tenant, UUID contact, UUID item, String name)
            throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO patient_procedures (id, company_id, contact_id, catalog_item_id,
                    name_snapshot, state, dentition)
                VALUES (?, ?, ?, ?, ?, 'PLANNED', 'PERMANENT')""")) {
            ps.setObject(1, id);
            ps.setObject(2, tenant);
            ps.setObject(3, contact);
            ps.setObject(4, item);
            ps.setString(5, name);
            ps.executeUpdate();
        }
        return id;
    }

    private static UUID insertBudget(Connection conn, UUID tenant, UUID contact, String title)
            throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO budgets (id, company_id, contact_id, title)
                VALUES (?, ?, ?, ?)""")) {
            ps.setObject(1, id);
            ps.setObject(2, tenant);
            ps.setObject(3, contact);
            ps.setString(4, title);
            ps.executeUpdate();
        }
        return id;
    }

    private static void insertBudgetItem(Connection conn, UUID tenant, UUID budget, UUID procedure)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO budget_items (company_id, budget_id, procedure_id, price_cents, position)
                VALUES (?, ?, ?, 10000, 0)""")) {
            ps.setObject(1, tenant);
            ps.setObject(2, budget);
            ps.setObject(3, procedure);
            ps.executeUpdate();
        }
    }

    private static void insertBudgetTransaction(Connection conn, UUID tenant, UUID budget, UUID tx)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO budget_transactions (budget_id, transaction_id, company_id)
                VALUES (?, ?, ?)""")) {
            ps.setObject(1, budget);
            ps.setObject(2, tx);
            ps.setObject(3, tenant);
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

    private static String visibleTitles() throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT string_agg(title, ',' ORDER BY title) FROM budgets")) {
            rs.next();
            return rs.getString(1);
        }
    }

    private static void updateBudgetA(String setClause) {
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE budgets SET " + setClause + " WHERE id = ?")) {
                ps.setObject(1, budgetA);
                ps.executeUpdate();
            }
        });
    }

    @Test
    void eachTenantSeesOnlyItsOwnBudgets() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        assertEquals("Orçamento A", visibleTitles());
        TenantContext.setCompanyId(TENANT_B);
        assertEquals("Orçamento B", visibleTitles());
    }

    @Test
    void eachTenantSeesOnlyItsOwnItemsAndTransactions() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        assertEquals(1, count("budget_items"));
        assertEquals(1, count("budget_transactions"));
        TenantContext.setCompanyId(TENANT_B);
        assertEquals(1, count("budget_items"));
        assertEquals(1, count("budget_transactions"));
    }

    @Test
    void noTenantContext_seesNothing() throws SQLException {
        assertEquals(null, visibleTitles());
        assertEquals(0, count("budget_items"));
        assertEquals(0, count("budget_transactions"));
    }

    @Test
    void tenantB_cannotInsertIntoTenantA() {
        TenantContext.setCompanyId(TENANT_B);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection()) {
                insertBudget(conn, TENANT_A, CONTACT_A, "Intruso");
            }
        });
    }

    @Test
    void invalidStatusIsRejected() {
        TenantContext.setCompanyId(TENANT_A);
        updateBudgetA("status = 'INVALID'");
    }

    @Test
    void installmentsAboveLimitAreRejected() {
        TenantContext.setCompanyId(TENANT_A);
        updateBudgetA("installments = 25");
    }

    @Test
    void approvedRequiresApprovedAt() {
        TenantContext.setCompanyId(TENANT_A);
        updateBudgetA("status = 'APPROVED', approved_at = NULL");
    }
}
