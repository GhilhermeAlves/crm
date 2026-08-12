package com.becommerce.crm.domain.invitation;

/**
 * Status do convite (Sprint 8.5). Recusa é mapeada para {@link #REVOKED}.
 * Não existe DECLINED — decisão documentada em sprints/8.5/REPORT.md.
 */
public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    REVOKED,
    EXPIRED
}