package com.becommerce.crm.clinical.anamnesis.formrequest;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Propaga o hash do token do formulário para a policy RLS de acesso por token
 * (V115), via {@code app.set_form_token_context} (GUC local à transação).
 * Análogo ao {@code RegistrationTokenContextHolder}.
 */
@Component
public class FormTokenContextHolder {

    private final JdbcTemplate jdbcTemplate;

    public FormTokenContextHolder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setTokenHash(String tokenHash) {
        jdbcTemplate.query("SELECT app.set_form_token_context(?)", rs -> null, tokenHash);
    }

    public void clear() {
        jdbcTemplate.execute("SELECT app.set_form_token_context(NULL)");
    }
}
