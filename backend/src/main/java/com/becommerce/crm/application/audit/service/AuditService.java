package com.becommerce.crm.application.audit.service;

import com.becommerce.crm.application.audit.dto.AuditLogResponse;
import com.becommerce.crm.application.audit.dto.AuditLogSearchRequest;
import com.becommerce.crm.application.audit.port.input.AuditUseCase;
import com.becommerce.crm.application.audit.port.output.AuditLogRepository;
import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.domain.audit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService implements AuditUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponse getById(UUID id, UUID companyId) {
        AuditLog auditLog = auditLogRepository.findById(id);
        if (auditLog == null) {
            throw new AuditLogNotFoundException("Log de auditoria não encontrado com ID: " + id);
        }
        if (!auditLog.getCompanyId().equals(companyId)) {
            throw new AuditLogNotFoundException("Log de auditoria não encontrado com ID: " + id);
        }
        return mapToResponse(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(UUID companyId, AuditLogSearchRequest request) {
        AuditAction action = parseEnum(request.action(), AuditAction.class);
        AuditModule module = parseEnum(request.module(), AuditModule.class);
        AuditStatus status = parseEnum(request.status(), AuditStatus.class);
        UUID userId = request.userId() != null ? UUID.fromString(request.userId()) : null;

        long totalElements = auditLogRepository.countSearch(
            companyId, action, module, status, userId,
            request.entityName(), request.entityId(), request.search(),
            request.startDate(), request.endDate()
        );

        var content = auditLogRepository.search(
            companyId, action, module, status, userId,
            request.entityName(), request.entityId(), request.search(),
            request.startDate(), request.endDate(),
            request.getEffectivePage(), request.getEffectivePageSize()
        ).stream().map(this::mapToResponse).toList();

        return PageResponse.of(content, request.getEffectivePage(), request.getEffectivePageSize(), totalElements);
    }

    public void recordAudit(AuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to record audit log: {}", e.getMessage(), e);
        }
    }

    private AuditLogResponse mapToResponse(AuditLog auditLog) {
        return new AuditLogResponse(
            auditLog.getId().toString(),
            auditLog.getCompanyId().toString(),
            auditLog.getUserId() != null ? auditLog.getUserId().toString() : null,
            auditLog.getUserName(),
            auditLog.getUserEmail(),
            auditLog.getAction() != null ? auditLog.getAction().name() : null,
            auditLog.getModule() != null ? auditLog.getModule().name() : null,
            auditLog.getEntityName(),
            auditLog.getEntityId(),
            auditLog.getDescription(),
            auditLog.getOldValues(),
            auditLog.getNewValues(),
            auditLog.getIpAddress(),
            auditLog.getUserAgent(),
            auditLog.getRequestMethod(),
            auditLog.getRequestUri(),
            auditLog.getStatus() != null ? auditLog.getStatus().name() : null,
            auditLog.isSuccess(),
            auditLog.getCreatedAt()
        );
    }

    private <T extends Enum<T>> T parseEnum(String value, Class<T> enumClass) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
