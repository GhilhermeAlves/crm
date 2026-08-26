package com.becommerce.crm.application.analytics.service;

import com.becommerce.crm.application.analytics.AnalyticsPeriod;
import com.becommerce.crm.application.analytics.dto.AnalyticsSummaryResponse;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import com.becommerce.crm.infrastructure.tenant.datasource.TenantAwareDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Sprint 19 — Tenant isolation do Analytics (RLS FORCE) sobre agregações.
 * Padrão das Sprints 16/17: PostgreSQL 17 + usuário NÃO-superuser +
 * TenantAwareDataSource. Cada tenant tem contagens distintas; o summary de A
 * só enxerga A, e o de B só enxerga B — inclusive na série diária.
 */
@Testcontainers
class AnalyticsIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("analytics_it")
            .withUsername("crm_superuser")
            .withPassword("analytics_it_pass");

    static final String APP_USER = "crm_app_user";
    static final String APP_PASSWORD = "crm_app_pass";

    static HikariDataSource rawPool;
    static NamedParameterJdbcTemplate jdbc;
    static TransactionTemplate tx;

    static final UUID TENANT_A = UUID.fromString("aaaaaaa1-0000-0000-0000-000000000001");
    static final UUID TENANT_B = UUID.fromString("bbbbbbb2-0000-0000-0000-000000000002");

    AnalyticsService analytics;

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("tenant-rls-bootstrap.sql"));
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("analytics-rls-bootstrap.sql"));
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
        rawPool = new HikariDataSource(config);
        jdbc = new NamedParameterJdbcTemplate(new TenantAwareDataSource(rawPool));
        tx = new TransactionTemplate(new DataSourceTransactionManager(rawPool));

        seedCompanies();
        seedTenantData(TENANT_A, 5, 3);
        seedTenantData(TENANT_B, 2, 7);
    }

    @AfterAll
    static void tearDown() {
        if (rawPool != null) {
            rawPool.close();
        }
    }

    @BeforeEach
    void setUp() {
        analytics = new AnalyticsService(jdbc);
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private static void seedCompanies() throws SQLException {
        try (Connection conn = new TenantAwareDataSource(rawPool).getConnection()) {
            for (UUID id : new UUID[]{TENANT_A, TENANT_B}) {
                try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT INTO companies (id, legal_name, trading_name, cnpj, email, phone,
                            address_zip_code, address_street, address_number, address_neighborhood,
                            address_city, address_state, address_country, plan, status, max_users, max_storage_mb)
                        VALUES (?, ?, ?, ?, ?, '11999990000', '00000-000', 'Rua', '1', 'Centro',
                            'São Paulo', 'SP', 'Brasil', 'FREE', 'ACTIVE', 10, 100)
                        ON CONFLICT (id) DO NOTHING
                        """)) {
                    ps.setObject(1, id);
                    ps.setString(2, "Tenant " + id);
                    ps.setString(3, "Tenant " + id);
                    ps.setString(4, id.toString().substring(0, 18));
                    ps.setString(5, id + "@crm.local");
                    ps.executeUpdate();
                }
            }
        }
    }

    private static void inTenant(UUID companyId, Runnable work) {
        TenantContext.setCompanyId(companyId);
        try {
            work.run();
        } finally {
            TenantContext.clear();
        }
    }

    private static void seedTenantData(UUID companyId, int contacts, int leads) {
        inTenant(companyId, () -> tx.executeWithoutResult(status -> {
            for (int i = 0; i < contacts; i++) {
                jdbc.update("INSERT INTO contacts (company_id, email) VALUES (:c, :e)",
                        java.util.Map.of("c", companyId, "e", UUID.randomUUID() + "@t.local"));
            }
            for (int i = 0; i < leads; i++) {
                jdbc.update("INSERT INTO leads (company_id) VALUES (:c)", java.util.Map.of("c", companyId));
            }
            jdbc.update("""
                    INSERT INTO opportunities (company_id, status, value, won_at)
                    VALUES (:c, 'WON', 500, NOW())
                    """, java.util.Map.of("c", companyId));
            jdbc.update("INSERT INTO workflow_runs (company_id, status) VALUES (:c, 'SUCCESS')",
                    java.util.Map.of("c", companyId));
            jdbc.update("""
                    INSERT INTO omnichannel_messages (company_id, direction)
                    VALUES (:c, 'OUTBOUND')
                    """, java.util.Map.of("c", companyId));
            Long seeded = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM contacts WHERE company_id = :c",
                    java.util.Map.of("c", companyId), Long.class);
        }));
    }

    private static LocalDate today() {
        // O banco/JVM dos testes rodam em UTC; usar UTC para alinhar período e timestamps.
        return LocalDate.now(java.time.ZoneId.of("UTC"));
    }

    @Test
    void summaryReturnsOnlyOwnTenantData() {
        var periodA = AnalyticsPeriod.resolve(today().minusDays(1), today(), "UTC");
        var a = analytics.summary(TENANT_A, periodA);

        assertEquals(5, a.current().contactsCreated(), "Tenant A deve ver apenas seus 5 contatos");
        assertEquals(3, a.current().leadsCreated());
        assertEquals(1, a.current().opportunitiesWon());

        var periodB = AnalyticsPeriod.resolve(today().minusDays(1), today(), "UTC");
        var b = analytics.summary(TENANT_B, periodB);
        assertEquals(2, b.current().contactsCreated(), "Tenant B deve ver apenas seus 2 contatos");
        assertEquals(7, b.current().leadsCreated());
    }

    @Test
    void dailySeriesIsIsolatedPerTenant() {
        var a = analytics.summary(TENANT_A,
                AnalyticsPeriod.resolve(today().minusDays(1), today(), "UTC"));
        long totalLeadsA = a.series().stream().mapToLong(AnalyticsSummaryResponse.DailyPoint::leads).sum();
        assertEquals(3, totalLeadsA);

        var b = analytics.summary(TENANT_B,
                AnalyticsPeriod.resolve(today().minusDays(1), today(), "UTC"));
        long totalLeadsB = b.series().stream().mapToLong(AnalyticsSummaryResponse.DailyPoint::leads).sum();
        assertEquals(7, totalLeadsB);
    }

    @Test
    void previousPeriodComparisonIsPopulatedAndIndependent() {
        // cria dado somente no período atual; período anterior fica zerado
        var period = AnalyticsPeriod.resolve(today().minusDays(1), today(), "UTC");
        var result = analytics.summary(TENANT_A, period);
        assertEquals(5, result.current().contactsCreated());
        assertEquals(0, result.previous().contactsCreated(),
                "Período anterior vazio deve retornar zeros sem erro");
    }

    @Test
    void emptyPeriodReturnsZerosWithoutError() {
        var farFuture = AnalyticsPeriod.resolve(LocalDate.of(2999, 1, 1), LocalDate.of(2999, 1, 2), "UTC");
        var result = analytics.summary(TENANT_A, farFuture);
        assertEquals(0, result.current().contactsCreated());
        assertEquals(0, result.current().workflowRunsMatched());
        assertTrue(result.series().size() >= 1);
    }

    private static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
