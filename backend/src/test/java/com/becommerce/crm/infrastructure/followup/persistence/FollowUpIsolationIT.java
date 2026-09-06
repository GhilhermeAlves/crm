package com.becommerce.crm.infrastructure.followup.persistence;

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
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de integração REAL (Testcontainers PostgreSQL 17 + HikariCP) do módulo
 * FollowUp (Sprint 22), executado no CI/VPS:
 * <ol>
 *   <li><b>Isolamento cross-tenant</b> — follow-ups de A jamais visíveis/alteráveis
 *       por B (RLS FORCE) e INSERT cross-tenant bloqueado (WITH CHECK).</li>
 *   <li><b>Defesa em profundidade</b> — FK composta (conversation_id, company_id)
 *       impede follow-up apontando para conversa de outra empresa.</li>
 *   <li><b>Idempotência de criação</b> — chave única (company_id, idempotency_key).</li>
 *   <li><b>Claim atômico concorrente</b> — N workers disputando o mesmo follow-up:
 *       exatamente UMA execução vence (idempotência de execução).</li>
 *   <li><b>Scheduler multi-tenant</b> — {@code app.followup_scheduler_candidates}
 *       (SECURITY DEFINER) enumera follow-ups vencidos em TODAS as empresas mesmo
 *       no contexto de tenant de B ou sem contexto.</li>
 * </ol>
 */
@Testcontainers
class FollowUpIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("crm_it_followup")
            .withUsername("crm_superuser")
            .withPassword("crm_it_pass");

    static final String APP_USER = "crm_app_user";
    static final String APP_PASSWORD = "crm_app_pass";

    static HikariDataSource rawPool;
    static TenantAwareDataSource tenantAwareDataSource;

    static final UUID TENANT_A = UUID.fromString("11111111-2222-3333-4444-555555555551");
    static final UUID TENANT_B = UUID.fromString("11111111-2222-3333-4444-555555555552");

    static final UUID CHANNEL_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    static final UUID CHANNEL_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    static final UUID CONV_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000011");
    static final UUID CONV_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000012");

    static final int CONCURRENT_THREADS = 8;

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("followup-rls-bootstrap.sql"));
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE ROLE " + APP_USER + " LOGIN PASSWORD '" + APP_PASSWORD
                        + "' NOSUPERUSER NOBYPASSRLS");
                st.execute("GRANT USAGE ON SCHEMA public TO " + APP_USER);
                st.execute("GRANT USAGE ON SCHEMA app TO " + APP_USER);
                st.execute("GRANT ALL ON ALL TABLES IN SCHEMA public TO " + APP_USER);
                st.execute("GRANT EXECUTE ON FUNCTION app.current_tenant_id() TO " + APP_USER);
                st.execute("GRANT EXECUTE ON FUNCTION app.followup_scheduler_candidates(INT) TO " + APP_USER);
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
        TenantContext.setCompanyId(TENANT_A);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            insertCompany(conn, TENANT_A, "FollowUp Tenant A LTDA", "13.131.131/0001-13", "a.followup@crm.local");
            insertChannel(conn, CHANNEL_A, TENANT_A, "espaco-a");
            insertConversation(conn, CONV_A, TENANT_A, CHANNEL_A);
        }

        TenantContext.setCompanyId(TENANT_B);
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            insertCompany(conn, TENANT_B, "FollowUp Tenant B LTDA", "24.242.242/0002-24", "b.followup@crm.local");
            insertChannel(conn, CHANNEL_B, TENANT_B, "espaco-b");
            insertConversation(conn, CONV_B, TENANT_B, CHANNEL_B);
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

    private static void insertChannel(Connection conn, UUID id, UUID companyId, String externalId)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO omnichannel_channels (id, company_id, type, provider, name, external_id)
                VALUES (?, ?, 'WHATSAPP', 'FAKE', 'WhatsApp ' || ?, ?)
                """)) {
            ps.setObject(1, id);
            ps.setObject(2, companyId);
            ps.setString(3, companyId.toString());
            ps.setString(4, externalId);
            ps.executeUpdate();
        }
    }

    private static void insertConversation(Connection conn, UUID id, UUID companyId, UUID channelId)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO omnichannel_conversations (id, company_id, channel_id, external_phone, status)
                VALUES (?, ?, ?, ?, 'OPEN')
                """)) {
            ps.setObject(1, id);
            ps.setObject(2, companyId);
            ps.setObject(3, channelId);
            ps.setString(4, "+550011112222");
            ps.executeUpdate();
        }
    }

    private UUID insertFollowUp(UUID companyId, UUID conversationId, LocalDateTime executeAt, UUID idempotencyKey)
            throws SQLException {
        UUID id = UUID.randomUUID();
        TenantContext.setCompanyId(companyId);
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     INSERT INTO followups (id, company_id, conversation_id, status, action_type,
                         action_content, execute_at, idempotency_key)
                     VALUES (?, ?, ?, 'PENDING', 'SEND_MESSAGE', 'Podemos retomar?', ?, ?)
                     """)) {
            ps.setObject(1, id);
            ps.setObject(2, companyId);
            ps.setObject(3, conversationId);
            ps.setObject(4, executeAt);
            ps.setObject(5, idempotencyKey);
            ps.executeUpdate();
        } finally {
            TenantContext.clear();
        }
        return id;
    }

    // ---------------------- Isolamento / RLS -------------------------------

    @Test
    void tenantA_shouldOnlySeeOwnFollowUps() throws SQLException {
        insertFollowUp(TENANT_A, CONV_A, LocalDateTime.now().plusHours(1), null);
        insertFollowUp(TENANT_B, CONV_B, LocalDateTime.now().plusHours(1), null);

        TenantContext.setCompanyId(TENANT_A);
        assertEquals(1, countFollowUps(), "A deve ver apenas o follow-up da própria empresa");
    }

    @Test
    void noContext_shouldSeeNothing() throws SQLException {
        insertFollowUp(TENANT_A, CONV_A, LocalDateTime.now().plusHours(1), null);
        TenantContext.clear();
        assertEquals(0, countFollowUps(), "Sem contexto de tenant não se enxerga nenhum follow-up");
    }

    @Test
    void crossTenantInsert_shouldBeBlockedByRls() throws SQLException {
        insertFollowUp(TENANT_A, CONV_A, LocalDateTime.now().plusHours(1), null);
        TenantContext.setCompanyId(TENANT_A);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                         INSERT INTO followups (company_id, conversation_id, status, action_type,
                             action_content, execute_at)
                         VALUES (?, ?, 'PENDING', 'SEND_MESSAGE', 'Invasao', ?)
                         """)) {
                ps.setObject(1, TENANT_B);
                ps.setObject(2, CONV_B);
                ps.setObject(3, LocalDateTime.now().plusHours(1));
                ps.executeUpdate();
            }
        }, "INSERT cross-tenant de follow-up deve ser bloqueado pela policy RLS");
    }

    @Test
    void crossTenantClaim_shouldNotAffectOtherTenant() throws SQLException {
        UUID due = insertFollowUp(TENANT_A, CONV_A, LocalDateTime.now().minusMinutes(5), null);
        TenantContext.setCompanyId(TENANT_B);
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     UPDATE followups SET status = 'PROCESSING'
                     WHERE id = ? AND company_id = ?
                     """)) {
            ps.setObject(1, due);
            ps.setObject(2, TENANT_A);
            assertEquals(0, ps.executeUpdate(), "B não pode alterar follow-up de A (RLS FORCE)");
        }
        TenantContext.setCompanyId(TENANT_A);
        assertEquals("PENDING", getStatus(due), "Follow-up de A deve continuar PENDING após tentativa de B");
    }

    @Test
    void followUpReferencingOtherCompanyConversation_shouldFailByCompoundFk() {
        TenantContext.setCompanyId(TENANT_A);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                         INSERT INTO followups (company_id, conversation_id, status, action_type,
                             action_content, execute_at)
                         VALUES (?, ?, 'PENDING', 'SEND_MESSAGE', 'x', ?)
                         """)) {
                ps.setObject(1, TENANT_A);
                ps.setObject(2, CONV_B);
                ps.setObject(3, LocalDateTime.now().plusHours(1));
                ps.executeUpdate();
            }
        }, "FK composta (conversation_id, company_id) deve impedir conversa de outra empresa");
    }

    // ---------------------- Idempotência de criação ------------------------

    @Test
    void duplicateIdempotencyKey_sameCompany_shouldViolateUnique() throws SQLException {
        UUID key = UUID.randomUUID();
        insertFollowUp(TENANT_A, CONV_A, LocalDateTime.now().plusHours(1), key);
        TenantContext.setCompanyId(TENANT_A);
        assertThrows(SQLException.class, () -> {
            try (Connection conn = tenantAwareDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                         INSERT INTO followups (company_id, conversation_id, status, action_type,
                             action_content, execute_at, idempotency_key)
                         VALUES (?, ?, 'PENDING', 'SEND_MESSAGE', 'dup', ?, ?)
                         """)) {
                ps.setObject(1, TENANT_A);
                ps.setObject(2, CONV_A);
                ps.setObject(3, LocalDateTime.now().plusHours(1));
                ps.setObject(4, key);
                ps.executeUpdate();
            }
        }, "Mesma idempotency_key na MESMA empresa deve violar a chave única");
    }

    @Test
    void sameIdempotencyKey_otherCompany_shouldBeAllowed() throws SQLException {
        UUID key = UUID.randomUUID();
        insertFollowUp(TENANT_A, CONV_A, LocalDateTime.now().plusHours(1), key);
        insertFollowUp(TENANT_B, CONV_B, LocalDateTime.now().plusHours(1), key);

        TenantContext.setCompanyId(TENANT_A);
        assertEquals(1, countFollowUpsWithKey(key), "A deve ter 1 follow-up com a chave");
        TenantContext.setCompanyId(TENANT_B);
        assertEquals(1, countFollowUpsWithKey(key), "B pode reutilizar a mesma chave (única por empresa)");
    }

    // ---------------------- Claim atômico concorrente ----------------------

    @Test
    void concurrentClaim_shouldExecuteExactlyOnce() throws Exception {
        UUID due = insertFollowUp(TENANT_A, CONV_A, LocalDateTime.now().minusMinutes(5), null);

        AtomicInteger winners = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(CONCURRENT_THREADS);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_THREADS);

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    TenantContext.setCompanyId(TENANT_A);
                    try (Connection conn = tenantAwareDataSource.getConnection();
                         PreparedStatement ps = conn.prepareStatement("""
                                 UPDATE followups
                                 SET status = 'PROCESSING', processing_started_at = ?, updated_at = ?
                                 WHERE id = ? AND company_id = ?
                                   AND execute_at <= ?
                                   AND (status = 'PENDING'
                                        OR (status = 'PROCESSING' AND processing_started_at < ?))
                                 """)) {
                        ps.setObject(1, LocalDateTime.now());
                        ps.setObject(2, LocalDateTime.now());
                        ps.setObject(3, due);
                        ps.setObject(4, TENANT_A);
                        ps.setObject(5, LocalDateTime.now());
                        ps.setObject(6, LocalDateTime.now().minusMinutes(15));
                        if (ps.executeUpdate() > 0) {
                            winners.incrementAndGet();
                        }
                    } finally {
                        TenantContext.clear();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
        }

        assertTrue(ready.await(10, TimeUnit.SECONDS), "workers devem ficar prontos");
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "workers devem terminar");
        assertTrue(pool.shutdownNow().isEmpty());

        assertEquals(1, winners.get(), "Exatamente UMA execução deve vencer o claim atômico");
        TenantContext.setCompanyId(TENANT_A);
        assertEquals("PROCESSING", getStatus(due), "Follow-up deve estar PROCESSING após o claim vencedor");
    }

    // ---------------------- Scheduler (SECURITY DEFINER) -------------------

    @Test
    void schedulerCandidates_shouldEnumerateDueFollowUpsAcrossTenants() throws SQLException {
        UUID dueInA = insertFollowUp(TENANT_A, CONV_A, LocalDateTime.now().minusMinutes(5), null);
        insertFollowUp(TENANT_B, CONV_B, LocalDateTime.now().plusHours(1), null);

        // Sem contexto de tenant (thread do scheduler): o app user NÃO enxerga
        // nada na tabela (RLS), mas o SECURITY DEFINER enxerga em todas as empresas.
        TenantContext.clear();
        try (Connection conn = rawPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT followup_id FROM app.followup_scheduler_candidates(100)")) {
            try (ResultSet rs = ps.executeQuery()) {
                int rows = 0;
                boolean sawDueInA = false;
                while (rs.next()) {
                    rows++;
                    if (dueInA.equals(rs.getObject("followup_id", UUID.class))) {
                        sawDueInA = true;
                    }
                }
                assertEquals(1, rows, "Apenas o follow-up VENCIDO deve ser candidato");
                assertTrue(sawDueInA, "Candidato de A deve aparecer sem contexto de tenant");
            }
        }
        assertEquals(0, countFollowUps(), "Ao mesmo tempo, o app user direto não enxerga nada (RLS)");
    }

    // ---------------------- helpers ---------------------------------------

    private int countFollowUps() throws SQLException {
        return countOf("SELECT count(*) FROM followups");
    }

    private int countFollowUpsWithKey(UUID key) throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT count(*) FROM followups WHERE idempotency_key = ?")) {
            ps.setObject(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private String getStatus(UUID followUpId) throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT status FROM followups WHERE id = ?")) {
            ps.setObject(1, followUpId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    private int countOf(String sql) throws SQLException {
        try (Connection conn = tenantAwareDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}