package com.becommerce.auth.infrastructure.tenant.config;

import com.becommerce.auth.infrastructure.tenant.datasource.TenantAwareDataSource;
import javax.sql.DataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * Embrulha o datasource do auth-service no {@link TenantAwareDataSource} para
 * que o contexto de tenant/identidade (RLS FORCE) seja aplicado a cada conexão.
 */
@Component
public class TenantDataSourcePostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource dataSource && !(bean instanceof TenantAwareDataSource)) {
            return new TenantAwareDataSource(dataSource);
        }
        return bean;
    }
}
