package com.becommerce.crm.sales.recall.infrastructure.persistence;

import com.becommerce.crm.sales.recall.application.port.out.RecallRepository;
import com.becommerce.crm.sales.recall.domain.Recall;
import com.becommerce.crm.sales.recall.domain.RecallStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RecallRepositoryImpl implements RecallRepository {

    private final RecallJpaRepository jpaRepository;

    public RecallRepositoryImpl(RecallJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Recall save(Recall recall) {
        return toDomain(jpaRepository.save(toEntity(recall)));
    }

    @Override
    public Optional<Recall> findById(UUID id) {
        return jpaRepository.findById(id).map(RecallRepositoryImpl::toDomain);
    }

    @Override
    public PageResult findByCompany(UUID companyId, UUID hostId, String tag, String q,
                                    LocalDate dueFromInclusive, LocalDate dueToExclusive,
                                    int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "dueDate"));
        Page<RecallJpaEntity> result = jpaRepository.findByCompany(
                companyId, hostId, tag, q, dueFromInclusive, dueToExclusive, pageRequest);
        List<Recall> recalls = result.getContent().stream().map(RecallRepositoryImpl::toDomain).toList();
        return new PageResult(recalls, result.getTotalElements());
    }

    @Override
    public List<SuggestionCandidate> findSuggestionCandidates(UUID companyId, Instant cutoff, Instant now) {
        return jpaRepository.findSuggestionCandidates(companyId, cutoff, now).stream()
                .map(RecallRepositoryImpl::toCandidate)
                .toList();
    }

    private static SuggestionCandidate toCandidate(RecallSuggestionRow row) {
        List<String> procedures = row.getRecentProcedures() == null || row.getRecentProcedures().isBlank()
                ? List.of()
                : Arrays.asList(row.getRecentProcedures().split("\n"));
        return new SuggestionCandidate(row.getContactId(), row.getFirstName(), row.getLastName(),
                row.getMobile(), row.getPhone(), row.getHostId(), row.getLastAppointmentAt(), procedures,
                Boolean.TRUE.equals(row.getHasFutureAppointment()),
                Boolean.TRUE.equals(row.getHasPendingRecall()),
                Boolean.TRUE.equals(row.getDismissed()));
    }

    private static RecallJpaEntity toEntity(Recall recall) {
        RecallJpaEntity entity = new RecallJpaEntity();
        entity.setId(recall.getId());
        entity.setCompanyId(recall.getCompanyId());
        entity.setContactId(recall.getContactId());
        entity.setPhone(recall.getPhone());
        entity.setHostId(recall.getHostId());
        entity.setTag(recall.getTag());
        entity.setDueDate(recall.getDueDate());
        entity.setStatus(recall.getStatus().name());
        entity.setAppointmentId(recall.getAppointmentId());
        entity.setCreatedBy(recall.getCreatedBy());
        entity.setCreatedAt(recall.getCreatedAt());
        entity.setUpdatedAt(recall.getUpdatedAt());
        return entity;
    }

    private static Recall toDomain(RecallJpaEntity entity) {
        return Recall.reconstitute(entity.getId(), entity.getCompanyId(), entity.getContactId(),
                entity.getPhone(), entity.getHostId(), entity.getTag(), entity.getDueDate(),
                RecallStatus.valueOf(entity.getStatus()), entity.getAppointmentId(), entity.getCreatedBy(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
