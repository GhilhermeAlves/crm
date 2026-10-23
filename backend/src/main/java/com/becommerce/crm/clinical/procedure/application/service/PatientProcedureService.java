package com.becommerce.crm.clinical.procedure.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.clinical.budget.application.port.out.BudgetRepository;
import com.becommerce.crm.clinical.budget.domain.Budget;
import com.becommerce.crm.clinical.procedure.application.dto.CompleteProcedureRequest;
import com.becommerce.crm.clinical.procedure.application.dto.PatientProcedureRequest;
import com.becommerce.crm.clinical.procedure.application.dto.PatientProcedureResponse;
import com.becommerce.crm.clinical.procedure.application.dto.ProcedureToothDto;
import com.becommerce.crm.clinical.procedure.application.port.in.PatientProcedureUseCase;
import com.becommerce.crm.clinical.procedure.application.port.out.PatientProcedureRepository;
import com.becommerce.crm.clinical.procedure.domain.PatientProcedure;
import com.becommerce.crm.clinical.procedure.domain.ProcedureState;
import com.becommerce.crm.clinical.procedure.domain.ProcedureTooth;
import com.becommerce.crm.clinical.procedure.domain.exception.PatientProcedureNotFoundException;
import com.becommerce.crm.finance.transaction.application.port.output.FinancialTransactionRepository;
import com.becommerce.crm.finance.transaction.domain.FinancialTransaction;
import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException;
import com.becommerce.crm.identity.membership.application.port.output.MembershipRepository;
import com.becommerce.crm.masterdata.catalog.application.port.output.CatalogItemRepository;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItem;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactConflictException;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactNotFoundException;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Procedimentos odontológicos do paciente (ficha clínica, Fase 3a). Guarda o nome
 * do serviço do catálogo como snapshot e, opcionalmente, gera o lançamento
 * financeiro de receita ao concluir. Cada operação isola a empresa ativa no
 * {@link TenantContext}.
 */
@Service
public class PatientProcedureService implements PatientProcedureUseCase {

    private static final String RESOURCE = "PatientProcedure";

    private final PatientProcedureRepository repository;
    private final ContactRepository contactRepository;
    private final CatalogItemRepository catalogRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final TenantAuditRecorder auditor;
    private final BudgetRepository budgetRepository;

    public PatientProcedureService(PatientProcedureRepository repository,
                                   ContactRepository contactRepository,
                                   CatalogItemRepository catalogRepository,
                                   FinancialTransactionRepository transactionRepository,
                                   UserRepository userRepository,
                                   MembershipRepository membershipRepository,
                                   TenantAuditRecorder auditor,
                                   BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
        this.repository = repository;
        this.contactRepository = contactRepository;
        this.catalogRepository = catalogRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.auditor = auditor;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientProcedureResponse> list(UUID companyId, UUID contactId, ProcedureState state,
                                               String category, Integer tooth) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            Map<UUID, String> categories = new HashMap<>();
            Map<UUID, String> names = new HashMap<>();
            List<PatientProcedure> all = repository.findByCompanyIdAndContactIdOrderByCreatedAtDesc(companyId, contactId);
            Map<UUID, Budget> budgets = all.isEmpty() ? Map.of()
                    : budgetRepository.findActiveByProcedureIds(companyId, all.stream().map(PatientProcedure::getId).toList());
            return all.stream()
                    .filter(p -> state == null || p.getState() == state)
                    .filter(p -> tooth == null || p.getTeeth().stream().anyMatch(t -> t.tooth() == tooth))
                    .filter(p -> category == null || category.isBlank()
                            || category.equalsIgnoreCase(categoryOf(p.getCatalogItemId(), categories)))
                    .map(p -> toResponse(p, categories, names, null, budgets.get(p.getId())))
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public PatientProcedureResponse create(UUID companyId, UUID contactId, PatientProcedureRequest request,
                                           UUID actorId, boolean canCreateFinance) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            CatalogItem item = requireService(companyId, request.catalogItemId());
            requireProfessional(companyId, request.professionalId());
            PatientProcedure procedure = PatientProcedure.create(companyId, contactId, request.catalogItemId(),
                    item.getName(), request.state(), request.dentition(), request.priceCents(),
                    request.professionalId(), request.notes(), request.performedAt(), toTeeth(request.teeth()),
                    actorId);
            procedure.setGenerateTransactionOnDone(request.generateTransaction());
            rememberPriceInCatalog(item, request.priceCents());
            FinancialTransaction generated = maybeGenerateTransaction(procedure, request.generateTransaction(),
                    request.dueDate(), actorId, canCreateFinance);
            repository.save(procedure);

            auditor.record(companyId, AuditAction.CREATE, AuditModule.CONTACTS, RESOURCE,
                    procedure.getId().toString(), "Procedimento registrado na ficha clínica.", actorId, null);
            Map<UUID, String> categories = new HashMap<>();
            categories.put(request.catalogItemId(), item.getCategory());
            return toResponse(procedure, categories, new HashMap<>(), generated, null);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public PatientProcedureResponse update(UUID companyId, UUID contactId, UUID procedureId,
                                           PatientProcedureRequest request, UUID actorId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            PatientProcedure procedure = requireOwned(companyId, contactId, procedureId);
            requireProfessional(companyId, request.professionalId());
            List<ProcedureTooth> teeth = toTeeth(request.teeth());
            if (procedure.isDone()) {
                if (!sameExceptNotes(procedure, request, teeth)) {
                    throw new ContactConflictException("Procedimento realizado só permite alterar as observações.");
                }
                procedure.editNotes(request.notes());
            } else {
                procedure.edit(request.state(), request.dentition(), request.priceCents(),
                        request.professionalId(), request.notes(), teeth);
                procedure.setGenerateTransactionOnDone(request.generateTransaction());
            }
            repository.save(procedure);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.CONTACTS, RESOURCE,
                    procedure.getId().toString(), "Procedimento atualizado.", actorId, null);
            return toResponse(procedure, new HashMap<>(), new HashMap<>());
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public PatientProcedureResponse complete(UUID companyId, UUID contactId, UUID procedureId,
                                             CompleteProcedureRequest request, UUID actorId,
                                             boolean canCreateFinance) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            PatientProcedure procedure = requireOwned(companyId, contactId, procedureId);
            requireProfessional(companyId, request.professionalId());
            if (request.generateTransaction()) {
                Budget active = activeBudgetOf(companyId, procedureId);
                if (active != null && active.isApproved()) {
                    throw new ContactValidationException("Este procedimento já é cobrado pelo orçamento aprovado.");
                }
            }
            procedure.complete(request.performedAt(), request.professionalId());
            FinancialTransaction generated = maybeGenerateTransaction(procedure, request.generateTransaction(),
                    request.dueDate(), actorId, canCreateFinance);
            repository.save(procedure);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.CONTACTS, RESOURCE,
                    procedure.getId().toString(), "Procedimento marcado como realizado.", actorId, null);
            return toResponse(procedure, new HashMap<>(), new HashMap<>(), generated,
                    activeBudgetOf(companyId, procedureId));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID contactId, UUID procedureId, UUID actorId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            PatientProcedure procedure = requireOwned(companyId, contactId, procedureId);
            Budget active = activeBudgetOf(companyId, procedureId);
            if (active != null && active.isApproved()) {
                throw new ContactConflictException("Procedimento de orçamento aprovado não pode ser excluído.");
            }
            if (procedure.getFinancialTransactionId() != null) {
                FinancialTransaction tx = transactionRepository.findById(procedure.getFinancialTransactionId())
                        .orElse(null);
                if (tx != null && tx.getStatus() == TransactionStatus.PAID) {
                    throw new ContactConflictException(
                            "Procedimento com pagamento registrado não pode ser excluído.");
                }
                if (tx != null && tx.getStatus() == TransactionStatus.PENDING) {
                    tx.cancel();
                    transactionRepository.save(tx);
                }
            }
            detachFromDraftBudget(active, procedureId, actorId);
            repository.delete(procedure);

            auditor.record(companyId, AuditAction.DELETE, AuditModule.CONTACTS, RESOURCE,
                    procedureId.toString(), "Procedimento excluído da ficha clínica.", actorId, null);
        } finally {
            TenantContext.clear();
        }
    }

    private Budget activeBudgetOf(UUID companyId, UUID procedureId) {
        return budgetRepository.findActiveByProcedureIds(companyId, List.of(procedureId)).get(procedureId);
    }

    private void detachFromDraftBudget(Budget budget, UUID procedureId, UUID actorId) {
        if (budget == null || !budget.isDraft()) {
            return;
        }
        budget.removeItemForProcedure(procedureId, actorId);
        if (budget.getItems().isEmpty()) {
            budgetRepository.delete(budget);
        } else {
            budgetRepository.save(budget);
        }
    }

    /** Serviço sem preço no catálogo passa a usar o valor informado nas próximas vezes. */
    private void rememberPriceInCatalog(CatalogItem item, Long priceCents) {
        if (item.getPrice() != null || priceCents == null || priceCents <= 0) {
            return;
        }
        item.update(item.getType(), item.getName(), item.getDescription(), item.getCategory(),
                BigDecimal.valueOf(priceCents, 2), item.getCurrency(), item.getSku(), item.getUnit(),
                item.getMinStock());
        catalogRepository.save(item);
    }

    private void requireProfessional(UUID companyId, UUID professionalId) {
        if (professionalId != null && !membershipRepository.existsActiveByUserIdAndCompanyId(professionalId, companyId)) {
            throw new ContactValidationException("Profissional não encontrado nesta empresa.");
        }
    }

    private CatalogItem requireService(UUID companyId, UUID catalogItemId) {
        CatalogItem item = catalogRepository.findById(catalogItemId)
                .filter(found -> found.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ContactValidationException("Serviço não encontrado no catálogo."));
        if (item.getType() != CatalogItemType.SERVICE || !item.isActive()) {
            throw new ContactValidationException("Escolha um serviço ativo do catálogo.");
        }
        return item;
    }

    private FinancialTransaction maybeGenerateTransaction(PatientProcedure procedure, boolean generate,
                                                          LocalDate dueDate, UUID actorId,
                                                          boolean canCreateFinance) {
        if (!generate || !procedure.isDone()) {
            return null;
        }
        if (!canCreateFinance) {
            throw new CrmAccessDeniedException("Você não tem permissão para gerar lançamentos financeiros.");
        }
        if (procedure.getPriceCents() <= 0) {
            throw new ContactValidationException("Informe um valor maior que zero para gerar o lançamento.");
        }
        FinancialTransaction tx = FinancialTransaction.create(UUID.randomUUID(), procedure.getCompanyId(),
                TransactionType.INCOME, describe(procedure), TransactionCategory.PROCEDURE,
                procedure.getPriceCents(), 0L, PaymentMethod.OTHER, dueDate == null ? LocalDate.now() : dueDate,
                procedure.getContactId(), procedure.getProfessionalId(), procedure.getCatalogItemId(), null, null,
                actorId);
        transactionRepository.save(tx);
        procedure.linkTransaction(tx.getId());
        return tx;
    }

    static String describe(PatientProcedure procedure) {
        String teeth = procedure.getTeeth().isEmpty() ? "boca toda"
                : "dentes " + procedure.getTeeth().stream().map(t -> String.valueOf(t.tooth()))
                        .collect(Collectors.joining(", "));
        return "Procedimento: " + procedure.getNameSnapshot() + " – " + teeth;
    }

    private static boolean sameExceptNotes(PatientProcedure procedure, PatientProcedureRequest request,
                                           List<ProcedureTooth> teeth) {
        return procedure.getState() == request.state()
                && procedure.getDentition() == request.dentition()
                && request.priceCents() != null && procedure.getPriceCents() == request.priceCents()
                && Objects.equals(procedure.getProfessionalId(), request.professionalId())
                && new HashSet<>(procedure.getTeeth()).equals(new HashSet<>(teeth));
    }

    private static List<ProcedureTooth> toTeeth(List<ProcedureToothDto> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream().map(dto -> new ProcedureTooth(dto.tooth(), dto.faces())).toList();
    }

    private void requireOwnedContact(UUID companyId, UUID contactId) {
        var contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ContactNotFoundException(contactId));
        if (!contact.getCompanyId().equals(companyId)) {
            throw new ContactNotFoundException(contactId);
        }
    }

    private PatientProcedure requireOwned(UUID companyId, UUID contactId, UUID procedureId) {
        PatientProcedure procedure = repository.findById(procedureId)
                .orElseThrow(() -> new PatientProcedureNotFoundException(procedureId));
        if (!procedure.getCompanyId().equals(companyId) || !procedure.getContactId().equals(contactId)) {
            throw new PatientProcedureNotFoundException(procedureId);
        }
        return procedure;
    }

    private String categoryOf(UUID catalogItemId, Map<UUID, String> cache) {
        if (catalogItemId == null) {
            return null;
        }
        if (cache.containsKey(catalogItemId)) {
            return cache.get(catalogItemId);
        }
        String category = catalogRepository.findById(catalogItemId).map(CatalogItem::getCategory).orElse(null);
        cache.put(catalogItemId, category);
        return category;
    }

    private PatientProcedureResponse toResponse(PatientProcedure p, Map<UUID, String> categories,
                                                Map<UUID, String> names) {
        return toResponse(p, categories, names, null, activeBudgetOf(p.getCompanyId(), p.getId()));
    }

    private PatientProcedureResponse toResponse(PatientProcedure p, Map<UUID, String> categories,
                                                Map<UUID, String> names, FinancialTransaction known, Budget budget) {
        PatientProcedureResponse.TransactionRef transaction = null;
        if (known != null) {
            transaction = new PatientProcedureResponse.TransactionRef(known.getId(), known.getStatus());
        } else if (p.getFinancialTransactionId() != null) {
            transaction = transactionRepository.findById(p.getFinancialTransactionId())
                    .map(tx -> new PatientProcedureResponse.TransactionRef(tx.getId(), tx.getStatus()))
                    .orElse(null);
        }
        return new PatientProcedureResponse(p.getId(), p.getContactId(), p.getCatalogItemId(),
                p.getNameSnapshot(), categoryOf(p.getCatalogItemId(), categories), p.getState(),
                p.getDentition(), p.getPriceCents(), p.getProfessionalId(),
                userName(p.getCompanyId(), p.getProfessionalId(), names), p.getNotes(), p.getPerformedAt(),
                p.getTeeth().stream().map(t -> new ProcedureToothDto(t.tooth(), t.faces())).toList(),
                transaction, p.getCreatedAt(), p.getUpdatedAt(),
                budget == null ? null : new PatientProcedureResponse.BudgetRef(budget.getId(), budget.getTitle(), budget.getStatus()),
                p.isGenerateTransactionOnDone());
    }

    private String userName(UUID companyId, UUID userId, Map<UUID, String> cache) {
        if (userId == null) {
            return null;
        }
        if (cache.containsKey(userId)) {
            return cache.get(userId);
        }
        String name = userRepository.findById(userId)
                .filter(user -> companyId.equals(user.getCompanyId())
                        || membershipRepository.existsActiveByUserIdAndCompanyId(userId, companyId))
                .map(User::getName).orElse(null);
        cache.put(userId, name);
        return name;
    }
}
