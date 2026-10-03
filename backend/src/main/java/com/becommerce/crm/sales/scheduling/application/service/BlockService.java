package com.becommerce.crm.sales.scheduling.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.sales.scheduling.application.dto.BlockResponse;
import com.becommerce.crm.sales.scheduling.application.dto.CreateBlockRequest;
import com.becommerce.crm.sales.scheduling.application.port.in.BlockUseCase;
import com.becommerce.crm.sales.scheduling.application.port.out.ScheduleBlockRepository;
import com.becommerce.crm.sales.scheduling.domain.BlockSource;
import com.becommerce.crm.sales.scheduling.domain.ScheduleBlock;
import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingNotFoundException;
import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingValidationException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BlockService implements BlockUseCase {

    private final ScheduleBlockRepository repository;
    private final TenantAuditRecorder auditor;

    public BlockService(ScheduleBlockRepository repository, TenantAuditRecorder auditor) {
        this.repository = repository;
        this.auditor = auditor;
    }

    @Override
    @Transactional
    public BlockResponse create(UUID companyId, CreateBlockRequest request, UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            ScheduleBlock block = ScheduleBlock.create(companyId, request.hostId(),
                    request.startAt(), request.endAt(), request.reason(), createdBy);
            repository.save(block);

            auditor.record(companyId, AuditAction.CREATE, AuditModule.SCHEDULING, "ScheduleBlock",
                    block.getId().toString(), "Bloqueio criado", createdBy, null);
            return toResponse(block);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BlockResponse getById(UUID companyId, UUID blockId) {
        try {
            TenantContext.setCompanyId(companyId);
            return toResponse(requireOwned(companyId, blockId));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID blockId) {
        try {
            TenantContext.setCompanyId(companyId);
            ScheduleBlock block = requireOwned(companyId, blockId);
            if (block.getSource() == BlockSource.GOOGLE) {
                throw new SchedulingValidationException("Bloqueios do Google Calendar não podem ser excluídos manualmente.");
            }
            repository.delete(block);

            auditor.record(companyId, AuditAction.DELETE, AuditModule.SCHEDULING, "ScheduleBlock",
                    blockId.toString(), "Bloqueio excluído", null, null);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockResponse> list(UUID companyId, Instant from, Instant to, List<UUID> hostIds) {
        try {
            TenantContext.setCompanyId(companyId);
            return blockRepository(companyId, from, to, hostIds).stream()
                    .map(BlockService::toResponse).toList();
        } finally {
            TenantContext.clear();
        }
    }

    private List<ScheduleBlock> blockRepository(UUID companyId, Instant from, Instant to, List<UUID> hostIds) {
        return repository.findByCompanyIdAndRange(companyId, from, to, hostIds);
    }

    private ScheduleBlock requireOwned(UUID companyId, UUID blockId) {
        ScheduleBlock block = repository.findById(blockId)
                .orElseThrow(() -> new SchedulingNotFoundException("Bloqueio", blockId));
        if (!block.getCompanyId().equals(companyId)) {
            throw new SchedulingNotFoundException("Bloqueio", blockId);
        }
        return block;
    }

    private static BlockResponse toResponse(ScheduleBlock b) {
        return new BlockResponse(b.getId(), b.getCompanyId(), b.getHostId(),
                b.getStartAt(), b.getEndAt(), b.getReason(), b.getSource(),
                b.getCreatedBy(), b.getCreatedAt());
    }
}
