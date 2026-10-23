package com.becommerce.crm.clinical.procedure.infrastructure.persistence;

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
 * Isolamento de tenant da ficha clínica aplicando a migration REAL
 * {@code V106__patient_procedures.sql} (Testcontainers PostgreSQL 17, role NOBYPASSRLS
 * como o {@code crm_app}): A e B isolados nas duas tabelas, sem contexto nada visível,
 * escrita cross-tenant barrada, CHECKs de estado/faces e troca (delete + reinsert) dos
 * dentes do próprio procedimento.
 */
@Testcontainers
class PatientProcedureIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("crm_it_procedure")
            .withUsername("crm_superuser")
            .withPassword("crm_it_pass");

    static final String APP_USER = "crm_procedure_app_user";
    static final String APP_PASSWORD = "crm_procedure_app_pass";

    static HikariDataSource rawPool;
    static TenantAwareDataSource tenantAwareDataSource;

    static final UUID TENANT_A = UUID.fromString("44444444-5555-6666-7777-888888888881");
    static final UUID TENANT_B = UUID.fromString("44444444-5555-6666-7777-888888888882");

    static final UUID CONTACT_A = UUID.fromString("66666666-0000-0000-0000-00000000000a");
    static final UUID CONTACT_B = UUID.fromString("66666666-0000-0000-0000-00000000000b");
    static final UUID ITEM_A = UUID.fromString("77777777-0000-0000-0000-00000000000a");
    static final UUID ITEM_B = UUID.fromString("77777777-0000-0000-0000-00000000000b");

    static UUID procedureA;
    static UUID procedureB;

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("agent-rls-bootstrap.sql"));
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE ROLE crm_app NOLOGIN NOBYPASSRLS");
                // Tabelas mínimas referenciadas pelas FKs da V106.
                st.execute("CREATE TABLE contacts (id UUID PRIMARY KEY)");
                st.execute("CREATE TABLE users (id UUID PRIMARY KEY)");
                st.execute("CREATE TABLE catalog_items (id UUID PRIMARY KEY)");
                st.execute("CREATE TABLE financial_transactions (id UUID PRIMARY KEY)");
            }
            insertCompany(conn, TENANT_A, "Procedimento A LTDA", "61.111.111/0001-11");
            insertCompany(conn, TENANT_B, "Procedimento B LTDA", "62.222.222/0002-22");
            try (Statement st = conn.createStatement()) {
                st.execute("INSERT INTO contacts (id) VALUES ('" + CONTACT_A + "'), ('" + CONTACT_B + "')");
                st.execute("INSERT INTO catalog_items (id) VALUES ('" + ITEM_A + "'), ('" + ITEM_B + "')");
            }
            // Arquivo inteiro num único execute (como o Flyway): o ScriptUtils divide em ';'
            // e quebraria o bloco DO $$ ... $$ de RLS/GRANT.
            try (Statement st = conn.createStatement()) {
                st.execute(new ClassPathResource("db/migration/V106__patient_procedures.sql")
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

        procedureA = seedProcedure(TENANT_A, CONTACT_A, ITEM_A, "Restauração A");
        procedureB = seedProcedure(TENANT_B, CONTACT_B, ITEM_B, "Restauração B");
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

    private static UUID seedProcedure(UUID tenant, UUID contact, UUID item, String name) throws SQLException {
        TenantContext.setCompanyId(tenant);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            UUID id = insertProcedure(conn, tenant, contact, item, name);
            insertTooth(conn, tenant, id, 11, "{M,O}");
            return id;
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

    private static void insertTooth(Connection conn, UUID tenant, UUID procedure, int tooth, String faces)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO patient_procedure_teeth (procedure_id, company_id, tooth, faces)
                VALUES (?, ?, ?, ?::varchar[])""")) {
            ps.setObject(1, procedure);
            ps.setObject(2, tenant);
            ps.setInt(3, tooth);
            ps.setString(4, faces);
            ps.executeUpdate();
        }
    }

    private static String visibleNames() throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT string_agg(name_snapshot, ',' ORDER BY name_snapshot) FROM patient_procedures")) {
            rs.next();
            return rs.getString(1);
        }
    }

    private static int visibleTeethCount() throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM patient_procedure_teeth")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Test
    void eachTenantSeesOnlyItsOwnProcedures() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        assertEquals("Restauração A", visibleNames());
        TenantContext.setCompanyId(TENANT_B);
        assertEquals("Restauração B", visibleNames());
    }

    @Test
    void eachTenantSeesOnlyItsOwnTeeth() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        assertEquals(1, visibleTeethCount());
        TenantContext.setCompanyId(TENANT_B);
        assertEquals(1, visibleTeethCount());
    }

    @Test
    void noTenantContext_seesNothing() throws SQLException {
        assertEquals(null, visibleNames());
        assertEquals(0, visibleTeethCount());
    }

    @Test
    void tenantB_cannotInsertIntoTenantA() {
        TenantContext.setCompanyId(TENANT_B);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection()) {
                insertProcedure(conn, TENANT_A, CONTACT_A, ITEM_A, "Intruso");
            }
        });
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection()) {
                insertTooth(conn, TENANT_A, procedureA, 12, "{M}");
            }
        });
    }

    @Test
    void doneRequiresPerformedAt() {
        TenantContext.setCompanyId(TENANT_A);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE patient_procedures SET state = 'DONE', performed_at = NULL WHERE id = ?")) {
                ps.setObject(1, procedureA);
                ps.executeUpdate();
            }
        });
    }

    @Test
    void invalidFaceIsRejected() {
        TenantContext.setCompanyId(TENANT_A);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection()) {
                insertTooth(conn, TENANT_A, procedureA, 13, "{X}");
            }
        });
    }

    @Test
    void tenantCanReplaceTeethOfOwnProcedure() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM patient_procedure_teeth WHERE procedure_id = ?")) {
                ps.setObject(1, procedureA);
                assertEquals(1, ps.executeUpdate());
            }
            insertTooth(conn, TENANT_A, procedureA, 21, "{D}");
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT count(*), min(tooth) FROM patient_procedure_teeth WHERE procedure_id = ?")) {
                ps.setObject(1, procedureA);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    assertEquals(1, rs.getInt(1));
                    assertEquals(21, rs.getInt(2));
                }
            }
        }
        // B continua com o próprio dente intacto.
        TenantContext.setCompanyId(TENANT_B);
        assertEquals(1, visibleTeethCount());
    }
}
