package com.becommerce.crm.infrastructure.identity.persistence;

import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import com.becommerce.crm.infrastructure.tenant.datasource.TenantAwareDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 20 (Fase 2) — matriz de precedência de permissões efetivas
 * (perfis ∪ ALLOW) − DENY + isolamento multi-tenant dos overrides.
 *
 * SQL idêntico ao de produção (SpringDataPermissionRepository /
 * SpringDataUserRepository do auth-service).
 */
@Testcontainers
class UserPermissionsIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("uperm_it").withUsername("su").withPassword("pw");

    static final String APP_USER = "crm_app_user";
    static HikariDataSource rawPool;
    static NamedParameterJdbcTemplate jdbc;

    static final UUID TENANT_A = UUID.fromString("aaaaaaa1-0000-0000-0000-000000000001");
    static final UUID TENANT_B = UUID.fromString("bbbbbbb2-0000-0000-0000-000000000002");

    static final String ROLE_ONLY = "00000000-0000-0000-0000-00000000aa01";
    static final String DENIED_BY_USER = "00000000-0000-0000-0000-00000000aa02";
    static final String USER_ALLOW_ONLY = "00000000-0000-0000-0000-00000000aa03";
    static final String USER_DENY_NO_ROLE = "00000000-0000-0000-0000-00000000aa04";

    static UUID userA;

    private static final String EFFECTIVE_SQL = """
            SELECT name FROM (
                SELECT DISTINCT p.id, p.name
                FROM permissions p
                INNER JOIN role_permissions rp ON rp.permission_id = p.id
                INNER JOIN user_roles ur ON ur.role_id = rp.role_id
                WHERE ur.user_id = :userId AND ur.company_id = :companyId
                UNION
                SELECT p.id, p.name
                FROM user_permissions up
                INNER JOIN permissions p ON p.id = up.permission_id
                WHERE up.user_id = :userId AND up.company_id = :companyId
                  AND up.effect = 'ALLOW'
            ) eff
            WHERE id NOT IN (
                SELECT up.permission_id FROM user_permissions up
                WHERE up.user_id = :userId AND up.company_id = :companyId
                  AND up.effect = 'DENY'
            )
            ORDER BY name
            """;

    @BeforeAll
    static void setup() throws Exception {
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils(conn);
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE ROLE " + APP_USER + " LOGIN PASSWORD 'p' NOSUPERUSER NOBYPASSRLS");
                st.execute("GRANT USAGE ON SCHEMA public TO " + APP_USER);
                st.execute("GRANT USAGE ON SCHEMA app TO " + APP_USER);
                st.execute("GRANT ALL ON ALL TABLES IN SCHEMA public TO " + APP_USER);
                st.execute("GRANT EXECUTE ON FUNCTION app.current_tenant_id() TO " + APP_USER);
            }
        }
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(postgres.getJdbcUrl());
        cfg.setUsername(APP_USER);
        cfg.setPassword("p");
        cfg.setMaximumPoolSize(2);
        rawPool = new HikariDataSource(cfg);
        jdbc = new NamedParameterJdbcTemplate(new TenantAwareDataSource(rawPool));

        seedCompanies();
        seedScenario();
    }

    private static void ScriptUtils(Connection conn) throws SQLException {
        try {
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(
                    conn, new ClassPathResource("tenant-rls-bootstrap.sql"));
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(
                    conn, new ClassPathResource("user-permissions-bootstrap.sql"));
        } catch (Exception e) {
            throw new SQLException(e.getMessage(), e);
        }
    }

    @AfterAll
    static void tearDown() {
        if (rawPool != null) rawPool.close();
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private static void seedCompanies() throws SQLException {
        for (UUID id : new UUID[]{TENANT_A, TENANT_B}) {
            try (Connection conn = new TenantAwareDataSource(rawPool).getConnection()) {
                TenantContext.setCompanyId(id);
                try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT INTO companies (id, legal_name, trading_name, cnpj, email, phone,
                            address_zip_code, address_street, address_number, address_neighborhood,
                            address_city, address_state, address_country, plan, status, max_users, max_storage_mb)
                        VALUES (?, ?, ?, ?, ?, '1', '1', 's', 'n', 'b', 'c', 'SP', 'BR', 'FREE', 'ACTIVE', 10, 100)
                        ON CONFLICT (id) DO NOTHING""")) {
                    ps.setObject(1, id);
                    ps.setString(2, "T" + id);
                    ps.setString(3, "T" + id);
                    ps.setString(4, id.toString().substring(0, 18));
                    ps.setString(5, id + "@x");
                    ps.executeUpdate();
                } finally {
                    TenantContext.clear();
                }
            }
        }
    }

    private static void insertRoleWithPerm(UUID companyId, UUID roleId, String permissionId) {
        inTenant(companyId, () -> {
            jdbc.update("INSERT INTO roles (id, company_id, name) VALUES (:r, :c, 'PERFIL')",
                    Map.of("r", roleId, "c", companyId));
            jdbc.update("""
                    INSERT INTO role_permissions (role_id, permission_id)
                    VALUES (:r, :p::uuid)
                    """, Map.of("r", roleId, "p", permissionId));
        });
    }

    private static void grantRole(UUID userId, UUID companyId, UUID roleId) {
        inTenant(companyId, () -> jdbc.update(
                "INSERT INTO user_roles (user_id, role_id, company_id) VALUES (:u, :r, :c)",
                Map.of("u", userId, "r", roleId, "c", companyId)));
    }

    private static void override(UUID userId, UUID companyId, String permissionId, String effect) {
        inTenant(companyId, () -> jdbc.update("""
                INSERT INTO user_permissions (company_id, user_id, permission_id, effect)
                VALUES (:c, :u, :p::uuid, :e)
                ON CONFLICT (user_id, permission_id) DO UPDATE SET effect = EXCLUDED.effect
                """, Map.of("c", companyId, "u", userId, "p", permissionId, "e", effect)));
    }

    private static void inTenant(UUID companyId, Runnable work) {
        TenantContext.setCompanyId(companyId);
        try {
            work.run();
        } finally {
            TenantContext.clear();
        }
    }

    private List<String> effective(UUID userId, UUID companyId) {
        TenantContext.setCompanyId(companyId);
        try {
            return jdbc.queryForList(EFFECTIVE_SQL,
                    Map.of("userId", userId, "companyId", companyId), String.class);
        } finally {
            TenantContext.clear();
        }
    }

    private static void seedScenario() throws SQLException {
        userA = UUID.randomUUID();

        // PERFIL com role_only + dennied_by_user
        UUID perfilA = UUID.randomUUID();
        insertRoleWithPerm(TENANT_A, perfilA, ROLE_ONLY);
        insertRoleWithPerm(TENANT_A, perfilA, DENIED_BY_USER);
        grantRole(userA, TENANT_A, perfilA);

        // Overrides do usuário A:
        override(userA, TENANT_A, DENIED_BY_USER, "DENY");     // perfil ALLOW + usuário DENY → DENY
        override(userA, TENANT_A, USER_ALLOW_ONLY, "ALLOW");   // sem papel + ALLOW → ALLOW
        override(userA, TENANT_A, USER_DENY_NO_ROLE, "DENY");  // sem papel + DENY → DENY

        // Tenant B: mesmo nome de permissão via override — isolação
        UUID userB = UUID.randomUUID();
        override(userB, TENANT_B, USER_ALLOW_ONLY, "ALLOW");
    }

    @Test
    void precedenceMatrixMatchesPolicy() {
        var effective = effective(userA, TENANT_A);

        assertTrue(effective.contains("crm:pilot:role_only"),
                "perfil ALLOW + INHERIT → ALLOW");
        assertFalse(effective.contains("crm:pilot:dennied_by_user"),
                "perfil ALLOW + usuário DENY → DENY");
        assertTrue(effective.contains("crm:pilot:user_allow_only"),
                "sem papel + usuário ALLOW → ALLOW");
        assertFalse(effective.contains("crm:pilot:user_deny_no_role"),
                "sem papel + usuário DENY → DENY");
        assertEquals(2, effective.size());
    }

    @Test
    void tenantBDoesNotSeeTenantAOverridesOrRoles() {
        var userB = UUID.randomUUID();
        var effectiveB = effective(userB, TENANT_B);
        // B só tem o próprio override ALLOW; nada de A vaza (roles/overrides de A)
        assertEquals(List.of("crm:pilot:user_allow_only"), effectiveB);
        assertFalse(effectiveB.contains("crm:pilot:role_only"));
    }
}
