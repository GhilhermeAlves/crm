package com.becommerce.crm.automation.ai.application.memory;

import com.becommerce.crm.automation.ai.application.dto.AgentMemoryRequest;
import com.becommerce.crm.automation.ai.application.dto.AgentMemoryResponse;
import com.becommerce.crm.automation.ai.application.port.output.AgentConfigRepository;
import com.becommerce.crm.automation.ai.domain.AgentConfig;
import com.becommerce.crm.automation.ai.domain.MemorySource;
import com.becommerce.crm.automation.ai.domain.MemoryType;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Gestão das memórias pela equipe (ver, registrar, corrigir, apagar — inclusive
 * para atender pedidos de titulares/LGPD). A empresa vem SEMPRE do usuário
 * autenticado; o contato precisa pertencer a ela.
 */
@Service
public class AgentMemoryAdminService {

    static final int LIST_LIMIT = 100;

    private final AgentMemoryService memoryService;
    private final AgentConfigRepository agentConfigRepository;
    private final ContactRepository contactRepository;

    public AgentMemoryAdminService(AgentMemoryService memoryService, AgentConfigRepository agentConfigRepository,
                                   ContactRepository contactRepository) {
        this.memoryService = memoryService;
        this.agentConfigRepository = agentConfigRepository;
        this.contactRepository = contactRepository;
    }

    /** Erro de regra (400) com mensagem para o usuário. */
    public static class MemoryRequestException extends RuntimeException {
        public MemoryRequestException(String message) {
            super(message);
        }
    }

    @Transactional(readOnly = true)
    public List<AgentMemoryResponse> list(UUID companyId, UUID contactId) {
        return inTenant(companyId, () -> {
            requireContact(companyId, contactId);
            return memoryService.listByContact(companyId, contactId, LIST_LIMIT).stream()
                    .map(AgentMemoryResponse::from).toList();
        });
    }

    @Transactional
    public AgentMemoryResponse create(UUID companyId, AgentMemoryRequest request) {
        return inTenant(companyId, () -> {
            requireContact(companyId, request.contactId());
            AgentConfig agent = agentConfigRepository.findByCompanyId(companyId)
                    .orElseThrow(() -> new MemoryRequestException("Configure o agente antes de registrar memórias."));
            AgentMemoryService.SaveOutcome outcome;
            try {
                outcome = memoryService.save(companyId, agent.getId(), request.contactId(),
                        MemoryType.parse(request.type()), request.content(), request.importance(),
                        MemorySource.USER, null);
            } catch (IllegalArgumentException e) {
                throw new MemoryRequestException(e.getMessage());
            }
            if (!outcome.saved()) {
                throw new MemoryRequestException(outcome.rejectionReason());
            }
            return AgentMemoryResponse.from(outcome.memory());
        });
    }

    @Transactional
    public Optional<AgentMemoryResponse> update(UUID companyId, UUID memoryId, AgentMemoryRequest request) {
        return inTenant(companyId, () -> {
            try {
                return memoryService.update(companyId, memoryId, MemoryType.parse(request.type()),
                        request.content(), request.importance(), null).map(AgentMemoryResponse::from);
            } catch (IllegalArgumentException e) {
                throw new MemoryRequestException(e.getMessage());
            }
        });
    }

    @Transactional
    public boolean delete(UUID companyId, UUID memoryId) {
        return inTenant(companyId, () -> memoryService.delete(companyId, memoryId));
    }

    private void requireContact(UUID companyId, UUID contactId) {
        if (contactId == null || contactRepository.findById(contactId)
                .filter(c -> companyId.equals(c.getCompanyId())).isEmpty()) {
            throw new MemoryRequestException("Contato não encontrado.");
        }
    }

    private static <T> T inTenant(UUID companyId, java.util.function.Supplier<T> action) {
        try {
            TenantContext.setCompanyId(companyId);
            return action.get();
        } finally {
            TenantContext.clear();
        }
    }
}
