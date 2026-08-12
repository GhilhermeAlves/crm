package com.becommerce.crm.infrastructure.invitation.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Propaga o token de convite da sessão para as policies RLS de acesso por token
 * (V036). Análogo ao {@code JdbcTenantLinkHolder}, mas define
 * {@code app.invitation_token_hash} via {@code app.set_invitation_token_context}.
 */
@Component
public class InvitationTokenContextHolder {

    private final JdbcTemplate jdbcTemplate;

    public InvitationTokenContextHolder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setTokenHash(String tokenHash) {
        jdbcTemplate.execute("SELECT app.set_invitation_token_context('" + tokenHash + "')");
    }

    public void clear() {
        jdbcTemplate.execute("SELECT app.set_invitation_token_context(NULL)");
    }
}