package com.becommerce.crm.masterdata.contact.registration.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Propaga o hash do token do link de cadastro para as policies RLS de acesso por
 * token (V108), via {@code app.set_registration_token_context} (GUC local à
 * transação). Análogo ao {@code InvitationTokenContextHolder}; o hash é hex de
 * 64 chars gerado pelo próprio serviço, mas vai como parâmetro mesmo assim.
 */
@Component
public class RegistrationTokenContextHolder {

    private final JdbcTemplate jdbcTemplate;

    public RegistrationTokenContextHolder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setTokenHash(String tokenHash) {
        jdbcTemplate.query("SELECT app.set_registration_token_context(?)", rs -> null,
                tokenHash);
    }

    public void clear() {
        jdbcTemplate.execute("SELECT app.set_registration_token_context(NULL)");
    }
}
