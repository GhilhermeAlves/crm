package com.becommerce.crm.infrastructure.tenant.datasource;

import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantAwareDataSourceTest {

    @Mock
    private DataSource delegateDataSource;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldSetTenantContextOnGetConnection() throws SQLException {
        UUID companyId = UUID.randomUUID();
        TenantContext.setCompanyId(companyId);

        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(delegateDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);

        Connection conn = new TenantAwareDataSource(delegateDataSource).getConnection();

        verify(statement).execute("SET app.current_company_id = '" + companyId + "'");
        assertNotNull(conn);
    }

    @Test
    void shouldResetTenantContextWhenEmpty() throws SQLException {
        TenantContext.clear();

        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(delegateDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        Connection conn = new TenantAwareDataSource(delegateDataSource).getConnection();

        verify(statement).execute("RESET app.current_company_id");
        assertNotNull(conn);
    }

    @Test
    void shouldUseValidatedUUID() throws SQLException {
        UUID companyId = UUID.randomUUID();
        TenantContext.setCompanyId(companyId);

        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(delegateDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);

        new TenantAwareDataSource(delegateDataSource).getConnection();

        String expectedSql = "SET app.current_company_id = '" + companyId + "'";
        verify(statement).execute(expectedSql);
    }

    @Test
    void shouldReapplyTenantContextWhenCompanyChangesMidTransaction() throws SQLException {
        UUID paulo = UUID.randomUUID();
        TenantContext.setCompanyId(paulo);

        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(delegateDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);

        Connection conn = new TenantAwareDataSource(delegateDataSource).getConnection();

        verify(statement).execute("SET app.current_company_id = '" + paulo + "'");
        clearInvocations(statement);

        UUID novaEmpresa = UUID.randomUUID();
        TenantContext.setCompanyId(novaEmpresa);

        conn.createStatement();

        verify(statement).execute("SET app.current_company_id = '" + novaEmpresa + "'");
    }

    @Test
    void shouldNotReapplyWhenContextUnchanged() throws SQLException {
        UUID paulo = UUID.randomUUID();
        TenantContext.setCompanyId(paulo);

        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(delegateDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);

        Connection conn = new TenantAwareDataSource(delegateDataSource).getConnection();

        verify(statement).execute("SET app.current_company_id = '" + paulo + "'");
        clearInvocations(statement);

        conn.createStatement();

        verify(statement, never()).execute(anyString());
    }

    @Test
    void shouldKeepTenantContextOnFlushWhenClearedInsideTransaction() throws SQLException {
        UUID companyId = UUID.randomUUID();
        TenantContext.setCompanyId(companyId);

        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(delegateDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);

        Connection conn = new TenantAwareDataSource(delegateDataSource).getConnection();

        verify(statement).execute("SET app.current_company_id = '" + companyId + "'");
        clearInvocations(statement);

        conn.setAutoCommit(false);
        TenantContext.clear();
        conn.prepareStatement("INSERT INTO contacts (company_id) VALUES (?)");

        // Dentro da transação, a limpeza do contexto NÃO deve gerar RESET: o
        // flush no commit ainda enxerga app.current_company_id da empresa.
        verify(statement, never()).execute(contains("RESET"));
    }

    @Test
    void shouldResetTenantContextAfterTransactionCommitWhenCleared() throws SQLException {
        UUID companyId = UUID.randomUUID();
        TenantContext.setCompanyId(companyId);

        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(delegateDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);

        Connection conn = new TenantAwareDataSource(delegateDataSource).getConnection();

        verify(statement).execute("SET app.current_company_id = '" + companyId + "'");
        clearInvocations(statement);

        conn.setAutoCommit(false);
        conn.commit();
        TenantContext.clear();
        conn.prepareStatement("SELECT 1");

        // Após o commit, o contexto limpo é aplicado de volta (RESET), evitando
        // vazar a empresa da transação para o próximo uso da conexão.
        verify(statement).execute("RESET app.current_company_id");
    }

    @Test
    void shouldDelegateGetConnectionWithCredentials() throws SQLException {
        UUID companyId = UUID.randomUUID();
        TenantContext.setCompanyId(companyId);

        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(delegateDataSource.getConnection("user", "pass")).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);

        Connection conn = new TenantAwareDataSource(delegateDataSource).getConnection("user", "pass");

        verify(delegateDataSource).getConnection("user", "pass");
        verify(statement).execute("SET app.current_company_id = '" + companyId + "'");
        assertNotNull(conn);
    }
}
