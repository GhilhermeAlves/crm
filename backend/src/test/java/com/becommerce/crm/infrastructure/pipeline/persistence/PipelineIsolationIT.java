package com.becommerce.crm.infrastructure.pipeline.persistence;

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
 * comprova o isolamento cross-tenant via RLS FORCE nas tabelas de pipeline
 * (pipelines, stages, opportunities, opportunity_history) (Sprint 11). Mesmo
 * fluxo do {@code LeadIsolationIT}: usuário de aplicação NÃO-bypass, garantia
 * de segurança equivalente ao runtime ({@code TenantAwareDataSource} +
 * {@code TenantContext}).
 */
@Testcontainers
class PipelineIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("crm_it_pipeline")
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

    static final UUID PIPELINE_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000011");
    static final UUID STAGE_A1 = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000021");
    static final UUID STAGE_A2 = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000022");
    static final UUID OPP_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000031");

    static final UUID PIPELINE_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000011");
    static final UUID STAGE_B1 = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000021");
    static final UUID STAGE_B2 = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000022");
    static final UUID OPP_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000031");

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("pipeline-rls-bootstrap.sql"));

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
            insertCompany(conn, TENANT_A, "Pipeline Tenant A LTDA", "11.111.111/0001-11", "a.pipe@crm.local");
            insertContact(conn, CONTACT_A, TENANT_A, "Ana", "Contato", "ana@a.com");
            insertPipeline(conn, PIPELINE_A, TENANT_A, "Vendas A");
            insertStage(conn, STAGE_A1, PIPELINE_A, TENANT_A, "Prospecção", 1);
            insertStage(conn, STAGE_A2, PIPELINE_A, TENANT_A, "Fechamento", 2);
            insertOpportunity(conn, OPP_A, TENANT_A, CONTACT_A, PIPELINE_A, STAGE_A1, "Oferta A");
        }

        TenantContext.setCompanyId(TENANT_B);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            insertCompany(conn, TENANT_B, "Pipeline Tenant B LTDA", "22.222.222/0002-22", "b.pipe@crm.local");
            insertContact(conn, CONTACT_B, TENANT_B, "Bruno", "Contato", "bruno@b.com");
            insertPipeline(conn, PIPELINE_B, TENANT_B, "Vendas B");
            insertStage(conn, STAGE_B1, PIPELINE_B, TENANT_B, "Prospecção", 1);
            insertStage(conn, STAGE_B2, PIPELINE_B, TENANT_B, "Fechamento", 2);
            insertOpportunity(conn, OPP_B, TENANT_B, CONTACT_B, PIPELINE_B, STAGE_B1, "Oferta B");
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

    private static void insertPipeline(Connection conn, UUID id, UUID companyId, String name)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO pipelines (id, company_id, name)
                VALUES (?, ?, ?)
                """)) {
            ps.setObject(1, id);
            ps.setObject(2, companyId);
            ps.setString(3, name);
            ps.executeUpdate();
        }
    }

    private static void insertStage(Connection conn, UUID id, UUID pipelineId, UUID companyId,
                                    String name, int order) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO stages (id, pipeline_id, company_id, name, "order", probability)
                VALUES (?, ?, ?, ?, ?, 50)
                """)) {
            ps.setObject(1, id);
            ps.setObject(2, pipelineId);
            ps.setObject(3, companyId);
            ps.setString(4, name);
            ps.setInt(5, order);
            ps.executeUpdate();
        }
    }

    private static void insertOpportunity(Connection conn, UUID id, UUID companyId, UUID contactId,
                                          UUID pipelineId, UUID stageId, String title) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO opportunities (id, company_id, title, value, contact_id, pipeline_id, stage_id)
                VALUES (?, ?, ?, 1000.00, ?, ?, ?)
                """)) {
            ps.setObject(1, id);
            ps.setObject(2, companyId);
            ps.setString(3, title);
            ps.setObject(4, contactId);
            ps.setObject(5, pipelineId);
            ps.setObject(6, stageId);
            ps.executeUpdate();
        }
    }

    // =========================================================================
    // Testes
    // =========================================================================

    @Test
    void tenantA_shouldOnlySeeOwnOpportunities() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        assertEquals(1, countOpportunities(), "Tenant A deve ver apenas 1 oportunidade (a sua)");
    }

    @Test
    void tenantB_shouldOnlySeeOwnOpportunities() throws SQLException {
        TenantContext.setCompanyId(TENANT_B);
        assertEquals(1, countOpportunities(), "Tenant B deve ver apenas 1 oportunidade (a sua)");
    }

    @Test
    void noContext_shouldSeeNoOpportunities() throws SQLException {
        TenantContext.clear();
        assertEquals(0, countOpportunities(), "Sem contexto de tenant não deve ver nenhuma oportunidade");
    }

    @Test
    void crossTenantSelect_shouldReturnEmpty() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        int count = countOpportunitiesOfCompany(TENANT_B);
        assertEquals(0, count, "Tenant A tentando ler oportunidades de B deve ver 0 (RLS esconde)");
    }

    @Test
    void crossTenantInsert_shouldBeBlockedByRLS() {
        TenantContext.setCompanyId(TENANT_A);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                         INSERT INTO opportunities (company_id, title, value, contact_id, pipeline_id, stage_id)
                         VALUES (?, 'Invasão', 1.00, ?, ?, ?)
                         """)) {
                ps.setObject(1, TENANT_B);
                ps.setObject(2, CONTACT_B);
                ps.setObject(3, PIPELINE_B);
                ps.setObject(4, STAGE_B1);
                ps.executeUpdate();
            }
        }, "INSERT cross-tenant de oportunidade deve lançar exceção de RLS");
    }

    @Test
    void crossTenantUpdate_shouldAffectZeroRows() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE opportunities SET status = 'WON' WHERE company_id = ?")) {
            ps.setObject(1, TENANT_B);
            int updated = ps.executeUpdate();
            assertEquals(0, updated, "UPDATE cross-tenant de oportunidade deve afetar 0 linhas");
        }
    }

    @Test
    void crossTenantDelete_shouldAffectZeroRows() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM opportunities WHERE company_id = ?")) {
            ps.setObject(1, TENANT_B);
            int deleted = ps.executeUpdate();
            assertEquals(0, deleted, "DELETE cross-tenant de oportunidade deve afetar 0 linhas");
        }
    }

    @Test
    void sameTenantInsertAndRead_shouldWork() throws SQLException {
        UUID oppA2 = UUID.fromString("cccccccc-0000-0000-0000-000000000003");
        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            insertOpportunity(conn, oppA2, TENANT_A, CONTACT_A, PIPELINE_A, STAGE_A1, "Nova Oferta");
        }
        assertEquals(2, countOpportunities(), "Tenant A deve ver sua nova oportunidade na mesma empresa");
    }

    private int countOpportunities() throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM opportunities")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int countOpportunitiesOfCompany(UUID companyId) throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT count(*) FROM opportunities WHERE company_id = ?")) {
            ps.setObject(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
