package com.becommerce.crm.infrastructure.lead.persistence;

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
 * Teste de integração REAL (Testcontainers PostgreSQL 17 + HikariCP) que
 * comprova o isolamento cross-tenant via RLS FORCE na tabela {@code leads}
 * (Sprint 10). Espelha o estado pós-migração V016/V021 e valida que o módulo
 * de leads é realmente isolado por empresa — mesma garantia de segurança que o
 * {@code TenantAwareDataSource} + {@code TenantContext} entregam em runtime.
 */
@Testcontainers
class LeadIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("crm_it_leads")
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

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("lead-rls-bootstrap.sql"));

            // Usuário de aplicação NÃO-superuser (não bypassa RLS — como em produção)
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
        // Empresa A com 1 contato + 1 lead
        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            insertCompany(conn, TENANT_A, "Lead Tenant A LTDA", "11.111.111/0001-11", "a.leads@crm.local");
            insertContact(conn, CONTACT_A, TENANT_A, "Ana", "Contato", "ana@a.com");
            insertLead(conn, TENANT_A, CONTACT_A, "NEW", "WHATSAPP");
        }

        // Empresa B com 1 contato + 1 lead (para isolar nos dois sentidos)
        TenantContext.setCompanyId(TENANT_B);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            insertCompany(conn, TENANT_B, "Lead Tenant B LTDA", "22.222.222/0002-22", "b.leads@crm.local");
            insertContact(conn, CONTACT_B, TENANT_B, "Bruno", "Contato", "bruno@b.com");
            insertLead(conn, TENANT_B, CONTACT_B, "CONTACTED", "FORM");
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

    private static void insertLead(Connection conn, UUID companyId, UUID contactId, String status,
                                   String source) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO leads (company_id, contact_id, status, score, source)
                VALUES (?, ?, ?, 0, ?)
                """)) {
            ps.setObject(1, companyId);
            ps.setObject(2, contactId);
            ps.setString(3, status);
            ps.setString(4, source);
            ps.executeUpdate();
        }
    }

    // =========================================================================
    // Testes
    // =========================================================================

    @Test
    void tenantA_shouldOnlySeeOwnLeads() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        assertEquals(1, countLeads(), "Tenant A deve ver apenas 1 lead (o seu)");
    }

    @Test
    void tenantB_shouldOnlySeeOwnLeads() throws SQLException {
        TenantContext.setCompanyId(TENANT_B);
        assertEquals(1, countLeads(), "Tenant B deve ver apenas 1 lead (o seu)");
    }

    @Test
    void noContext_shouldSeeNoLeads() throws SQLException {
        TenantContext.clear();
        assertEquals(0, countLeads(), "Sem contexto de tenant não deve ver nenhum lead");
    }

    @Test
    void crossTenantSelect_shouldReturnEmpty() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        int count = countLeadsOfCompany(TENANT_B);
        assertEquals(0, count, "Tenant A tentando ler leads de B deve ver 0 (RLS esconde)");
    }

    @Test
    void crossTenantInsert_shouldBeBlockedByRLS() {
        TenantContext.setCompanyId(TENANT_A);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                         INSERT INTO leads (company_id, contact_id, status, score, source)
                         VALUES (?, ?, 'NEW', 0, 'API')
                         """)) {
                ps.setObject(1, TENANT_B);
                ps.setObject(2, CONTACT_B);
                ps.executeUpdate();
            }
        }, "INSERT cross-tenant de lead deve lançar exceção de RLS");
    }

    @Test
    void crossTenantUpdate_shouldAffectZeroRows() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE leads SET status = 'CONVERTED' WHERE company_id = ?")) {
            ps.setObject(1, TENANT_B);
            int updated = ps.executeUpdate();
            assertEquals(0, updated, "UPDATE cross-tenant de lead deve afetar 0 linhas");
        }
    }

    @Test
    void crossTenantDelete_shouldAffectZeroRows() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM leads WHERE company_id = ?")) {
            ps.setObject(1, TENANT_B);
            int deleted = ps.executeUpdate();
            assertEquals(0, deleted, "DELETE cross-tenant de lead deve afetar 0 linhas");
        }
    }

    @Test
    void sameTenantInsertAndRead_shouldWork() throws SQLException {
        UUID contactA2 = UUID.fromString("cccccccc-0000-0000-0000-000000000003");
        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            insertContact(conn, contactA2, TENANT_A, "Carla", "Novo", "carla@a.com");
            insertLead(conn, TENANT_A, contactA2, "QUALIFIED", "IMPORT");
        }
        assertEquals(2, countLeads(), "Tenant A deve ver seu novo lead na mesma empresa");
    }

    private int countLeads() throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM leads")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int countLeadsOfCompany(UUID companyId) throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT count(*) FROM leads WHERE company_id = ?")) {
            ps.setObject(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}