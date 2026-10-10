package com.becommerce.crm.finance.report.infrastructure.persistence;

import com.becommerce.crm.finance.accounting.application.port.output.AccountingReportGateway;
import com.becommerce.crm.finance.accounting.infrastructure.persistence.AccountingReportGatewayImpl;
import com.becommerce.crm.finance.report.application.dto.SummaryFilter;
import com.becommerce.crm.finance.report.application.port.output.FinanceReportGateway;
import com.becommerce.crm.finance.report.domain.ReportGranularity;
import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Executa as consultas reais do {@link FinanceReportGateway} (Etapa 2) e do
 * {@link AccountingReportGateway} (Etapa 3) contra a migration real
 * {@code V092} + Testcontainers: agregações por status/pago, saúde (6 meses),
 * fluxo de caixa, distribuição de receita, visão geral, agenda, headline, mês
 * disponíveis e resolução paciente/profissional. RLS FORCE isola A e B.
 */
@Testcontainers
class FinanceReportGatewayIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("crm_it_report")
            .withUsername("crm_superuser")
            .withPassword("crm_it_pass");

    static final String APP_USER = "crm_report_app_user";
    static final String APP_PASSWORD = "crm_report_app_pass";

    static HikariDataSource rawPool;
    static TenantAwareDataSource tenantAwareDataSource;
    static FinanceReportGateway financeGateway;
    static AccountingReportGateway accountingGateway;

    static final UUID TENANT_A = UUID.fromString("44444444-5555-6666-7777-888888888830");
    static final UUID TENANT_B = UUID.fromString("44444444-5555-6666-7777-888888888831");

    static final UUID U_ANA = UUID.fromString("55555555-0000-0000-0000-000000000001");
    static final UUID U_CARLOS = UUID.fromString("55555555-0000-0000-0000-000000000002");
    static final UUID C_ANA = UUID.fromString("66666666-0000-0000-0000-000000000001");
    static final UUID CI_LIMPEZA = UUID.fromString("77777777-0000-0000-0000-000000000001");

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("agent-rls-bootstrap.sql"));
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE ROLE crm_app NOLOGIN NOBYPASSRLS");
                createSupportTables(st);
            }
            insertCompany(conn, TENANT_A, "Report A LTDA", "55.555.555/0005-55");
            insertCompany(conn, TENANT_B, "Report B LTDA", "56.666.666/0006-66");
            try (Statement st = conn.createStatement()) {
                st.execute(new ClassPathResource("db/migration/V092__financial_transactions.sql")
                        .getContentAsString(StandardCharsets.UTF_8));
            }
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE ROLE " + APP_USER + " LOGIN PASSWORD '" + APP_PASSWORD
                        + "' NOSUPERUSER NOBYPASSRLS");
                st.execute("GRANT crm_app TO " + APP_USER);
                st.execute("GRANT USAGE ON SCHEMA public TO " + APP_USER);
                st.execute("GRANT USAGE ON SCHEMA app TO " + APP_USER);
                st.execute("GRANT EXECUTE ON FUNCTION app.current_tenant_id() TO " + APP_USER);
                st.execute("GRANT SELECT ON companies, contacts, users, catalog_items, "
                        + "appointments, opportunities TO " + APP_USER);
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
        NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(tenantAwareDataSource);
        financeGateway = new FinanceReportGatewayImpl(jdbc);
        accountingGateway = new AccountingReportGatewayImpl(jdbc);

        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            seedReferences(conn);
            seedFinancialData(conn);
        } finally {
            TenantContext.clear();
        }
    }

    private static void createSupportTables(Statement st) throws SQLException {
        st.execute("CREATE TABLE contacts (id UUID PRIMARY KEY, company_id UUID NOT NULL, "
                + "first_name VARCHAR(100), last_name VARCHAR(100), email VARCHAR(255), "
                + "deleted_at TIMESTAMP)");
        st.execute("CREATE TABLE users (id UUID PRIMARY KEY, company_id UUID NOT NULL, "
                + "name VARCHAR(150), email VARCHAR(255), deleted_at TIMESTAMP)");
        st.execute("CREATE TABLE catalog_items (id UUID PRIMARY KEY, name VARCHAR(200))");
        st.execute("CREATE TABLE appointments (id UUID PRIMARY KEY, company_id UUID NOT NULL, "
                + "host_id UUID NOT NULL, appointment_type_id UUID, title VARCHAR(200), "
                + "start_at TIMESTAMPTZ NOT NULL, end_at TIMESTAMPTZ NOT NULL, status VARCHAR(20))");
        st.execute("CREATE TABLE opportunities (id UUID PRIMARY KEY, company_id UUID NOT NULL, "
                + "title VARCHAR(200), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        st.execute("CREATE TABLE permissions (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), "
                + "name VARCHAR(100) UNIQUE NOT NULL, description TEXT, module VARCHAR(50), "
                + "resource VARCHAR(50), action VARCHAR(50))");
        st.execute("CREATE TABLE roles (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), name VARCHAR(50))");
        st.execute("CREATE TABLE role_permissions (role_id UUID, permission_id UUID, "
                + "PRIMARY KEY (role_id, permission_id))");
        st.execute("INSERT INTO roles (name) VALUES ('ADMIN'), ('MANAGER'), ('AGENT'), ('VIEWER')");
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

    private static void seedReferences(Connection conn) throws SQLException {
        exec(conn, "INSERT INTO users (id, company_id, name, email) VALUES (?, ?, ?, ?)",
                U_ANA, TENANT_A, "Dr. Ana", "ana@crm.local");
        exec(conn, "INSERT INTO users (id, company_id, name, email) VALUES (?, ?, ?, ?)",
                U_CARLOS, TENANT_A, "Dr. Carlos", "carlos@crm.local");
        exec(conn, "INSERT INTO contacts (id, company_id, first_name, last_name, email) "
                + "VALUES (?, ?, ?, ?, ?)", C_ANA, TENANT_A, "Ana", "Souza", "ana.s@mail.com");
        exec(conn, "INSERT INTO catalog_items (id, name) VALUES (?, ?)",
                CI_LIMPEZA, "Limpeza");
        // Profissional SÓ da empresa B (para garantir isolamento na resolução).
        exec(conn, "INSERT INTO users (id, company_id, name, email) VALUES (?, ?, ?, ?)",
                UUID.fromString("55555555-0000-0000-0000-000000000099"), TENANT_B,
                "Dra. Bloqueada", "bloqueada@b.com");
        // Agendas (empresa A) e orçamento no mês corrente.
        exec(conn, "INSERT INTO appointments (id, company_id, host_id, title, start_at, end_at, status) "
                + "VALUES (?, ?, ?, ?, TIMESTAMPTZ '2026-01-05 10:00:00+00', "
                + "TIMESTAMPTZ '2026-01-05 11:00:00+00', 'COMPLETED')",
                UUID.randomUUID(), TENANT_A, U_ANA, "Agenda 1");
        exec(conn, "INSERT INTO appointments (id, company_id, host_id, title, start_at, end_at, status) "
                + "VALUES (?, ?, ?, ?, TIMESTAMPTZ '2026-01-06 10:00:00+00', "
                + "TIMESTAMPTZ '2026-01-06 11:00:00+00', 'CONFIRMED')",
                UUID.randomUUID(), TENANT_A, U_ANA, "Agenda 2");
        exec(conn, "INSERT INTO appointments (id, company_id, host_id, title, start_at, end_at, status) "
                + "VALUES (?, ?, ?, ?, TIMESTAMPTZ '2026-02-02 09:00:00+00', "
                + "TIMESTAMPTZ '2026-02-02 10:00:00+00', 'NO_SHOW')",
                UUID.randomUUID(), TENANT_A, U_CARLOS, "Agenda 3");
        exec(conn, "INSERT INTO appointments (id, company_id, host_id, title, start_at, end_at, status) "
                + "VALUES (?, ?, ?, ?, TIMESTAMPTZ '2026-03-03 14:00:00+00', "
                + "TIMESTAMPTZ '2026-03-03 15:00:00+00', 'COMPLETED')",
                UUID.randomUUID(), TENANT_A, U_ANA, "Agenda 4");
        exec(conn, "INSERT INTO appointments (id, company_id, host_id, title, start_at, end_at, status) "
                + "VALUES (?, ?, ?, ?, now(), now() + interval '1 hour', 'COMPLETED')",
                UUID.randomUUID(), TENANT_A, U_ANA, "Agenda agora");
        exec(conn, "INSERT INTO opportunities (id, company_id, title, created_at) "
                + "VALUES (?, ?, ?, TIMESTAMP '2026-01-15 09:00:00')",
                UUID.randomUUID(), TENANT_A, "Orcamento 1");
        exec(conn, "INSERT INTO opportunities (id, company_id, title, created_at) "
                + "VALUES (?, ?, ?, now())",
                UUID.randomUUID(), TENANT_A, "Orcamento agora");
    }

    private static void seedFinancialData(Connection conn) throws SQLException {
        for (int month = 1; month <= 6; month++) {
            LocalDate paid = LocalDate.of(2026, month, 10);
            insertTransaction(conn, TENANT_A, "INCOME", "Mensal receita", "PROCEDURE", 8000L,
                    300L, "PIX", paid, paid, "PAID", null, null, null);
            LocalDate paidRent = LocalDate.of(2026, month, 5);
            insertTransaction(conn, TENANT_A, "EXPENSE", "Aluguel mensal", "RENT", 4000L,
                    0L, "BOLETO", paidRent, paidRent, "PAID", null, null, null);
        }
        // Extras em Janeiro (receita com profissional/contato/item, pendentes, cancelado).
        insertTransaction(conn, TENANT_A, "INCOME", "Consulta clinica", "CONSULTATION", 20000L,
                0L, "CREDIT_CARD", LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 12),
                "PAID", C_ANA, U_ANA, CI_LIMPEZA);
        insertTransaction(conn, TENANT_A, "INCOME", "Venda de produto aguardando", "PRODUCT_SALE",
                15000L, 0L, "PIX", LocalDate.of(2026, 1, 20), null, "PENDING", null, null, null);
        insertTransaction(conn, TENANT_A, "EXPENSE", "Folha de pagamento", "PAYROLL",
                30000L, 0L, "TRANSFER", LocalDate.of(2026, 1, 20), null, "PENDING", null, null, null);
        insertTransaction(conn, TENANT_A, "INCOME", "Cancelado", "PROCEDURE", 999999L,
                0L, "PIX", LocalDate.of(2026, 1, 15), null, "CANCELLED", null, null, null);
        // Extra em Fevereiro.
        insertTransaction(conn, TENANT_A, "INCOME", "Venda de produto", "PRODUCT_SALE",
                7000L, 0L, "CASH", LocalDate.of(2026, 2, 8), LocalDate.of(2026, 2, 8),
                "PAID", null, null, null);
        // Empresa B: um registro que A nunca pode enxergar.
        insertTransaction(conn, TENANT_B, "INCOME", "Soma secreta B", "PROCEDURE", 5_000_000L,
                0L, "PIX", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 10), "PAID",
                null, null, null);
    }

    private static void insertTransaction(Connection conn, UUID companyId, String type, String description,
                                          String category, long amountCents, long feeCents, String method,
                                          LocalDate due, LocalDate paid, String status, UUID contactId,
                                          UUID professionalId, UUID catalogItemId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO financial_transactions (company_id, type, description, category,
                    amount_cents, fee_cents, payment_method, due_date, paid_at, status,
                    contact_id, professional_user_id, catalog_item_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            ps.setObject(1, companyId);
            ps.setString(2, type);
            ps.setString(3, description);
            ps.setString(4, category);
            ps.setLong(5, amountCents);
            ps.setLong(6, feeCents);
            ps.setString(7, method);
            ps.setObject(8, due);
            ps.setObject(9, paid);
            ps.setString(10, status);
            ps.setObject(11, contactId);
            ps.setObject(12, professionalId);
            ps.setObject(13, catalogItemId);
            ps.executeUpdate();
        }
    }

    private static void exec(Connection conn, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            ps.executeUpdate();
        }
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

    private static LocalDate jan() {
        return LocalDate.of(2026, 1, 31);
    }

    // =========================================================================
    // ETAPA 2 — FinanceReportGateway
    // =========================================================================

    @Test
    void summaryTotalsKeepsPaidPendingAndSkipsCancelled() {
        List<FinanceReportGateway.SummarySumRow> rows = financeGateway.summaryTotals(TENANT_A,
                new SummaryFilter(null, null, null, LocalDate.of(2026, 1, 1), jan(),
                        null, null, null, null, null));

        Map<String, Long> totals = new HashMap<>();
        for (FinanceReportGateway.SummarySumRow row : rows) {
            totals.put(row.type() + ":" + row.status(), row.amountCents());
        }
        assertEquals(28000L, totals.get("INCOME:PAID"));
        assertEquals(15000L, totals.get("INCOME:PENDING"));
        assertEquals(4000L, totals.get("EXPENSE:PAID"));
        assertEquals(30000L, totals.get("EXPENSE:PENDING"));
        assertNull(totals.get("INCOME:CANCELLED"));
    }

    @Test
    void healthTotalsReturnsTheSixPaidMonths() {
        List<FinanceReportGateway.HealthMonthRow> rows = financeGateway.healthTotals(TENANT_A,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

        Map<String, long[]> byMonth = new HashMap<>();
        for (FinanceReportGateway.HealthMonthRow row : rows) {
            long[] pair = byMonth.computeIfAbsent(row.month(), key -> new long[2]);
            if (row.type() == TransactionType.INCOME) {
                pair[0] += row.amountCents();
            } else {
                pair[1] += row.amountCents();
            }
        }
        assertEquals(6, byMonth.size());
        assertEquals(28000L, byMonth.get("2026-01")[0]);
        assertEquals(4000L, byMonth.get("2026-01")[1]);
        assertEquals(15000L, byMonth.get("2026-02")[0]);
        assertEquals(4000L, byMonth.get("2026-03")[1]);
        assertEquals(8000L, byMonth.get("2026-06")[0]);
    }

    @Test
    void historyTotalsBuildsPaidForecastAndDefaultBuckets() {
        List<FinanceReportGateway.HistoryRow> rows = financeGateway.historyTotals(TENANT_A,
                ReportGranularity.MONTH, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 6, 30));

        long janPaidIncome = -1;
        long janForecastIncome = -1;
        long janDefaultExpense = -1;
        for (FinanceReportGateway.HistoryRow row : rows) {
            if (row.period().equals(LocalDate.of(2026, 1, 1)) && row.bucket().equals("PAID")
                    && row.type() == TransactionType.INCOME) {
                janPaidIncome = row.amountCents();
            }
            if (row.period().equals(LocalDate.of(2026, 1, 1)) && row.bucket().equals("FORECAST")
                    && row.type() == TransactionType.INCOME) {
                janForecastIncome = row.amountCents();
            }
            if (row.period().equals(LocalDate.of(2026, 1, 1)) && row.bucket().equals("DEFAULT")
                    && row.type() == TransactionType.EXPENSE) {
                janDefaultExpense = row.amountCents();
            }
        }
        assertEquals(28000L, janPaidIncome);
        assertEquals(43000L, janForecastIncome);
        assertEquals(30000L, janDefaultExpense);
    }

    @Test
    void revenueByProcedureAverageAndProfessional() {
        List<FinanceReportGateway.ProcedureTotalsRow> byProcedure = financeGateway.revenueByProcedure(
                TENANT_A, ReportGranularity.MONTH, LocalDate.of(2026, 1, 1), jan(), List.of());
        assertEquals(20000L, byProcedure.stream().filter(r -> r.catalogItemId().equals(CI_LIMPEZA))
                .mapToLong(FinanceReportGateway.ProcedureTotalsRow::amountCents).sum());
        assertEquals("Limpeza", byProcedure.get(0).name());

        List<FinanceReportGateway.ProcedureAverageRow> average = financeGateway.revenueAverageByProcedure(
                TENANT_A, LocalDate.of(2026, 1, 1), jan(), List.of());
        assertEquals(20000L, average.get(0).averageCents());

        List<FinanceReportGateway.ProfessionalTotalsRow> byProfessional = financeGateway
                .revenueByProfessional(TENANT_A, ReportGranularity.MONTH,
                        LocalDate.of(2026, 1, 1), jan());
        assertEquals(20000L, byProfessional.stream().filter(r -> U_ANA.equals(r.professionalUserId()))
                .mapToLong(FinanceReportGateway.ProfessionalTotalsRow::amountCents).sum());
        assertEquals(8000L, byProfessional.stream().filter(r -> r.professionalUserId() == null)
                .mapToLong(FinanceReportGateway.ProfessionalTotalsRow::amountCents).sum());
    }

    @Test
    void overviewPractitionersAndAppointments() {
        List<FinanceReportGateway.UserRow> users = financeGateway.practitioners(TENANT_A,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), List.of());
        assertEquals(2, users.size());
        assertTrue(users.stream().map(FinanceReportGateway.UserRow::name)
                .anyMatch(name -> name.equals("Dr. Ana")));

        List<FinanceReportGateway.AgendaStatusRow> agenda = financeGateway.agendaByStatus(TENANT_A,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), null, List.of(), List.of());
        long total = agenda.stream().mapToLong(FinanceReportGateway.AgendaStatusRow::count).sum();
        assertEquals(4, total);

        List<FinanceReportGateway.ProcedureCountRow> counts = financeGateway.procedureCountsByPeriod(
                TENANT_A, ReportGranularity.MONTH, LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31));
        assertEquals(1L, counts.stream().filter(r -> r.bucket().equals("DONE"))
                .filter(r -> r.period().equals(LocalDate.of(2026, 1, 1)))
                .mapToLong(FinanceReportGateway.ProcedureCountRow::count).sum());
    }

    @Test
    void headlineUsesMonthWeekWindowAndCurrentMonthBudgets() {
        LocalDate today = LocalDate.now();
        assertEquals(28000L, financeGateway.paidIncome(TENANT_A, LocalDate.of(2026, 1, 1), jan()));
        assertEquals(1L, financeGateway.attendancesThisWeek(TENANT_A, today.withDayOfMonth(1),
                today.withDayOfMonth(today.lengthOfMonth())));
        assertEquals(1L, financeGateway.budgetsCreatedThisMonth(TENANT_A, today.withDayOfMonth(1),
                today.plusMonths(1).withDayOfMonth(1)));
    }

    // =========================================================================
    // ETAPA 3 — AccountingReportGateway
    // =========================================================================

    @Test
    void availableMonthsListedDescending() {
        List<String> months = accountingGateway.availableMonths(TENANT_A);

        assertEquals(6, months.size());
        assertEquals("2026-06", months.get(0));
        assertEquals("2026-01", months.get(5));
    }

    @Test
    void resolveContactAndProfessionalAreTenantScoped() {
        assertEquals(C_ANA, accountingGateway.resolveContact(TENANT_A, "Ana Souza"));
        assertNull(accountingGateway.resolveContact(TENANT_A, "Pessoa inexistente"));
        assertEquals(U_CARLOS, accountingGateway.resolveProfessional(TENANT_A, "Dr. Carlos"));
        assertEquals(U_CARLOS, accountingGateway.resolveProfessional(TENANT_A, "carlos@crm.local"));
        assertNull(accountingGateway.resolveProfessional(TENANT_A, "Dra. Bloqueada"));
    }

    @Test
    void monthlyAccountingStatementAndTransactionDetails() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = jan();

        List<AccountingReportGateway.CategoryTotalRow> monthly = accountingGateway.monthlyAccounting(
                TENANT_A, start, end);
        long income = monthly.stream().filter(r -> r.type() == TransactionType.INCOME)
                .mapToLong(AccountingReportGateway.CategoryTotalRow::totalCents).sum();
        long expense = monthly.stream().filter(r -> r.type() == TransactionType.EXPENSE)
                .mapToLong(AccountingReportGateway.CategoryTotalRow::totalCents).sum();
        long fee = monthly.stream().mapToLong(AccountingReportGateway.CategoryTotalRow::feeCents).sum();
        assertEquals(28000L, income);
        assertEquals(4000L, expense);
        assertEquals(300L, fee);

        List<AccountingReportGateway.StatementRow> statement = accountingGateway.statement(TENANT_A,
                start, end);
        assertEquals(3, statement.size());
        assertEquals("Aluguel mensal", statement.get(0).description());

        List<AccountingReportGateway.TransactionDetailRow> details = accountingGateway
                .transactionDetails(TENANT_A, start, end);
        assertEquals(5, details.size());
        assertTrue(details.stream().noneMatch(r -> r.status() == TransactionStatus.CANCELLED));
        assertTrue(details.stream().anyMatch(r -> "Ana Souza".equals(r.contactName())));
        assertTrue(details.stream().anyMatch(r -> "Dr. Ana".equals(r.professionalName())));
        assertTrue(details.stream().anyMatch(r -> r.paymentMethod() == PaymentMethod.PIX));
    }
}