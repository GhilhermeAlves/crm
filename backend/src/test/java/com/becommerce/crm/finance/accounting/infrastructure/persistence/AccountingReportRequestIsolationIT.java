package com.becommerce.crm.finance.accounting.infrastructure.persistence;

import com.becommerce.crm.finance.accounting.domain.AccountingReportType;
import com.becommerce.crm.finance.accounting.domain.ReportFormat;
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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
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
import java.time.YearMonth;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@code accounting_report_requests} (V093) com Testcontainers: isolamento RLS
 * FORCE entre tenants, upsert por (empresa, tipo, formato, mês) incrementando
 * {@code generated_count} e 400/erro em escrita cross-tenant. Usa o
 * {@code NamedParameterJdbcTemplate} sobre o {@code TenantAwareDataSource}, como
 * o {@code AccountingReportRequestRepositoryImpl} real.
 */
@Testcontainers
class AccountingReportRequestIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("crm_it_accounting")
            .withUsername("crm_superuser")
            .withPassword("crm_it_pass");

    static final String APP_USER = "crm_accounting_app_user";
    static final String APP_PASSWORD = "crm_accounting_app_pass";

    static HikariDataSource rawPool;
    static TenantAwareDataSource tenantAwareDataSource;
    static AccountingReportRequestRepositoryImpl repository;

    static final UUID TENANT_A = UUID.fromString("44444444-5555-6666-7777-888888888821");
    static final UUID TENANT_B = UUID.fromString("44444444-5555-6666-7777-888888888822");

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("agent-rls-bootstrap.sql"));
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE ROLE crm_app NOLOGIN NOBYPASSRLS");
            }
            insertCompany(conn, TENANT_A, "Accounting A LTDA", "53.333.333/0003-33");
            insertCompany(conn, TENANT_B, "Accounting B LTDA", "54.444.444/0004-44");
            // Migration real aplicada de ponta a ponta (V093 não tem bloco DO).
            try (Statement st = conn.createStatement()) {
                st.execute(new ClassPathResource("db/migration/V093__accounting_report_requests.sql")
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
        repository = new AccountingReportRequestRepositoryImpl(
                new NamedParameterJdbcTemplate(tenantAwareDataSource));
    }

    @AfterAll
    static void tearDown() {
        if (rawPool != null) {
            rawPool.close();
        }
    }

    @BeforeEach
    void clearRequests() throws SQLException {
        TenantContext.clear();
        try (Connection conn = postgres.createConnection("");
             Statement st = conn.createStatement()) {
            st.execute("TRUNCATE accounting_report_requests");
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

    private static int visibleRequests(UUID tenant) {
        if (tenant != null) {
            TenantContext.setCompanyId(tenant);
        }
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             var rs = st.executeQuery("SELECT COUNT(*) FROM accounting_report_requests")) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException ex) {
            throw new IllegalStateException(ex);
        } finally {
            TenantContext.clear();
        }
    }

    private static int register(UUID companyId) {
        return repository.register(companyId, AccountingReportType.MONTHLY_ACCOUNTING,
                ReportFormat.CSV, YearMonth.of(2026, 1), TENANT_A);
    }

    @Test
    void eachTenantSeesOnlyItsOwnRequests() {
        TenantContext.setCompanyId(TENANT_A);
        repository.register(TENANT_A, AccountingReportType.STATEMENT, ReportFormat.PDF,
                YearMonth.of(2026, 1), TENANT_A);
        TenantContext.clear();
        TenantContext.setCompanyId(TENANT_B);
        repository.register(TENANT_B, AccountingReportType.STATEMENT, ReportFormat.PDF,
                YearMonth.of(2026, 1), TENANT_A);
        TenantContext.clear();

        assertEquals(1, visibleRequests(TENANT_A));
        assertEquals(1, visibleRequests(TENANT_B));
    }

    @Test
    void noTenantContext_seesNothing() {
        TenantContext.setCompanyId(TENANT_A);
        register(TENANT_A);
        TenantContext.clear();

        assertEquals(0, visibleRequests(null));
    }

    @Test
    void tenantB_cannotRegisterForTenantA() {
        TenantContext.setCompanyId(TENANT_B);
        assertThrows(DataAccessException.class, () -> repository.register(TENANT_A,
                AccountingReportType.MONTHLY_ACCOUNTING, ReportFormat.CSV,
                YearMonth.of(2026, 1), TENANT_A));
        TenantContext.clear();

        assertEquals(0, visibleRequests(TENANT_A));
    }

    @Test
    void sameKeyUpsertsIncrementingGeneratedCount() {
        TenantContext.setCompanyId(TENANT_A);
        assertEquals(1, register(TENANT_A));
        assertEquals(2, register(TENANT_A));
        assertEquals(3, register(TENANT_A));
        TenantContext.clear();

        assertEquals(1, visibleRequests(TENANT_A));
        int count = new NamedParameterJdbcTemplate(tenantAwareDataSource)
                .queryForObject("SELECT generated_count FROM accounting_report_requests",
                        new MapSqlParameterSource(), Integer.class);
        assertEquals(3, count);
    }
}