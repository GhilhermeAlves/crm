package com.becommerce.crm.masterdata.person;

import com.becommerce.crm.shared.tenant.context.TenantContext;
import com.becommerce.crm.shared.tenant.datasource.TenantAwareDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Migrations REAIS V147/V148/V149 num PostgreSQL 17: preenchimento a partir de contacts (mesmo
 * UUID), CPF repetido/inválido virando pendência, gatilho de sincronização (cliente/paciente,
 * contatos, endereço com cidade do IBGE) e isolamento por RLS com usuário não-superuser.
 */
@Testcontainers
class PersonSyncIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("crm_it_persons")
            .withUsername("crm_superuser")
            .withPassword("crm_it_pass");

    static final String APP_USER = "crm_persons_app_user";
    static final String APP_PASSWORD = "crm_persons_app_pass";

    static final UUID TENANT_A = UUID.fromString("71111111-2222-3333-4444-555555555551");
    static final UUID TENANT_B = UUID.fromString("71111111-2222-3333-4444-555555555552");
    static final UUID OLD_CPF = UUID.fromString("a1000000-0000-0000-0000-000000000001");
    static final UUID DUP_CPF = UUID.fromString("a1000000-0000-0000-0000-000000000002");
    static final UUID BAD_CPF = UUID.fromString("a1000000-0000-0000-0000-000000000003");
    static final UUID OTHER_TENANT = UUID.fromString("b1000000-0000-0000-0000-000000000001");

    static HikariDataSource rawPool;
    static TenantAwareDataSource tenantAware;

    @BeforeAll
    static void setup() throws Exception {
        try (Connection conn = postgres.createConnection("")) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("agent-rls-bootstrap.sql"));
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE ROLE crm_app NOLOGIN NOBYPASSRLS");
                st.execute("ALTER TABLE companies ADD COLUMN enabled_modules VARCHAR(500) NOT NULL DEFAULT 'CRM,PATIENTS'");
                st.execute("CREATE TABLE users (id UUID PRIMARY KEY, first_name VARCHAR(100), last_name VARCHAR(100), email VARCHAR(255))");
                st.execute("CREATE TABLE storage_objects (id UUID PRIMARY KEY)");
                st.execute("CREATE TABLE company_settings (company_id UUID PRIMARY KEY, require_contact_cpf BOOLEAN NOT NULL DEFAULT FALSE)");
                st.execute("CREATE TABLE permissions (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), name VARCHAR(100) UNIQUE, "
                        + "description TEXT, module VARCHAR(50), resource VARCHAR(50), action VARCHAR(50))");
                st.execute("CREATE TABLE roles (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), name VARCHAR(50), company_id UUID)");
                st.execute("CREATE TABLE role_permissions (role_id UUID, permission_id UUID, PRIMARY KEY (role_id, permission_id))");
                // contacts com as colunas que o gatilho lê (V015/V080/V081/V097/V104), com RLS FORCE como em produção.
                st.execute("""
                        CREATE TABLE contacts (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
                            first_name VARCHAR(100) NOT NULL, last_name VARCHAR(100), email VARCHAR(255),
                            phone VARCHAR(20), mobile VARCHAR(20), notes TEXT, avatar_url TEXT,
                            birth_date DATE, cpf VARCHAR(14), rg VARCHAR(20), rg_issuer VARCHAR(20),
                            gender VARCHAR(20), marital_status VARCHAR(30), professional_status VARCHAR(20),
                            zip_code VARCHAR(10), street VARCHAR(160), address_number VARCHAR(20),
                            address_complement VARCHAR(80), neighborhood VARCHAR(80), city VARCHAR(80), state VARCHAR(2),
                            health_plan VARCHAR(80), health_plan_number VARCHAR(40), record_number VARCHAR(40),
                            referral_source VARCHAR(80), is_patient BOOLEAN NOT NULL DEFAULT FALSE, patient_since DATE,
                            created_at TIMESTAMP NOT NULL DEFAULT now(), updated_at TIMESTAMP NOT NULL DEFAULT now(),
                            deleted_at TIMESTAMP)""");
                st.execute("ALTER TABLE contacts ENABLE ROW LEVEL SECURITY");
                st.execute("ALTER TABLE contacts FORCE ROW LEVEL SECURITY");
                st.execute("CREATE POLICY tenant_isolation_policy ON contacts USING (company_id = app.current_tenant_id()) "
                        + "WITH CHECK (company_id = app.current_tenant_id())");
                st.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON contacts TO crm_app");
            }
            insertCompany(conn, TENANT_A, "Pessoas A LTDA", "71.111.111/0001-11");
            insertCompany(conn, TENANT_B, "Pessoas B LTDA", "72.222.222/0002-22");
            try (Statement st = conn.createStatement()) {
                // Dados "antigos" (antes da V147): o mais antigo fica com o CPF.
                st.execute("INSERT INTO contacts (id, company_id, first_name, last_name, cpf, phone, email, is_patient, "
                        + "record_number, zip_code, street, address_number, neighborhood, city, state, created_at) VALUES "
                        + "('" + OLD_CPF + "', '" + TENANT_A + "', 'Paulo', 'Alves', '529.982.247-25', '(34) 99999-0000', "
                        + "'paulo@a.com', TRUE, 'P-1', '38400-000', 'Rua A', '10', 'Centro', 'Uberlândia', 'MG', '2026-01-01'), "
                        + "('" + DUP_CPF + "', '" + TENANT_A + "', 'Kely', 'Alves', '52998224725', NULL, NULL, TRUE, NULL, "
                        + "NULL, NULL, NULL, NULL, NULL, NULL, '2026-02-01'), "
                        + "('" + BAD_CPF + "', '" + TENANT_A + "', 'Sem', 'Documento', '123', NULL, NULL, FALSE, NULL, "
                        + "NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-01'), "
                        + "('" + OTHER_TENANT + "', '" + TENANT_B + "', 'Bruno', NULL, '52998224725', NULL, NULL, FALSE, NULL, "
                        + "NULL, NULL, NULL, NULL, NULL, NULL, '2026-01-01')");
            }
            for (String m : new String[] {"V147__persons_core.sql", "V148__locations.sql", "V149__person_field_settings.sql"}) {
                try (Statement st = conn.createStatement()) {
                    st.execute(new ClassPathResource("db/migration/" + m).getContentAsString(StandardCharsets.UTF_8));
                }
            }
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE ROLE " + APP_USER + " LOGIN PASSWORD '" + APP_PASSWORD + "' NOSUPERUSER NOBYPASSRLS");
                st.execute("GRANT crm_app TO " + APP_USER);
                st.execute("GRANT USAGE ON SCHEMA public TO " + APP_USER);
                st.execute("GRANT USAGE ON SCHEMA app TO " + APP_USER);
                st.execute("GRANT EXECUTE ON FUNCTION app.current_tenant_id() TO " + APP_USER);
                st.execute("GRANT SELECT ON companies TO " + APP_USER);
            }
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(APP_USER);
        config.setPassword(APP_PASSWORD);
        config.setMaximumPoolSize(2);
        rawPool = new HikariDataSource(config);
        tenantAware = new TenantAwareDataSource(rawPool);
    }

    @AfterAll
    static void tearDown() {
        if (rawPool != null) rawPool.close();
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void backfillUsaOMesmoIdEOMaisAntigoFicaComOCpf() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        try (Connection c = tenantAware.getConnection()) {
            assertEquals("52998224725", one(c, "SELECT tax_id FROM persons WHERE id = '" + OLD_CPF + "'"));
            assertEquals("Paulo Alves", one(c, "SELECT name FROM persons WHERE id = '" + OLD_CPF + "'"));
            assertNull(one(c, "SELECT tax_id FROM persons WHERE id = '" + DUP_CPF + "'"));
            assertEquals("DUPLICATE_OTHER_PERSON", one(c, "SELECT issue_type FROM person_migration_issues WHERE person_id = '" + DUP_CPF + "'"));
            assertEquals(OLD_CPF.toString(), one(c, "SELECT kept_person_id::text FROM person_migration_issues WHERE person_id = '" + DUP_CPF + "'"));
            assertEquals("INVALID_TAX_ID", one(c, "SELECT issue_type FROM person_migration_issues WHERE person_id = '" + BAD_CPF + "'"));
            assertEquals("t", one(c, "SELECT provisional::text FROM persons WHERE id = '" + BAD_CPF + "'").substring(0, 1));
        }
    }

    @Test
    void pacienteViraClienteEPacienteComContatosEEnderecoDoIbge() throws SQLException {
        TenantContext.setCompanyId(TENANT_A);
        try (Connection c = tenantAware.getConnection()) {
            assertEquals("1", one(c, "SELECT customer_code FROM customers WHERE person_id = '" + OLD_CPF + "'"));
            assertEquals("P-1", one(c, "SELECT record_number FROM patients WHERE person_id = '" + OLD_CPF + "'"));
            assertEquals("34999990000", one(c, "SELECT value FROM person_contacts WHERE person_id = '" + OLD_CPF
                    + "' AND sync_slot = 'CONTACT_PHONE'"));
            assertEquals("paulo@a.com", one(c, "SELECT value FROM person_contacts WHERE person_id = '" + OLD_CPF
                    + "' AND contact_type = 'EMAIL'"));
            assertEquals("38400000", one(c, "SELECT postal_code FROM person_addresses WHERE person_id = '" + OLD_CPF + "'"));
            assertEquals("3170206", one(c, "SELECT city_id::text FROM person_addresses WHERE person_id = '" + OLD_CPF + "'"));
            assertNotNull(one(c, "SELECT neighborhood_id::text FROM person_addresses WHERE person_id = '" + OLD_CPF + "'"));
        }
    }

    @Test
    void gatilhoSincronizaInclusaoEAlteracaoDeContato() throws SQLException {
        UUID novo = UUID.randomUUID();
        TenantContext.setCompanyId(TENANT_A);
        try (Connection c = tenantAware.getConnection(); Statement st = c.createStatement()) {
            st.execute("INSERT INTO contacts (id, company_id, first_name, last_name, phone) VALUES ('" + novo + "', '"
                    + TENANT_A + "', 'Ana', 'Lima', '34988887777')");
            assertEquals("Ana Lima", one(c, "SELECT name FROM persons WHERE id = '" + novo + "'"));
            st.execute("UPDATE contacts SET last_name = 'Lima Souza', is_patient = TRUE, record_number = 'P-9' WHERE id = '" + novo + "'");
            assertEquals("Ana Lima Souza", one(c, "SELECT name FROM persons WHERE id = '" + novo + "'"));
            assertEquals("P-9", one(c, "SELECT record_number FROM patients WHERE person_id = '" + novo + "'"));
            // Paulo = 1, Kely = 2 (pacientes da migração), novo paciente = 3.
            assertEquals("3", one(c, "SELECT customer_code FROM customers WHERE person_id = '" + novo + "'"));
            st.execute("UPDATE contacts SET deleted_at = now() WHERE id = '" + novo + "'");
            assertEquals("t", one(c, "SELECT deleted::text FROM persons WHERE id = '" + novo + "'").substring(0, 1));
        }
    }

    @Test
    void outraEmpresaNaoEnxergaPessoasEPodeTerOMesmoCpf() throws SQLException {
        TenantContext.setCompanyId(TENANT_B);
        try (Connection c = tenantAware.getConnection()) {
            assertEquals("1", one(c, "SELECT count(*)::text FROM persons"));
            assertEquals("52998224725", one(c, "SELECT tax_id FROM persons WHERE id = '" + OTHER_TENANT + "'"));
            assertNull(one(c, "SELECT name FROM persons WHERE id = '" + OLD_CPF + "'"));
        }
    }

    @Test
    void validacaoDeDocumentoNoBanco() throws SQLException {
        try (Connection c = postgres.createConnection("")) {
            assertEquals("t", one(c, "SELECT app.is_valid_cnpj('12ABC34501DE35')::text").substring(0, 1));
            assertEquals("f", one(c, "SELECT app.is_valid_cnpj('12ABC34501DE36')::text").substring(0, 1));
            assertEquals("t", one(c, "SELECT app.is_valid_cpf('52998224725')::text").substring(0, 1));
            assertEquals("5571", one(c, "SELECT count(*)::text FROM cities"));
            assertEquals("PF", one(c, "SELECT min(context) FROM person_field_contexts fc JOIN person_fields f "
                    + "ON f.id = fc.field_id WHERE f.field_key = 'PERSON_NAME'"));
        }
    }

    private static String one(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private static void insertCompany(Connection conn, UUID id, String name, String cnpj) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO companies (id, legal_name, trading_name, cnpj, email, phone,
                    address_zip_code, address_street, address_number, address_neighborhood,
                    address_city, address_state, address_country, plan, status, max_users, max_storage_mb)
                VALUES (?, ?, ?, ?, ?, '0000-0000', '00000-000', 'Rua Teste', '0', 'Centro',
                    'Sao Paulo', 'SP', 'Brasil', 'STARTER', 'ACTIVE', 10, 1024)
                """)) {
            ps.setObject(1, id);
            ps.setString(2, name);
            ps.setString(3, name);
            ps.setString(4, cnpj);
            ps.setString(5, id + "@it.local");
            ps.executeUpdate();
        }
    }
}
