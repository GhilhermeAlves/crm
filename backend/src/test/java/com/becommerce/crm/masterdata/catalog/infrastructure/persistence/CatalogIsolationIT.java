package com.becommerce.crm.masterdata.catalog.infrastructure.persistence;

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
 * Isolamento de tenant do catálogo aplicando a migration REAL {@code V078__catalog_items.sql}
 * (Testcontainers PostgreSQL 17, role NOBYPASSRLS como o {@code crm_app}): A e B isolados,
 * sem contexto nada visível, escrita cross-tenant barrada, SKU único por empresa e
 * permissões do módulo concedidas aos papéis certos.
 */
@Testcontainers
class CatalogIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("crm_it_catalog")
            .withUsername("crm_superuser")
            .withPassword("crm_it_pass");

    static final String APP_USER = "crm_catalog_app_user";
    static final String APP_PASSWORD = "crm_catalog_app_pass";

    static HikariDataSource rawPool;
    static TenantAwareDataSource tenantAwareDataSource;

    static final UUID TENANT_A = UUID.fromString("33333333-4444-5555-6666-777777777771");
    static final UUID TENANT_B = UUID.fromString("33333333-4444-5555-6666-777777777772");

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("agent-rls-bootstrap.sql"));
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE ROLE crm_app NOLOGIN NOBYPASSRLS");
                // Mínimo do RBAC que a V078 alimenta (permissões + vínculo papel -> permissão).
                st.execute("""
                        CREATE TABLE permissions (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            name VARCHAR(100) UNIQUE NOT NULL, description TEXT, module VARCHAR(50),
                            resource VARCHAR(50), action VARCHAR(50))""");
                st.execute("CREATE TABLE roles (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), name VARCHAR(50))");
                st.execute("""
                        CREATE TABLE role_permissions (role_id UUID, permission_id UUID,
                            PRIMARY KEY (role_id, permission_id))""");
            }
            insertCompany(conn, TENANT_A, "Catalog A LTDA", "41.111.111/0001-11");
            insertCompany(conn, TENANT_B, "Catalog B LTDA", "42.222.222/0002-22");
            try (Statement st = conn.createStatement()) {
                st.execute("INSERT INTO roles (name) VALUES ('ADMIN'), ('MANAGER'), ('AGENT'), ('VIEWER')");
            }
            // Arquivo inteiro num único execute (como o Flyway): o ScriptUtils divide em ';'
            // e quebraria o bloco DO $$ ... $$ das permissões.
            try (Statement st = conn.createStatement()) {
                st.execute(new ClassPathResource("db/migration/V078__catalog_items.sql")
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

        seedItem(TENANT_A, "Plano A", "SKU-1");
        seedItem(TENANT_B, "Plano B", "SKU-1"); // mesmo SKU em outra empresa é permitido
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

    private static void seedItem(UUID tenant, String name, String sku) throws SQLException {
        TenantContext.setCompanyId(tenant);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            insertItem(conn, tenant, name, sku);
        } finally {
            TenantContext.clear();
        }
    }

    private static void insertItem(Connection conn, UUID tenant, String name, String sku) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO catalog_items (company_id, item_type, name, price, sku) VALUES (?, 'PRODUCT', ?, 10, ?)")) {
            ps.setObject(1, tenant);
            ps.setString(2, name);
            ps.setString(3, sku);
            ps.executeUpdate();
        }
    }

    private static String visibleNames() throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT string_agg(name, ',' ORDER BY name) FROM catalog_items")) {
            rs.next();
            return rs.getString(1);
        }
    }

    @Test
    void eachTenantSeesOnlyItsOwnItems() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        assertEquals("Plano A", visibleNames());
        TenantContext.setCompanyId(TENANT_B);
        assertEquals("Plano B", visibleNames());
    }

    @Test
    void noTenantContext_seesNothing() throws SQLException {
        assertEquals(null, visibleNames());
    }

    @Test
    void tenantB_cannotInsertIntoTenantA() {
        TenantContext.setCompanyId(TENANT_B);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection()) {
                insertItem(conn, TENANT_A, "Intruso", null);
            }
        });
    }

    @Test
    void skuIsUniquePerCompany() {
        TenantContext.setCompanyId(TENANT_A);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection()) {
                insertItem(conn, TENANT_A, "Duplicado", "SKU-1");
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
                     JOIN permissions p ON p.id = rp.permission_id""")) {
            rs.next();
            assertEquals("ADMIN:catalog:manage,ADMIN:catalog:read,AGENT:catalog:read,"
                    + "MANAGER:catalog:manage,MANAGER:catalog:read,VIEWER:catalog:read", rs.getString(1));
        }
    }
}
