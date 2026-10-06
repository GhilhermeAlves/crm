package com.becommerce.crm.identity.invitation.infrastructure.persistence;

import com.becommerce.crm.identity.invitation.application.service.InvitationTokenService;
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
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Garantias do PostgreSQL usadas pelo aceite/cadastro por convite, com as
 * migrations REAIS {@code V036__invitations.sql} e {@code V084__invitations_invitee_name.sql}
 * e um role NOBYPASSRLS como o {@code crm_app}:
 *
 * <ul>
 *   <li>o {@code SELECT ... FOR NO KEY UPDATE} (o que o Hibernate emite para
 *       {@code PESSIMISTIC_WRITE}) passa pela policy de token e serializa duas
 *       transações sobre o mesmo convite — só uma aceita;</li>
 *   <li>sem o contexto do token, o lock não enxerga o convite (RLS);</li>
 *   <li>{@code users.email} UNIQUE barra o segundo cadastro simultâneo do mesmo e-mail;</li>
 *   <li>o índice parcial impede dois convites PENDING para o mesmo e-mail/empresa,
 *       mas permite um novo convite depois do aceite.</li>
 * </ul>
 */
@Testcontainers
class InvitationConcurrencyIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("crm_it_invitation")
            .withUsername("crm_superuser")
            .withPassword("crm_it_pass");

    static final String APP_USER = "crm_invitation_app_user";
    static final String APP_PASSWORD = "crm_invitation_app_pass";

    static final UUID COMPANY = UUID.fromString("44444444-5555-6666-7777-888888888881");
    static final String LOCK_SQL =
            "SELECT id, status FROM invitations WHERE token_hash = ? FOR NO KEY UPDATE";

    @BeforeAll
    static void setupDatabase() throws Exception {
        try (Connection conn = superuser()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("tenant-rls-bootstrap.sql"));
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE ROLE crm_app NOLOGIN NOBYPASSRLS");
                // Arquivo inteiro num único execute (como o Flyway), por causa dos blocos $$.
                st.execute(migration("V036__invitations.sql"));
                st.execute(migration("V084__invitations_invitee_name.sql"));
                st.execute("CREATE ROLE " + APP_USER + " LOGIN PASSWORD '" + APP_PASSWORD
                        + "' NOSUPERUSER NOBYPASSRLS");
                st.execute("GRANT crm_app TO " + APP_USER);
                st.execute("GRANT USAGE ON SCHEMA public TO " + APP_USER);
                st.execute("GRANT USAGE ON SCHEMA app TO " + APP_USER);
                st.execute("GRANT EXECUTE ON FUNCTION app.current_tenant_id() TO " + APP_USER);
            }
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO companies (id, legal_name, trading_name, cnpj, email, phone,
                        address_zip_code, address_street, address_number, address_neighborhood,
                        address_city, address_state, address_country, plan, status,
                        max_users, max_storage_mb)
                    VALUES (?, 'Convites LTDA', 'Convites', '45.555.555/0001-55', 'c@crm.local',
                        '0000-0000', '00000-000', 'Rua', '0', 'Centro', 'Sao Paulo', 'SP', 'Brasil',
                        'STARTER', 'ACTIVE', 10, 1024)""")) {
                ps.setObject(1, COMPANY);
                ps.executeUpdate();
            }
        }
    }

    @BeforeEach
    void cleanInvitations() throws SQLException {
        try (Connection conn = superuser(); Statement st = conn.createStatement()) {
            st.execute("DELETE FROM invitations");
            st.execute("DELETE FROM users");
        }
    }

    private static String migration(String file) throws Exception {
        return new ClassPathResource("db/migration/" + file).getContentAsString(StandardCharsets.UTF_8);
    }

    private static Connection superuser() throws SQLException {
        return postgres.createConnection("");
    }

    private static Connection appConnection() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), APP_USER, APP_PASSWORD);
    }

    private static String insertPendingInvitation(String email) throws SQLException {
        String token = InvitationTokenService.generateToken();
        try (Connection conn = superuser(); PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO invitations (company_id, email, invitee_name, role, token_hash, status, expires_at)
                VALUES (?, ?, 'Fulano', 'AGENT', ?, 'PENDING', now() + interval '7 days')""")) {
            ps.setObject(1, COMPANY);
            ps.setString(2, email);
            ps.setString(3, InvitationTokenService.hash(token));
            ps.executeUpdate();
        }
        return token;
    }

    private static void setTokenContext(Connection conn, String token) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT app.set_invitation_token_context(?)")) {
            ps.setString(1, InvitationTokenService.hash(token));
            ps.execute();
        }
    }

    /** Uma "requisição de aceite": lock, confere PENDING, segura o lock um pouco e aceita. */
    private static Callable<Boolean> acceptAttempt(String token, CyclicBarrier start) {
        return () -> {
            try (Connection conn = appConnection()) {
                conn.setAutoCommit(false);
                setTokenContext(conn, token);
                start.await(10, TimeUnit.SECONDS);
                try (PreparedStatement lock = conn.prepareStatement(LOCK_SQL)) {
                    lock.setString(1, InvitationTokenService.hash(token));
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next() || !"PENDING".equals(rs.getString("status"))) {
                            conn.rollback();
                            return false;
                        }
                        UUID id = rs.getObject("id", UUID.class);
                        Thread.sleep(300); // a outra transação fica esperando o lock
                        try (PreparedStatement upd = conn.prepareStatement(
                                "UPDATE invitations SET status = 'ACCEPTED', updated_at = now() WHERE id = ?")) {
                            upd.setObject(1, id);
                            upd.executeUpdate();
                        }
                    }
                }
                conn.commit();
                return true;
            }
        };
    }

    @Test
    void twoConcurrentAcceptsOfTheSameInvitation_onlyOneWins() throws Exception {
        String token = insertPendingInvitation("corrida@empresa.com");
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            results.add(pool.submit(acceptAttempt(token, start)));
            results.add(pool.submit(acceptAttempt(token, start)));

            int winners = 0;
            for (Future<Boolean> r : results) {
                if (r.get(30, TimeUnit.SECONDS)) {
                    winners++;
                }
            }
            assertEquals(1, winners);
        } finally {
            pool.shutdownNow();
        }

        try (Connection conn = superuser(); Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT status FROM invitations")) {
            rs.next();
            assertEquals("ACCEPTED", rs.getString(1));
        }
    }

    @Test
    void lockWithoutTokenContext_doesNotSeeTheInvitation() throws Exception {
        String token = insertPendingInvitation("rls@empresa.com");
        try (Connection conn = appConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement lock = conn.prepareStatement(LOCK_SQL)) {
                lock.setString(1, InvitationTokenService.hash(token));
                try (ResultSet rs = lock.executeQuery()) {
                    assertEquals(false, rs.next());
                }
            }
            conn.rollback();
        }
    }

    @Test
    void lockWithAnotherTokenContext_doesNotSeeTheInvitation() throws Exception {
        String token = insertPendingInvitation("outro@empresa.com");
        try (Connection conn = appConnection()) {
            conn.setAutoCommit(false);
            setTokenContext(conn, "token-de-outro-convite");
            try (PreparedStatement lock = conn.prepareStatement(LOCK_SQL)) {
                lock.setString(1, InvitationTokenService.hash(token));
                try (ResultSet rs = lock.executeQuery()) {
                    assertEquals(false, rs.next());
                }
            }
            conn.rollback();
        }
    }

    @Test
    void twoConcurrentSignupsWithTheSameEmail_onlyOneUserRow() throws Exception {
        CyclicBarrier start = new CyclicBarrier(2);
        Callable<Boolean> insert = () -> {
            try (Connection conn = superuser()) {
                conn.setAutoCommit(false);
                start.await(10, TimeUnit.SECONDS);
                try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT INTO users (email, password_hash, name, company_id, keycloak_sub)
                        VALUES ('mesmo@empresa.com', 'x', 'Mesmo', ?, ?)""")) {
                    ps.setObject(1, COMPANY);
                    ps.setString(2, UUID.randomUUID().toString());
                    ps.executeUpdate();
                    conn.commit();
                    return true;
                } catch (SQLException e) {
                    conn.rollback();
                    assertEquals("23505", e.getSQLState());
                    return false;
                }
            }
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> a = pool.submit(insert);
            Future<Boolean> b = pool.submit(insert);
            int created = (a.get(30, TimeUnit.SECONDS) ? 1 : 0) + (b.get(30, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, created);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void secondPendingInvitationForSameEmail_isRejected_butAllowedAfterAcceptance() throws Exception {
        insertPendingInvitation("unico@empresa.com");

        SQLException duplicate = assertThrows(SQLException.class,
                () -> insertPendingInvitation("unico@empresa.com"));
        assertEquals("23505", duplicate.getSQLState());

        try (Connection conn = superuser(); Statement st = conn.createStatement()) {
            st.execute("UPDATE invitations SET status = 'ACCEPTED' WHERE email = 'unico@empresa.com'");
        }
        insertPendingInvitation("unico@empresa.com"); // histórico ACCEPTED não bloqueia novo convite
    }

    @Test
    void inviteeNameIsOptional() throws Exception {
        try (Connection conn = superuser(); PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO invitations (company_id, email, role, token_hash, status, expires_at)
                VALUES (?, 'semnome@empresa.com', 'AGENT', ?, 'PENDING', now() + interval '7 days')""")) {
            ps.setObject(1, COMPANY);
            ps.setString(2, InvitationTokenService.hash(InvitationTokenService.generateToken()));
            assertEquals(1, ps.executeUpdate());
        }
    }
}
