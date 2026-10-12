package com.becommerce.crm.masterdata.inventory.infrastructure.persistence;

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
 * Isolamento de tenant do controle de estoque aplicando as migrations REAIS
 * {@code V078__catalog_items.sql} e {@code V095__inventory.sql} (Testcontainers
 * PostgreSQL 17, role NOBYPASSRLS como o {@code crm_app}): A e B isolados,
 * sem contexto nada visível, escrita cross-tenant barrada (inclusive via FK
 * composta de {@code inventory_movements}), saldo {@code quantity} editável pelo
 * app, categoria de produto validada contra a lista fixa e permissões do módulo
 * concedidas aos papéis certos.
 */
@Testcontainers
class InventoryIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("crm_it_inventory")
            .withUsername("crm_superuser")
            .withPassword("crm_it_pass");

    static final String APP_USER = "crm_inventory_app_user";
    static final String APP_PASSWORD = "crm_inventory_app_pass";

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
                // Mínimo do RBAC que V078/V095 alimentam (permissões + vínculo papel -> permissão).
                st.execute("""
                        CREATE TABLE permissions (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            name VARCHAR(100) UNIQUE NOT NULL, description TEXT, module VARCHAR(50),
                            resource VARCHAR(50), action VARCHAR(50))""");
                st.execute("CREATE TABLE roles (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), name VARCHAR(50))");
                st.execute("""
                        CREATE TABLE role_permissions (role_id UUID, permission_id UUID,
                            PRIMARY KEY (role_id, permission_id))""");
            }
            insertCompany(conn, TENANT_A, "Inventory A LTDA", "41.111.111/0001-11");
            insertCompany(conn, TENANT_B, "Inventory B LTDA", "42.222.222/0002-22");
            try (Statement st = conn.createStatement()) {
                st.execute("INSERT INTO roles (name) VALUES ('ADMIN'), ('MANAGER'), ('AGENT'), ('VIEWER')");
            }
            // Arquivos inteiros num único execute cada (como o Flyway): o ScriptUtils
            // divide em ';' e quebraria o bloco DO $$ ... $$ das permissões.
            try (Statement st = conn.createStatement()) {
                st.execute(new ClassPathResource("db/migration/V078__catalog_items.sql")
                        .getContentAsString(StandardCharsets.UTF_8));
                st.execute(new ClassPathResource("db/migration/V095__inventory.sql")
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

        seedItem(TENANT_A, "Luva A", "SKU-1");
        seedItem(TENANT_B, "Luva B", "SKU-1"); // mesmo SKU em outra empresa é permitido
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

    private static UUID insertItemReturningId(Connection conn, UUID tenant, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO catalog_items (company_id, item_type, name) VALUES (?, 'PRODUCT', ?) "
                + "RETURNING id")) {
            ps.setObject(1, tenant);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getObject(1, UUID.class);
            }
        }
    }

    /** Só os itens do seed (SKU-1): outros testes inserem itens e a ordem de execução não é fixa. */
    private static String visibleNames() throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT string_agg(name, ',' ORDER BY name) FROM catalog_items "
                     + "WHERE sku = 'SKU-1'")) {
            rs.next();
            return rs.getString(1);
        }
    }

    private static String countDelta(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(SUM(delta), 0) FROM inventory_movements")) {
            rs.next();
            return rs.getString(1);
        }
    }

    private static int quantityOf(Connection conn, UUID itemId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT quantity FROM catalog_items WHERE id = ?")) {
            ps.setObject(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Test
    void eachTenantSeesOnlyItsOwnItems() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        assertEquals("Luva A", visibleNames());
        TenantContext.setCompanyId(TENANT_B);
        assertEquals("Luva B", visibleNames());
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
    void rlsIsolatesMovementsBetweenTenants() throws Exception {
        TenantContext.setCompanyId(TENANT_A);
        UUID itemA;
        UUID itemB;
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            itemA = insertItemReturningId(conn, TENANT_A, "Luva A");
        }
        TenantContext.setCompanyId(TENANT_B);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            itemB = insertItemReturningId(conn, TENANT_B, "Luva B");
            // B registra movimento no item de B
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO inventory_movements (company_id, catalog_item_id, movement_type, delta) "
                    + "VALUES (?, ?, 'ENTRADA', 5)")) {
                ps.setObject(1, TENANT_B);
                ps.setObject(2, itemB);
                ps.executeUpdate();
            }
            // B NÃO consegue inserir movimento apontando para o item de A
            assertThrows(SQLException.class, () -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO inventory_movements (company_id, catalog_item_id, movement_type, delta) "
                        + "VALUES (?, ?, 'ENTRADA', 1)")) {
                    ps.setObject(1, TENANT_B);
                    ps.setObject(2, itemA);
                    ps.executeUpdate();
                }
            });
            // B só enxerga o próprio movimento
            assertEquals("5", countDelta(conn));
        }
    }

    @Test
    void appUserCanUpdateQuantityCacheOnCatalogItem() throws Exception {
        TenantContext.setCompanyId(TENANT_A);
        UUID itemA;
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            itemA = insertItemReturningId(conn, TENANT_A, "Luva A");
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE catalog_items SET quantity = 3 WHERE id = ?")) {
                ps.setObject(1, itemA);
                ps.executeUpdate();
            }
            assertEquals(3, quantityOf(conn, itemA));
        }
    }

    @Test
    void creatingProductWithOutOfListCategoryIsRejectedByDb() {
        TenantContext.setCompanyId(TENANT_A);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO catalog_items (company_id, item_type, name, category) "
                        + "VALUES (?, 'PRODUCT', 'Item', 'Planos')")) {
                    ps.setObject(1, TENANT_A);
                    ps.executeUpdate();
                }
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
            assertEquals("ADMIN:catalog:manage,ADMIN:catalog:read,ADMIN:inventory:manage,"
                    + "ADMIN:inventory:read,AGENT:catalog:read,AGENT:inventory:read,"
                    + "MANAGER:catalog:manage,MANAGER:catalog:read,MANAGER:inventory:manage,"
                    + "MANAGER:inventory:read,VIEWER:catalog:read,VIEWER:inventory:read", rs.getString(1));
        }
    }
}