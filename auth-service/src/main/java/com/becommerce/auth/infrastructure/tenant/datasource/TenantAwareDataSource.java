package com.becommerce.auth.infrastructure.tenant.datasource;

import com.becommerce.auth.infrastructure.tenant.context.TenantContext;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.slf4j.LoggerFactory;

/**
 * Wrapper do datasource que aplica o contexto de tenant/identidade em cada
 * conexão obtida. Sem isso, com RLS FORCE e o usuário {@code crm_app}
 * (NOBYPASSRLS), o auth-service não enxergaria nem a própria linha em
 * {@code users} durante a resolução de identidade.
 */
public class TenantAwareDataSource implements DataSource {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(TenantAwareDataSource.class);

    private final DataSource delegate;

    public TenantAwareDataSource(DataSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = delegate.getConnection();
        setTenantContext(connection);
        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = delegate.getConnection(username, password);
        setTenantContext(connection);
        return connection;
    }

    private void setTenantContext(Connection connection) throws SQLException {
        UUID companyId = TenantContext.getCompanyId();
        String keycloakSub = TenantContext.getKeycloakSub();
        try (var stmt = connection.createStatement()) {
            if (keycloakSub != null && !keycloakSub.isBlank()) {
                String safeSub = keycloakSub.replace("'", "''");
                stmt.execute("SET app.current_keycloak_sub = '" + safeSub + "'");
            } else {
                stmt.execute("RESET app.current_keycloak_sub");
            }
            if (companyId != null) {
                String safeId = companyId.toString();
                stmt.execute("SET app.current_company_id = '" + safeId + "'");
            } else {
                stmt.execute("RESET app.current_company_id");
            }
        }
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) || delegate.isWrapperFor(iface);
    }
}
