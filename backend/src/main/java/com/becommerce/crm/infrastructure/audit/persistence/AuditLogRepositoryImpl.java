package com.becommerce.crm.infrastructure.audit.persistence;

import com.becommerce.crm.application.audit.port.output.AuditLogRepository;
import com.becommerce.crm.domain.audit.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final SpringDataAuditLogRepository repository;

    public AuditLogRepositoryImpl(SpringDataAuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogJpaEntity entity = toJpaEntity(auditLog);
        AuditLogJpaEntity saved = repository.save(entity);
        return toDomainEntity(saved);
    }

    @Override
    public List<AuditLog> search(UUID companyId, AuditAction action, AuditModule module,
                                  AuditStatus status, UUID userId, String entityName,
                                  String entityId, String search,
                                  LocalDateTime startDate, LocalDateTime endDate,
                                  int page, int pageSize) {
        return repository.search(
            companyId,
            action != null ? action.name() : null,
            module != null ? module.name() : null,
            status != null ? status.name() : null,
            userId, entityName, entityId, search,
            startDate, endDate,
            PageRequest.of(page - 1, pageSize)
        ).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public long countSearch(UUID companyId, AuditAction action, AuditModule module,
                             AuditStatus status, UUID userId, String entityName,
                             String entityId, String search,
                             LocalDateTime startDate, LocalDateTime endDate) {
        return repository.countSearch(
            companyId,
            action != null ? action.name() : null,
            module != null ? module.name() : null,
            status != null ? status.name() : null,
            userId, entityName, entityId, search,
            startDate, endDate
        );
    }

    @Override
    public AuditLog findById(UUID id) {
        return repository.findById(id).map(this::toDomainEntity).orElse(null);
    }

    @Override
    public long countByCompanyId(UUID companyId) {
        return repository.countByCompanyId(companyId);
    }

    @Override
    public long countByCompanyIdAndCreatedAtAfter(UUID companyId, LocalDateTime since) {
        return repository.countByCompanyIdAndCreatedAtAfter(companyId, since);
    }

    @Override
    public List<AuditLog> findRecentByCompanyId(UUID companyId, int limit) {
        return repository.findByCompanyIdOrderByCreatedAtDesc(companyId, PageRequest.of(0, limit))
            .stream()
            .map(this::toDomainEntity)
            .toList();
    }

    private AuditLogJpaEntity toJpaEntity(AuditLog auditLog) {
        AuditLogJpaEntity entity = new AuditLogJpaEntity();
        entity.setId(auditLog.getId());
        entity.setCompanyId(auditLog.getCompanyId());
        entity.setUserId(auditLog.getUserId());
        entity.setUserName(auditLog.getUserName());
        entity.setUserEmail(auditLog.getUserEmail());
        entity.setAction(auditLog.getAction() != null ? auditLog.getAction().name() : null);
        entity.setModule(auditLog.getModule() != null ? auditLog.getModule().name() : null);
        entity.setEntityName(auditLog.getEntityName());
        entity.setEntityId(auditLog.getEntityId());
        entity.setDescription(auditLog.getDescription());
        entity.setOldValues(auditLog.getOldValues());
        entity.setNewValues(auditLog.getNewValues());
        entity.setIpAddress(auditLog.getIpAddress());
        entity.setUserAgent(auditLog.getUserAgent());
        entity.setRequestMethod(auditLog.getRequestMethod());
        entity.setRequestUri(auditLog.getRequestUri());
        entity.setStatus(auditLog.getStatus() != null ? auditLog.getStatus().name() : "SUCCESS");
        entity.setSuccess(auditLog.isSuccess());
        entity.setCreatedAt(auditLog.getCreatedAt());
        return entity;
    }

    private AuditLog toDomainEntity(AuditLogJpaEntity entity) {
        AuditLog auditLog = AuditLog.create(entity.getCompanyId(),
            AuditAction.valueOf(entity.getAction()),
            AuditModule.valueOf(entity.getModule()));
        auditLog.setId(entity.getId());
        auditLog.setUserId(entity.getUserId());
        auditLog.setUserName(entity.getUserName());
        auditLog.setUserEmail(entity.getUserEmail());
        auditLog.setEntityName(entity.getEntityName());
        auditLog.setEntityId(entity.getEntityId());
        auditLog.setDescription(entity.getDescription());
        auditLog.setOldValues(entity.getOldValues());
        auditLog.setNewValues(entity.getNewValues());
        auditLog.setIpAddress(entity.getIpAddress());
        auditLog.setUserAgent(entity.getUserAgent());
        auditLog.setRequestMethod(entity.getRequestMethod());
        auditLog.setRequestUri(entity.getRequestUri());
        auditLog.setStatus(AuditStatus.valueOf(entity.getStatus()));
        auditLog.setSuccess(entity.isSuccess());
        auditLog.setCreatedAt(entity.getCreatedAt());
        return auditLog;
    }
}
