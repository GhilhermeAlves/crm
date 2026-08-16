package com.becommerce.crm.infrastructure.tenant.datasource;

import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.slf4j.LoggerFactory;

public class TenantAwareDataSource implements DataSource {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(TenantAwareDataSource.class);

    private final DataSource delegate;

    public TenantAwareDataSource(DataSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = delegate.getConnection();
        TenantSnapshot initial = new TenantSnapshot();
        applyTenantContext(connection, initial);
        return wrap(connection, initial);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = delegate.getConnection(username, password);
        TenantSnapshot initial = new TenantSnapshot();
        applyTenantContext(connection, initial);
        return wrap(connection, initial);
    }

    /**
     * Envolve a conexão em um proxy que reaplica o contexto de tenant (GUCs)
     * antes de cada statement. Assim, mudanças no {@link TenantContext} no meio
     * de uma transação (ex.: provisionar uma empresa nova em createCompany)
     * são refletidas na conexão reutilizada, sem violar as policies RLS (V019).
     *
     * <p>Os GUCs só são reemitidos quando o contexto muda, evitando overhead e
     * spam de log em operações de leitura frequentes.
     */
    private Connection wrap(Connection target, TenantSnapshot initial) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new TenantAwareConnectionHandler(target, initial));
    }

    private static class TenantSnapshot {
        final UUID companyId;
        final String keycloakSub;
        final String identityEmail;
        final String identityPhone;
        final String resetToken;

        TenantSnapshot() {
            this.companyId = TenantContext.getCompanyId();
            this.keycloakSub = TenantContext.getKeycloakSub();
            this.identityEmail = TenantContext.getIdentityEmail();
            this.identityPhone = TenantContext.getIdentityPhone();
            this.resetToken = TenantContext.getResetToken();
        }

        boolean sameAs(TenantSnapshot other) {
            return other != null
                    && Objects.equals(this.companyId, other.companyId)
                    && Objects.equals(this.keycloakSub, other.keycloakSub)
                    && Objects.equals(this.identityEmail, other.identityEmail)
                    && Objects.equals(this.identityPhone, other.identityPhone)
                    && Objects.equals(this.resetToken, other.resetToken);
        }
    }

    private class TenantAwareConnectionHandler implements InvocationHandler {
        private final Connection target;
        private TenantSnapshot applied;

        TenantAwareConnectionHandler(Connection target, TenantSnapshot applied) {
            this.target = target;
            this.applied = applied;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (isStatementFactory(name)) {
                ensureContextApplied();
            }
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        private void ensureContextApplied() throws SQLException {
            TenantSnapshot current = new TenantSnapshot();
            if (!current.sameAs(applied)) {
                applyTenantContext(target, current);
                applied = current;
            }
        }

        private boolean isStatementFactory(String name) {
            return "createStatement".equals(name)
                    || "prepareStatement".equals(name)
                    || "prepareCall".equals(name);
        }
    }

    /**
     * Aplica os GUCs de contexto de tenant na conexão, refletindo o snapshot atual.
     */
    private void applyTenantContext(Connection connection, TenantSnapshot ctx) throws SQLException {
        try (var stmt = connection.createStatement()) {
            if (ctx.keycloakSub != null && !ctx.keycloakSub.isBlank()) {
                String safeSub = ctx.keycloakSub.replace("'", "''");
                stmt.execute("SET app.current_keycloak_sub = '" + safeSub + "'");
            } else {
                stmt.execute("RESET app.current_keycloak_sub");
            }
            if (ctx.identityEmail != null && !ctx.identityEmail.isBlank()) {
                String safeEmail = ctx.identityEmail.replace("'", "''");
                stmt.execute("SET app.current_identity_email = '" + safeEmail + "'");
            } else {
                stmt.execute("RESET app.current_identity_email");
            }
            if (ctx.identityPhone != null && !ctx.identityPhone.isBlank()) {
                String safePhone = ctx.identityPhone.replace("'", "''");
                stmt.execute("SET app.current_identity_phone = '" + safePhone + "'");
            } else {
                stmt.execute("RESET app.current_identity_phone");
            }
            if (ctx.resetToken != null && !ctx.resetToken.isBlank()) {
                String safeToken = ctx.resetToken.replace("'", "''");
                stmt.execute("SET app.current_reset_token = '" + safeToken + "'");
            } else {
                stmt.execute("RESET app.current_reset_token");
            }
            if (ctx.companyId != null) {
                String safeId = ctx.companyId.toString();
                stmt.execute("SET app.current_company_id = '" + safeId + "'");
                log.info("[TENANT] conn={} SET app.current_company_id = {}", System.identityHashCode(connection), safeId);
            } else {
                stmt.execute("RESET app.current_company_id");
                log.info("[TENANT] conn={} RESET app.current_company_id (no tenant context)", System.identityHashCode(connection));
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