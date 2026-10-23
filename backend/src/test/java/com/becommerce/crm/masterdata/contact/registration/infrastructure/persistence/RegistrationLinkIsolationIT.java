package com.becommerce.crm.masterdata.contact.registration.infrastructure.persistence;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RLS das tabelas novas da migration REAL {@code V108} ({@code appointment_procedures}
 * e {@code patient_registration_links}) com role NOBYPASSRLS como o {@code crm_app}:
 * isolamento por tenant e o acesso público por token (sem tenant), que libera só a
 * linha do token informado via {@code app.set_registration_token_context}.
 */
@Testcontainers
class RegistrationLinkIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("crm_it_registration")
            .withUsername("crm_superuser")
            .withPassword("crm_it_pass");

    static final String APP_USER = "crm_registration_app_user";
    static final String APP_PASSWORD = "crm_registration_app_pass";

    static HikariDataSource rawPool;
    static TenantAwareDataSource tenantAwareDataSource;

    static final UUID TENANT_A = UUID.fromString("66666666-7777-8888-9999-aaaaaaaaaaa1");
    static final UUID TENANT_B = UUID.fromString("66666666-7777-8888-9999-aaaaaaaaaaa2");
    static final UUID CONTACT_A = UUID.fromString("77777777-0000-0000-0000-00000000000a");
    static final UUID CONTACT_B = UUID.fromString("77777777-0000-0000-0000-00000000000b");
    static final UUID APPT_A = UUID.fromString("abababab-0000-0000-0000-00000000000a");
    static final UUID APPT_B = UUID.fromString("abababab-0000-0000-0000-00000000000b");
    static final UUID ITEM_A = UUID.fromString("cdcdcdcd-0000-0000-0000-00000000000a");
    static final UUID ITEM_B = UUID.fromString("cdcdcdcd-0000-0000-0000-00000000000b");

    static final String HASH_A = "a".repeat(64);
    static final String HASH_B = "b".repeat(64);

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("agent-rls-bootstrap.sql"));
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE ROLE crm_app NOLOGIN NOBYPASSRLS");
                // Tabelas mínimas referenciadas (FK/ALTER) pela V108.
                st.execute("CREATE TABLE contacts (id UUID PRIMARY KEY)");
                st.execute("CREATE TABLE catalog_items (id UUID PRIMARY KEY)");
                st.execute("CREATE TABLE appointments (id UUID PRIMARY KEY)");
                st.execute("CREATE TABLE schedule_blocks (id UUID PRIMARY KEY)");
            }
            insertCompany(conn, TENANT_A, "Cadastro A LTDA", "65.111.111/0001-11");
            insertCompany(conn, TENANT_B, "Cadastro B LTDA", "66.222.222/0002-22");
            try (Statement st = conn.createStatement()) {
                st.execute("INSERT INTO contacts (id) VALUES ('" + CONTACT_A + "'), ('" + CONTACT_B + "')");
                st.execute("INSERT INTO catalog_items (id) VALUES ('" + ITEM_A + "'), ('" + ITEM_B + "')");
                st.execute("INSERT INTO appointments (id) VALUES ('" + APPT_A + "'), ('" + APPT_B + "')");
            }
            // Arquivo inteiro num único execute (como o Flyway): o bloco DO $$ ... $$ não pode ser dividido.
            try (Statement st = conn.createStatement()) {
                st.execute(new ClassPathResource(
                        "db/migration/V108__appointment_recurrence_procedures_registration.sql")
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

        seed(TENANT_A, CONTACT_A, APPT_A, ITEM_A, HASH_A);
        seed(TENANT_B, CONTACT_B, APPT_B, ITEM_B, HASH_B);
    }

    @AfterAll
    static void tearDown() {
        if (rawPool != null) {
            rawPool.close();
        }
    }

    @BeforeEach
    @AfterEach
    void clearContext() throws SQLException {
        TenantContext.clear();
        // Restaura used_at (um teste marca o link A como usado).
        try (Connection conn = postgres.createConnection("");
             Statement st = conn.createStatement()) {
            st.execute("UPDATE patient_registration_links SET used_at = NULL");
        }
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

    private static void seed(UUID tenant, UUID contact, UUID appointment, UUID item, String hash)
            throws SQLException {
        TenantContext.setCompanyId(tenant);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            insertLink(conn, tenant, contact, hash);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO appointment_procedures (appointment_id, catalog_item_id, company_id) VALUES (?, ?, ?)")) {
                ps.setObject(1, appointment);
                ps.setObject(2, item);
                ps.setObject(3, tenant);
                ps.executeUpdate();
            }
        } finally {
            TenantContext.clear();
        }
    }

    private static void insertLink(Connection conn, UUID tenant, UUID contact, String hash) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO patient_registration_links (company_id, contact_id, token, expires_at)
                VALUES (?, ?, ?, now() + interval '7 days')""")) {
            ps.setObject(1, tenant);
            ps.setObject(2, contact);
            ps.setString(3, hash);
            ps.executeUpdate();
        }
    }

    private static int count(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static int countWithTenant(String table) throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            return count(conn, "SELECT count(*) FROM " + table);
        }
    }

    private static void setToken(Connection conn, String hash) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT app.set_registration_token_context(?)")) {
            ps.setString(1, hash);
            ps.executeQuery().close();
        }
    }

    @Test
    void eachTenantSeesOnlyItsOwnRows() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        assertEquals(1, countWithTenant("patient_registration_links"));
        assertEquals(1, countWithTenant("appointment_procedures"));
        TenantContext.setCompanyId(TENANT_B);
        assertEquals(1, countWithTenant("patient_registration_links"));
        assertEquals(1, countWithTenant("appointment_procedures"));
    }

    @Test
    void noTenantNoToken_seesNothing() throws SQLException {
        assertEquals(0, countWithTenant("patient_registration_links"));
        assertEquals(0, countWithTenant("appointment_procedures"));
    }

    @Test
    void tenantB_cannotInsertIntoTenantA() {
        TenantContext.setCompanyId(TENANT_B);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection()) {
                insertLink(conn, TENANT_A, CONTACT_A, "c".repeat(64));
            }
        });
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO appointment_procedures (appointment_id, catalog_item_id, company_id) VALUES (?, ?, ?)")) {
                ps.setObject(1, APPT_A);
                ps.setObject(2, ITEM_B);
                ps.setObject(3, TENANT_A);
                ps.executeUpdate();
            }
        });
    }

    @Test
    void tokenWithoutTenant_seesOnlyItsLink_andNotOtherTables() throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection()) { // sem TenantContext: GUCs de tenant resetados
            conn.setAutoCommit(false);
            setToken(conn, HASH_A);
            assertEquals(1, count(conn, "SELECT count(*) FROM patient_registration_links"));
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT company_id, contact_id FROM patient_registration_links")) {
                rs.next();
                assertEquals(TENANT_A, rs.getObject(1, UUID.class));
                assertEquals(CONTACT_A, rs.getObject(2, UUID.class));
            }
            assertEquals(0, count(conn, "SELECT count(*) FROM appointment_procedures"));
            conn.commit();
            // GUC local à transação: depois do commit, nada visível.
            assertEquals(0, count(conn, "SELECT count(*) FROM patient_registration_links"));
            conn.commit();
        }
    }

    @Test
    void unknownToken_seesNothing() throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection()) { // sem TenantContext: GUCs de tenant resetados
            conn.setAutoCommit(false);
            setToken(conn, "f".repeat(64));
            assertEquals(0, count(conn, "SELECT count(*) FROM patient_registration_links"));
            conn.rollback();
        }
    }

    @Test
    void token_canMarkOnlyItsOwnLinkUsed() throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection()) { // sem TenantContext: GUCs de tenant resetados
            conn.setAutoCommit(false);
            setToken(conn, HASH_A);
            try (Statement st = conn.createStatement()) {
                assertEquals(1, st.executeUpdate("UPDATE patient_registration_links SET used_at = now()"));
            }
            // Não pode trocar o token da linha (WITH CHECK).
            assertThrows(SQLException.class, () -> {
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("UPDATE patient_registration_links SET token = '" + HASH_B + "'");
                }
            });
            conn.rollback();
            setToken(conn, HASH_A);
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("UPDATE patient_registration_links SET used_at = now()");
            }
            conn.commit();
        }
        TenantContext.setCompanyId(TENANT_B);
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT used_at FROM patient_registration_links")) {
            rs.next();
            assertNull(rs.getObject(1));
        }
    }

    @Test
    void token_cannotDeleteOrInsert() throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection()) { // sem TenantContext: GUCs de tenant resetados
            conn.setAutoCommit(false);
            setToken(conn, HASH_A);
            try (Statement st = conn.createStatement()) {
                assertEquals(0, st.executeUpdate("DELETE FROM patient_registration_links"));
            }
            assertThrows(SQLException.class, () -> insertLink(conn, TENANT_A, CONTACT_A, HASH_A.replace('a', 'd')));
            conn.rollback();
        }
    }
}
