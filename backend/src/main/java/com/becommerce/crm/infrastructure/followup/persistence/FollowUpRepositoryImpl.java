package com.becommerce.crm.infrastructure.followup.persistence;

import com.becommerce.crm.application.followup.port.output.FollowUpRepository;
import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.domain.followup.FollowUp;
import com.becommerce.crm.domain.followup.FollowUpAction;
import com.becommerce.crm.domain.followup.FollowUpCancellationReason;
import com.becommerce.crm.domain.followup.FollowUpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FollowUpRepositoryImpl implements FollowUpRepository {

    private final FollowUpJpaRepository jpaRepository;

    public FollowUpRepositoryImpl(FollowUpJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public FollowUp save(FollowUp followUp) {
        return toDomain(jpaRepository.save(toEntity(followUp)));
    }

    @Override
    public Optional<FollowUp> findById(UUID id) {
        return jpaRepository.findById(id).map(FollowUpRepositoryImpl::toDomain);
    }

    @Override
    public Optional<FollowUp> findByIdempotencyKey(UUID companyId, UUID idempotencyKey) {
        return jpaRepository.findByCompanyIdAndIdempotencyKey(companyId, idempotencyKey)
                .map(FollowUpRepositoryImpl::toDomain);
    }

    @Override
    public PageResponse<FollowUp> findByCompany(UUID companyId, UUID conversationId, int page, int pageSize) {
        Page<FollowUpJpaEntity> result = jpaRepository.findByCompany(
                companyId, conversationId, PageRequest.of(page, pageSize));
        return PageResponse.of(result.stream().map(FollowUpRepositoryImpl::toDomain).toList(),
                page, pageSize, result.getTotalElements());
    }

    @Override
    public boolean claim(UUID companyId, UUID followUpId, LocalDateTime now, LocalDateTime staleProcessingCutoff) {
        return jpaRepository.claim(companyId, followUpId, now, staleProcessingCutoff) > 0;
    }

    @Override
    public boolean markSent(UUID companyId, UUID followUpId, String resultText, LocalDateTime now) {
        return jpaRepository.markSent(companyId, followUpId, resultText, now) > 0;
    }

    @Override
    public boolean scheduleRetry(UUID companyId, UUID followUpId, LocalDateTime nextExecuteAt,
                                 String error, LocalDateTime now) {
        return jpaRepository.scheduleRetry(companyId, followUpId, nextExecuteAt, error, now) > 0;
    }

    @Override
    public boolean markFailedTerminal(UUID companyId, UUID followUpId, String error, LocalDateTime now) {
        return jpaRepository.markFailedTerminal(companyId, followUpId, error, now) > 0;
    }

    @Override
    public boolean cancelPending(UUID companyId, UUID followUpId, LocalDateTime now) {
        return jpaRepository.cancelPending(companyId, followUpId, FollowUpCancellationReason.USER.name(), now) > 0;
    }

    @Override
    public boolean cancelProcessingByRule(UUID companyId, UUID followUpId,
                                          FollowUpCancellationReason reason, LocalDateTime now) {
        return jpaRepository.cancelProcessingByRule(companyId, followUpId, reason.name(), now) > 0;
    }

    private static FollowUpJpaEntity toEntity(FollowUp f) {
        FollowUpJpaEntity e = new FollowUpJpaEntity();
        e.setId(f.getId());
        e.setCompanyId(f.getCompanyId());
        e.setConversationId(f.getConversationId());
        if (f.getStatus() != null) { e.setStatus(f.getStatus().name()); }
        if (f.getActionType() != null) { e.setActionType(f.getActionType().name()); }
        e.setActionContent(f.getActionContent());
        e.setExecuteAt(f.getExecuteAt());
        e.setAttempts(f.getAttempts());
        e.setLastError(f.getLastError());
        e.setResultText(f.getResultText());
        e.setProcessingStartedAt(f.getProcessingStartedAt());
        e.setProcessedAt(f.getProcessedAt());
        e.setCancelledAt(f.getCancelledAt());
        if (f.getCancelledReason() != null) { e.setCancelledReason(f.getCancelledReason().name()); }
        e.setIdempotencyKey(f.getIdempotencyKey());
        e.setSequenceId(f.getSequenceId());
        e.setCreatedAt(f.getCreatedAt());
        e.setUpdatedAt(f.getUpdatedAt());
        return e;
    }

    private static FollowUp toDomain(FollowUpJpaEntity e) {
        return FollowUp.reconstitute(e.getId(), e.getCompanyId(), e.getConversationId(),
                e.getStatus() == null ? null : FollowUpStatus.valueOf(e.getStatus()),
                e.getActionType() == null ? null : FollowUpAction.valueOf(e.getActionType()),
                e.getActionContent(), e.getExecuteAt(), e.getAttempts(), e.getLastError(), e.getResultText(),
                e.getProcessingStartedAt(), e.getProcessedAt(), e.getCancelledAt(),
                e.getCancelledReason() == null ? null : FollowUpCancellationReason.valueOf(e.getCancelledReason()),
                e.getIdempotencyKey(), e.getSequenceId(), e.getCreatedAt(), e.getUpdatedAt());
    }
}