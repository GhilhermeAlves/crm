package com.becommerce.crm.application.followup.port.output;

import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.domain.followup.FollowUp;
import com.becommerce.crm.domain.followup.FollowUpCancellationReason;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de persistência de FollowUp (Sprint 22). Os métodos que representam
 * partidas de corrida (claim/transições) são executados no banco de forma
 * atômica e respondem {@code false}/{@code 0} quando o guard de status
 * impede a operação — permitindo idempotência garantida entre instâncias.
 */
public interface FollowUpRepository {

    FollowUp save(FollowUp followUp);

    Optional<FollowUp> findById(UUID id);

    Optional<FollowUp> findByIdempotencyKey(UUID companyId, UUID idempotencyKey);

    PageResponse<FollowUp> findByCompany(UUID companyId, UUID conversationId, int page, int pageSize);

    /**
     * PENDING -> PROCESSING (ou recupera PROCESSING órfão mais antigo que o
     * cutoff em caso de crash). Retorna {@code false} se outro worker venceu.
     */
    boolean claim(UUID companyId, UUID followUpId, LocalDateTime now, LocalDateTime staleProcessingCutoff);

    boolean markSent(UUID companyId, UUID followUpId, String resultText, LocalDateTime now);

    /** PROCESSING -> PENDING reagendado com backoff (tentativa segura). */
    boolean scheduleRetry(UUID companyId, UUID followUpId, LocalDateTime nextExecuteAt, String error, LocalDateTime now);

    boolean markFailedTerminal(UUID companyId, UUID followUpId, String error, LocalDateTime now);

    /** Cancelamento do usuário: apenas PENDING. */
    boolean cancelPending(UUID companyId, UUID followUpId, LocalDateTime now);

    /** Cancelamento por regra do processador (modo HUMAN ou follow-up obsoleto). */
    boolean cancelProcessingByRule(UUID companyId, UUID followUpId, FollowUpCancellationReason reason, LocalDateTime now);
}