package com.becommerce.crm.domain.followup;

/**
 * Ciclo de vida de um FollowUp (Sprint 22).
 *
 * <p>Transições permitidas (máquina de estados do domínio):
 * <pre>
 *   PENDING   -> PROCESSING (claim atômico do worker)
 *   PROCESSING -> SENT      (ação executada com sucesso)
 *   PROCESSING -> PENDING   (retry seguro após falha transitória)
 *   PROCESSING -> FAILED    (falha terminal após limite de tentativas)
 *   PENDING   -> CANCELLED  (cancelamento pelo usuário)
 *   PROCESSING -> CANCELLED (regra do domínio: modo HUMAN ou follow-up obsoleto)
 * </pre>
 * As transições de {@code PROCESSING} para estados finais são feitas de forma
 * atômica no repositório (guard de status) — ver {@code FollowUpRepository}.
 */
public enum FollowUpStatus {
    PENDING,
    PROCESSING,
    SENT,
    CANCELLED,
    FAILED
}