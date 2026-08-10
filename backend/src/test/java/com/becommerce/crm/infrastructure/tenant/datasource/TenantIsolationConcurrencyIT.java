package com.becommerce.crm.infrastructure.tenant.datasource;

import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de integração REAL (Testcontainers PostgreSQL 17 + HikariCP) que
 * comprova o isolamento cross-tenant via RLS sob concorrência, com pool
 * reduzido para forçar REUSO das mesmas conexões físicas.
 */
@Testcontainers
class TenantIsolationConcurrencyIT {

    private static final Logger log = LoggerFactory.getLogger(TenantIsolationConcurrencyIT.class);

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("crm_it")
            .withUsername("crm_superuser")
            .withPassword("crm_it_pass");

    static final String APP_USER = "crm_app_user";
    static final String APP_PASSWORD = "crm_app_pass";

    static HikariDataSource rawPool;
    static TenantAwareDataSource tenantAwareDataSource;

    static final UUID TENANT_A = UUID.fromString("11111111-2222-3333-4444-555555555551");
    static final UUID TENANT_B = UUID.fromString("11111111-2222-3333-4444-555555555552");

    @BeforeAll
    static void setupDatabase() throws Exception {
        // 1. Bootstrap do schema (como superuser do container)
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("tenant-rls-bootstrap.sql"));

            // 2. Criar usuário de APLICAÇÃO não-superuser (igual à VPS deveria ser)
            //    Superuser/owner BYPASSA RLS — este é exatamente o risco real da VPS.
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE ROLE " + APP_USER + " LOGIN PASSWORD '" + APP_PASSWORD + "' NOSUPERUSER NOBYPASSRLS");
                st.execute("GRANT USAGE ON SCHEMA public TO " + APP_USER);
                st.execute("GRANT USAGE ON SCHEMA app TO " + APP_USER);
                st.execute("GRANT ALL ON ALL TABLES IN SCHEMA public TO " + APP_USER);
                st.execute("GRANT EXECUTE ON FUNCTION app.current_tenant_id() TO " + APP_USER);
                st.execute("ALTER TABLE users OWNER TO " + APP_USER);
                st.execute("ALTER TABLE roles OWNER TO " + APP_USER);
                st.execute("ALTER TABLE user_roles OWNER TO " + APP_USER);
                st.execute("ALTER TABLE audit_logs OWNER TO " + APP_USER);
                st.execute("ALTER TABLE refresh_tokens OWNER TO " + APP_USER);
                st.execute("ALTER TABLE password_reset_tokens OWNER TO " + APP_USER);
                st.execute("ALTER TABLE companies OWNER TO " + APP_USER);
            }
        }

        // 3. Pool conecta com o usuário NÃO-superuser (espelha produção correta)
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(APP_USER);
        config.setPassword(APP_PASSWORD);
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(2);
        config.setIdleTimeout(60000);
        config.setConnectionTimeout(5000);
        rawPool = new HikariDataSource(config);
        tenantAwareDataSource = new TenantAwareDataSource(rawPool);

        seedTenants();
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

    private static void seedTenants() throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            insertCompany(conn, TENANT_A, "Tenant A LTDA", "11.111.111/0001-11", "a@crm.local");
            insertCompany(conn, TENANT_B, "Tenant B LTDA", "22.222.222/0002-22", "b@crm.local");
        }

        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            insertUser(conn, "user.a1@crm.local", TENANT_A);
            insertUser(conn, "user.a2@crm.local", TENANT_A);
        }

        TenantContext.setCompanyId(TENANT_B);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            insertUser(conn, "user.b1@crm.local", TENANT_B);
            insertUser(conn, "user.b2@crm.local", TENANT_B);
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

    private static void insertUser(Connection conn, String email, UUID companyId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO users (email, password_hash, name, first_name, company_id, is_active,
                    created_at, updated_at, status, language, timezone)
                VALUES (?, 'hash', ?, 'First', ?, TRUE, NOW(), NOW(), 'ACTIVE', 'pt-BR', 'America/Sao_Paulo')
                RETURNING id
                """)) {
            ps.setString(1, email);
            ps.setString(2, email);
            ps.setObject(3, companyId);
            ps.execute();
        }
    }

    // =========================================================================
    // Testes
    // =========================================================================

    @Test
    void tenantA_shouldOnlySeeOwnUsers() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM users")) {
            rs.next();
            assertEquals(2, rs.getInt(1), "Tenant A deve ver apenas seus 2 usuários");
        }
    }

    @Test
    void tenantB_shouldOnlySeeOwnUsers() throws SQLException {
        TenantContext.setCompanyId(TENANT_B);
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM users")) {
            rs.next();
            assertEquals(2, rs.getInt(1), "Tenant B deve ver apenas seus 2 usuários");
        }
    }

    @Test
    void noContext_shouldSeeNothing() throws SQLException {
        TenantContext.clear();
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM users")) {
            rs.next();
            assertEquals(0, rs.getInt(1), "Sem contexto de tenant não deve ver nenhuma linha");
        }
    }

    @Test
    void crossTenantSelect_shouldReturnEmpty() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT count(*) FROM users WHERE company_id = ?")) {
            ps.setObject(1, TENANT_B);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(0, rs.getInt(1),
                        "Tenant A tentando ler usuários de B deve ver 0 (RLS esconde a linha)");
            }
        }
    }

    @Test
    void crossTenantInsert_shouldBeBlockedByRLS() {
        TenantContext.setCompanyId(TENANT_A);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                         INSERT INTO users (email, password_hash, name, first_name, company_id, is_active,
                             created_at, updated_at, status, language, timezone)
                         VALUES ('invader@evil.com', 'hash', 'Invader', 'Invader', ?, TRUE,
                             NOW(), NOW(), 'ACTIVE', 'pt-BR', 'America/Sao_Paulo')
                         """)) {
                ps.setObject(1, TENANT_B);
                ps.executeUpdate();
            }
        }, "INSERT cross-tenant deve lançar exceção de RLS");
    }

    @Test
    void crossTenantUpdate_shouldAffectZeroRows() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE users SET name = 'hacked' WHERE company_id = ?")) {
            ps.setObject(1, TENANT_B);
            int updated = ps.executeUpdate();
            assertEquals(0, updated, "UPDATE cross-tenant deve afetar 0 linhas");
        }
    }

    @Test
    void crossTenantDelete_shouldAffectZeroRows() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM users WHERE company_id = ?")) {
            ps.setObject(1, TENANT_B);
            int deleted = ps.executeUpdate();
            assertEquals(0, deleted, "DELETE cross-tenant deve afetar 0 linhas");
        }
    }

    @Test
    void refreshToken_isolationBetweenTenants() throws SQLException {
        insertToken(TENANT_B, "token_b_001", "family_b");

        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT count(*) FROM refresh_tokens WHERE token = ?")) {
            ps.setString(1, "token_b_001");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(0, rs.getInt(1),
                        "Tenant A não pode ver refresh token de usuário de B");
            }
        }
    }

    @Test
    void passwordResetToken_isolationBetweenTenants() throws SQLException {
        UUID tokenOfB = insertPasswordResetToken(TENANT_B);

        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT count(*) FROM password_reset_tokens WHERE id = ?")) {
            ps.setObject(1, tokenOfB);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(0, rs.getInt(1),
                        "Tenant A não pode ver password reset token de usuário de B");
            }
        }
    }

    @Test
    void auditLog_isolationBetweenTenants() throws SQLException {
        TenantContext.setCompanyId(TENANT_B);
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     INSERT INTO audit_logs (company_id, user_id, action, module, entity_name, entity_id,
                         description, status, success, created_at)
                     VALUES (?, NULL, 'CREATE', 'USERS', 'User', 'some-id', 'desc', 'SUCCESS', TRUE, NOW())
                     """)) {
            ps.setObject(1, TENANT_B);
            ps.executeUpdate();
        }

        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT count(*) FROM audit_logs")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(0, rs.getInt(1),
                        "Tenant A não deve ver audit logs de B");
            }
        }
    }

    // =========================================================================
    // CONCORRÊNCIA — pool reduzido (2) força REUSO das mesmas conexões
    // =========================================================================

    @Test
    void concurrentRequests_shouldNeverLeakCrossTenantData() throws Exception {
        int threads = 8;
        int iterationsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger violations = new AtomicInteger();
        AtomicInteger operations = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final UUID tenant = (t % 2 == 0) ? TENANT_A : TENANT_B;
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                for (int i = 0; i < iterationsPerThread; i++) {
                    TenantContext.setCompanyId(tenant);
                    try (Connection conn = tenantAwareDataSource.getConnection();
                         Statement st = conn.createStatement();
                         ResultSet rs = st.executeQuery("SELECT count(*) FROM users")) {
                        rs.next();
                        int count = rs.getInt(1);
                        operations.incrementAndGet();
                        if (count != 2) {
                            violations.incrementAndGet();
                        }
                    } catch (Throwable ex) {
                        failure.compareAndSet(null, ex);
                    }
                }
                return null;
            }));
        }

        ready.await(10, TimeUnit.SECONDS);
        start.countDown();

        for (Future<?> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        if (failure.get() != null) {
            throw new AssertionError("Falha inesperada em operação concorrente", failure.get());
        }

        int expected = threads * iterationsPerThread;
        assertEquals(expected, operations.get(), "Todas as operações concorrentes devem executar");
        assertEquals(0, violations.get(),
                "Nenhuma thread pode ver dados de outro tenant (" + violations.get() + " violações)");
        log.info("Concorrência OK: {} operações, 0 violações cross-tenant (pool size=2)", operations.get());
    }

    @Test
    void sequentialReuse_samePoolConnections_shouldResetContext() throws SQLException {
        // A sequência abaixo reaproveita as 2 conexões do pool alternando tenants.
        // Garante que o contexto de A nunca permanece quando B usa a conexão.
        for (int i = 0; i < 30; i++) {
            TenantContext.setCompanyId(TENANT_A);
            assertCountUsers(2, "Tenant A (iteração " + i + ")");

            TenantContext.setCompanyId(TENANT_B);
            assertCountUsers(2, "Tenant B (iteração " + i + ")");

            TenantContext.clear();
            assertCountUsers(0, "Sem contexto (iteração " + i + ")");
        }
    }

    /**
     * Sprint 8.4 — Company Switcher: alternar a empresa ativa (via atualização de
     * {@code users.company_id}, propagada ao {@code TenantContext} → GUC
     * {@code app.current_company_id}) deve trocar a visão RLS. Empresa A ativa →
     * só dados de A; switch B → só dados de B (nada de A); switch A → só dados de A.
     */
    @Test
    void switchingActiveCompany_togglesTenantIsolation() throws SQLException {
        // Empresa A ativa → vê apenas dados de A
        TenantContext.setCompanyId(TENANT_A);
        assertCountUsers(2, "Empresa A ativa → vê apenas seus dados");
        assertCrossTenantCount(TENANT_B, 0, "Empresa A ativa não pode ver dados de B");

        // switch → Empresa B ativa → vê apenas dados de B (nada de A)
        TenantContext.setCompanyId(TENANT_B);
        assertCountUsers(2, "Empresa B ativa → vê apenas seus dados");
        assertCrossTenantCount(TENANT_A, 0, "Empresa B ativa não pode ver dados de A");

        // switch de volta → Empresa A ativa → vê apenas dados de A
        TenantContext.setCompanyId(TENANT_A);
        assertCountUsers(2, "Empresa A reativada → vê apenas seus dados");
        assertCrossTenantCount(TENANT_B, 0, "Empresa A reativada não pode ver dados de B");
    }

    private void assertCrossTenantCount(UUID otherTenant, int expected, String message) throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT count(*) FROM users WHERE company_id = ?")) {
            ps.setObject(1, otherTenant);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(expected, rs.getInt(1), message);
            }
        }
    }

    private void assertCountUsers(int expected, String message) throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM users")) {
            rs.next();
            assertEquals(expected, rs.getInt(1), message);
        }
    }

    private UUID insertToken(UUID tenant, String token, String family) throws SQLException {
        UUID userId = firstUserId(tenant);
        TenantContext.setCompanyId(tenant);
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     INSERT INTO refresh_tokens (user_id, token, family, expires_at, is_revoked, created_at)
                     VALUES (?, ?, ?, NOW() + INTERVAL '1 day', FALSE, NOW())
                     """)) {
            ps.setObject(1, userId);
            ps.setString(2, token);
            ps.setString(3, family);
            ps.execute();
        }
        return userId;
    }

    private UUID insertPasswordResetToken(UUID tenant) throws SQLException {
        UUID userId = firstUserId(tenant);
        TenantContext.setCompanyId(tenant);
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     INSERT INTO password_reset_tokens (token, user_id, expires_at, used, created_at)
                     VALUES (?, ?, NOW() + INTERVAL '1 day', FALSE, NOW())
                     RETURNING id
                     """)) {
            ps.setString(1, "reset_token_" + UUID.randomUUID());
            ps.setObject(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getObject(1, UUID.class);
            }
        }
    }

    private UUID firstUserId(UUID tenant) throws SQLException {
        TenantContext.setCompanyId(tenant);
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM users WHERE company_id = ? LIMIT 1")) {
            ps.setObject(1, tenant);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject(1, UUID.class);
                }
            }
        }
        throw new IllegalStateException("Nenhum usuário para tenant " + tenant);
    }
}
