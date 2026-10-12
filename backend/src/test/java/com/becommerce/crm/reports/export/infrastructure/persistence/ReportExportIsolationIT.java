package com.becommerce.crm.reports.export.infrastructure.persistence;

import com.becommerce.crm.reports.export.domain.ExportFormat;
import com.becommerce.crm.reports.export.domain.ReportExport;
import com.becommerce.crm.reports.export.domain.ReportExportStatus;
import com.becommerce.crm.reports.export.domain.ReportExportType;
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
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code report_exports} (V096) com Testcontainers: ida e volta do
 * {@link ReportExportRepositoryImpl} (insert, update para READY, busca recente)
 * e isolamento RLS FORCE entre tenants.
 */
@Testcontainers
class ReportExportIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("crm_it_report_exports")
            .withUsername("crm_superuser")
            .withPassword("crm_it_pass");

    static final String APP_USER = "crm_report_exports_app_user";
    static final String APP_PASSWORD = "crm_report_exports_app_pass";

    static HikariDataSource rawPool;
    static TenantAwareDataSource tenantAwareDataSource;
    static ReportExportRepositoryImpl repository;

    static final UUID TENANT_A = UUID.fromString("55555555-6666-7777-8888-999999999931");
    static final UUID TENANT_B = UUID.fromString("55555555-6666-7777-8888-999999999932");
    static final UUID USER = UUID.fromString("55555555-6666-7777-8888-999999999933");

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("agent-rls-bootstrap.sql"));
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE ROLE crm_app NOLOGIN NOBYPASSRLS");
            }
            insertCompany(conn, TENANT_A, "Exports A LTDA", "55.333.333/0003-33");
            insertCompany(conn, TENANT_B, "Exports B LTDA", "56.444.444/0004-44");
            try (Statement st = conn.createStatement()) {
                st.execute(new ClassPathResource("db/migration/V096__report_exports.sql")
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
        repository = new ReportExportRepositoryImpl(new NamedParameterJdbcTemplate(tenantAwareDataSource));
    }

    @AfterAll
    static void tearDown() {
        if (rawPool != null) {
            rawPool.close();
        }
    }

    @BeforeEach
    void clearExports() throws SQLException {
        TenantContext.clear();
        try (Connection conn = postgres.createConnection("");
             Statement st = conn.createStatement()) {
            st.execute("TRUNCATE report_exports");
        }
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private static void insertCompany(Connection conn, UUID id, String name, String cnpj)
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
            ps.setString(5, id + "@crm.local");
            ps.executeUpdate();
        }
    }

    private static ReportExport export(UUID companyId) {
        return ReportExport.pending(companyId, USER, ReportExportType.CASH_FLOW, ExportFormat.XLSX,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
    }

    @Test
    void insertUpdateAndFindRoundTrip() {
        TenantContext.setCompanyId(TENANT_A);
        ReportExport export = export(TENANT_A);
        repository.insert(export);
        UUID objectId = UUID.randomUUID();
        export.markReady(objectId, "fluxo.xlsx");
        repository.update(export);

        ReportExport loaded = repository.findById(TENANT_A, export.id()).orElseThrow();
        assertEquals(ReportExportStatus.READY, loaded.status());
        assertEquals(objectId, loaded.storageObjectId());
        assertEquals("fluxo.xlsx", loaded.fileName());
        assertEquals(LocalDate.of(2026, 6, 30), loaded.periodTo());
        assertNotNull(loaded.finishedAt());

        List<ReportExport> recent = repository.findRecent(TENANT_A, USER, 20);
        assertEquals(1, recent.size());
    }

    @Test
    void eachTenantSeesOnlyItsOwnExports() {
        TenantContext.setCompanyId(TENANT_A);
        ReportExport fromA = export(TENANT_A);
        repository.insert(fromA);
        TenantContext.setCompanyId(TENANT_B);
        repository.insert(export(TENANT_B));

        assertTrue(repository.findById(TENANT_A, fromA.id()).isEmpty());
        assertEquals(1, repository.findRecent(TENANT_B, USER, 20).size());
        TenantContext.setCompanyId(TENANT_A);
        assertEquals(1, repository.findRecent(TENANT_A, USER, 20).size());
    }

    @Test
    void tenantBCannotInsertForTenantA() {
        TenantContext.setCompanyId(TENANT_B);
        assertThrows(DataAccessException.class, () -> repository.insert(export(TENANT_A)));
    }
}
