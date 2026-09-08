package com.becommerce.crm.domain.followup;

/**
 * Estado de uma {@link FollowUpSequence} (Sprint 22). Sequências INACTIVE não
 * devem ser usadas para novos agendamentos.
 */
public enum FollowUpSequenceStatus {
    ACTIVE,
    INACTIVE
}
