package com.becommerce.crm.sales.chair.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.sales.chair.application.dto.ChairResponse;
import com.becommerce.crm.sales.chair.application.dto.CreateChairRequest;
import com.becommerce.crm.sales.chair.application.dto.UpdateChairRequest;
import com.becommerce.crm.sales.chair.application.port.in.ChairUseCase;
import com.becommerce.crm.sales.chair.application.port.out.ChairRepository;
import com.becommerce.crm.sales.chair.domain.Chair;
import com.becommerce.crm.sales.chair.domain.exception.ChairNotFoundException;
import com.becommerce.crm.sales.chair.domain.exception.ChairValidationException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ChairService implements ChairUseCase {

    private final ChairRepository repository;
    private final TenantAuditRecorder auditor;

    public ChairService(ChairRepository repository, TenantAuditRecorder auditor) {
        this.repository = repository;
        this.auditor = auditor;
    }

    @Override
    @Transactional
    public ChairResponse create(UUID companyId, CreateChairRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            String name = request.name() == null ? null : request.name().trim();
            repository.findByCompanyIdAndName(companyId, name)
                    .ifPresent(existing -> {
                        throw new ChairValidationException("Já existe uma cadeira com o nome '" + name + "'.");
                    });

            Chair chair = Chair.create(companyId, name, request.color(),
                    request.active() != null ? request.active() : true,
                    request.sortOrder() != null ? request.sortOrder() : 0);
            repository.save(chair);

            auditor.record(companyId, AuditAction.CREATE, AuditModule.SCHEDULING, "Chair",
                    chair.getId().toString(), "Cadeira criada: " + chair.getName(), null, null);
            return toResponse(chair);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ChairResponse getById(UUID companyId, UUID chairId) {
        try {
            TenantContext.setCompanyId(companyId);
            return toResponse(requireOwned(companyId, chairId));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public ChairResponse update(UUID companyId, UUID chairId, UpdateChairRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            Chair chair = requireOwned(companyId, chairId);

            if (request.name() != null && !request.name().isBlank()) {
                String name = request.name().trim();
                repository.findByCompanyIdAndName(companyId, name)
                        .filter(existing -> !existing.getId().equals(chairId))
                        .ifPresent(existing -> {
                            throw new ChairValidationException("Já existe uma cadeira com o nome '" + name + "'.");
                        });
            }

            chair.update(request.name(), request.color(), request.active(), request.sortOrder());
            repository.save(chair);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.SCHEDULING, "Chair",
                    chair.getId().toString(), "Cadeira atualizada: " + chair.getName(), null, null);
            return toResponse(chair);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID chairId) {
        try {
            TenantContext.setCompanyId(companyId);
            Chair chair = requireOwned(companyId, chairId);
            repository.delete(chair);

            auditor.record(companyId, AuditAction.DELETE, AuditModule.SCHEDULING, "Chair",
                    chairId.toString(), "Cadeira excluída: " + chair.getName(), null, null);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChairResponse> listByCompany(UUID companyId) {
        try {
            TenantContext.setCompanyId(companyId);
            return repository.findByCompanyId(companyId).stream()
                    .map(ChairService::toResponse)
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInCompany(UUID companyId, UUID chairId) {
        try {
            TenantContext.setCompanyId(companyId);
            if (chairId == null) {
                return false;
            }
            return repository.findById(chairId)
                    .map(chair -> chair.getCompanyId().equals(companyId))
                    .orElse(false);
        } finally {
            TenantContext.clear();
        }
    }

    private Chair requireOwned(UUID companyId, UUID chairId) {
        Chair chair = repository.findById(chairId)
                .orElseThrow(() -> new ChairNotFoundException("Cadeira", chairId));
        if (!chair.getCompanyId().equals(companyId)) {
            throw new ChairNotFoundException("Cadeira", chairId);
        }
        return chair;
    }

    private static ChairResponse toResponse(Chair chair) {
        return new ChairResponse(chair.getId(), chair.getCompanyId(), chair.getName(), chair.getColor(),
                chair.isActive(), chair.getSortOrder(), chair.getCreatedAt(), chair.getUpdatedAt());
    }
}
