package com.becommerce.crm.clinical.budget.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.clinical.budget.application.dto.ApproveBudgetRequest;
import com.becommerce.crm.clinical.budget.application.dto.BudgetRequest;
import com.becommerce.crm.clinical.budget.application.dto.BudgetResponse;
import com.becommerce.crm.clinical.budget.application.dto.BudgetSummaryResponse;
import com.becommerce.crm.clinical.budget.application.port.in.BudgetUseCase;
import com.becommerce.crm.clinical.budget.application.port.out.BudgetRepository;
import com.becommerce.crm.clinical.budget.domain.Budget;
import com.becommerce.crm.clinical.budget.domain.BudgetItem;
import com.becommerce.crm.clinical.budget.domain.exception.BudgetNotFoundException;
import com.becommerce.crm.clinical.procedure.application.dto.ProcedureToothDto;
import com.becommerce.crm.clinical.procedure.application.port.out.PatientProcedureRepository;
import com.becommerce.crm.clinical.procedure.domain.PatientProcedure;
import com.becommerce.crm.clinical.procedure.domain.ProcedureState;
import com.becommerce.crm.finance.transaction.application.port.output.FinancialTransactionRepository;
import com.becommerce.crm.finance.transaction.domain.FinancialTransaction;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactConflictException;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactNotFoundException;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;
import com.becommerce.crm.masterdata.contact.note.application.service.PatientTimelineRecorder;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orçamentos do paciente (Fase 3b). Agrupa procedimentos "A realizar" com valor
 * congelado e, ao aprovar, gera as parcelas financeiras de receita. Cada operação
 * isola a empresa ativa no {@link TenantContext}.
 */
@Service
public class BudgetService implements BudgetUseCase {

    private static final String RESOURCE = "Budget";

    private final BudgetRepository repository;
    private final PatientProcedureRepository procedureRepository;
    private final ContactRepository contactRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TenantAuditRecorder auditor;


    private PatientTimelineRecorder timeline;

    /** Opcional: registra os eventos nas anotações do paciente. */
    @Autowired(required = false)
    public void setPatientTimelineRecorder(PatientTimelineRecorder timeline) {
        this.timeline = timeline;
    }

    private void note(UUID companyId, UUID contactId, String body, UUID actorId) {
        if (timeline != null) {
            timeline.record(companyId, contactId, body, actorId);
        }
    }

    public BudgetService(BudgetRepository repository,
                         PatientProcedureRepository procedureRepository,
                         ContactRepository contactRepository,
                         FinancialTransactionRepository transactionRepository,
                         UserRepository userRepository,
                         TenantAuditRecorder auditor) {
        this.repository = repository;
        this.procedureRepository = procedureRepository;
        this.contactRepository = contactRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.auditor = auditor;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetSummaryResponse> list(UUID companyId, UUID contactId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            Map<UUID, String> names = new HashMap<>();
            return repository.findByCompanyIdAndContactIdOrderByUpdatedAtDesc(companyId, contactId).stream()
                    .map(b -> new BudgetSummaryResponse(b.getId(), b.getTitle(), b.getStatus(), b.subtotalCents(),
                            b.getDiscountCents(), b.totalCents(), b.getUpdatedAt(),
                            userName(b.getUpdatedBy(), names)))
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse get(UUID companyId, UUID contactId, UUID budgetId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            return toResponse(requireOwned(companyId, contactId, budgetId, false));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public BudgetResponse create(UUID companyId, UUID contactId, BudgetRequest request, UUID actorId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            List<BudgetItem> items = buildItems(companyId, contactId, request, null);
            Budget budget = Budget.create(companyId, contactId, request.title(), items, request.discountCents(),
                    request.notes(), actorId);
            repository.save(budget);

            note(companyId, contactId, "Orçamento criado: " + budget.getTitle(), actorId);

            auditor.record(companyId, AuditAction.CREATE, AuditModule.CONTACTS, RESOURCE,
                    budget.getId().toString(), "Orçamento criado.", actorId, null);
            return toResponse(budget);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public BudgetResponse update(UUID companyId, UUID contactId, UUID budgetId, BudgetRequest request,
                                 UUID actorId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            Budget budget = requireOwned(companyId, contactId, budgetId, true);
            if (!budget.isDraft()) {
                throw new ContactConflictException("Só orçamentos em rascunho podem ser editados.");
            }
            List<BudgetItem> items = buildItems(companyId, contactId, request, budget);
            budget.edit(request.title(), items, request.discountCents(), request.notes(), actorId);
            repository.save(budget);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.CONTACTS, RESOURCE,
                    budget.getId().toString(), "Orçamento atualizado.", actorId, null);
            return toResponse(budget);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public BudgetResponse approve(UUID companyId, UUID contactId, UUID budgetId, ApproveBudgetRequest request,
                                  UUID actorId, boolean canCreateFinance) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            if (!canCreateFinance) {
                throw new CrmAccessDeniedException("Você não tem permissão para gerar lançamentos financeiros.");
            }
            Budget budget = requireOwned(companyId, contactId, budgetId, true);
            if (budget.isDraft()) {
                requireItemsStillBillable(budget);
            }
            budget.approve(request.paymentMethod(), request.installments(), request.firstDueDate(), actorId);
            if (budget.totalCents() > 0) {
                List<FinancialTransaction> series = FinancialTransaction.createSeries(companyId,
                        TransactionType.INCOME, "Orçamento: " + budget.getTitle(), TransactionCategory.PROCEDURE,
                        budget.totalCents(), 0L, request.paymentMethod(), request.firstDueDate(), contactId, null,
                        null, null, null, request.installments(), actorId);
                transactionRepository.saveAll(series);
                budget.linkTransactions(series.stream().map(FinancialTransaction::getId).toList());
            }
            repository.save(budget);

            note(companyId, contactId, "Orçamento aprovado: " + budget.getTitle(), actorId);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.CONTACTS, RESOURCE,
                    budget.getId().toString(), "Orçamento aprovado.", actorId, null);
            return toResponse(budget);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public BudgetResponse lose(UUID companyId, UUID contactId, UUID budgetId, UUID actorId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            Budget budget = requireOwned(companyId, contactId, budgetId, true);
            if (budget.isApproved()) {
                List<FinancialTransaction> linked = loadTransactions(budget);
                if (linked.stream().anyMatch(tx -> tx.getStatus() == TransactionStatus.PAID)) {
                    throw new ContactConflictException(
                            "Orçamento com parcela paga não pode ser marcado como perdido.");
                }
                for (FinancialTransaction tx : linked) {
                    if (tx.getStatus() == TransactionStatus.PENDING) {
                        tx.cancel();
                        transactionRepository.save(tx);
                    }
                }
            }
            budget.markLost(actorId);
            repository.save(budget);

            note(companyId, contactId, "Orçamento marcado como perdido: " + budget.getTitle(), actorId);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.CONTACTS, RESOURCE,
                    budget.getId().toString(), "Orçamento marcado como perdido.", actorId, null);
            return toResponse(budget);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID contactId, UUID budgetId, UUID actorId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            Budget budget = requireOwned(companyId, contactId, budgetId, false);
            if (budget.isApproved()) {
                throw new ContactConflictException(
                        "Orçamento aprovado não pode ser excluído. Marque como perdido antes.");
            }
            repository.delete(budget);

            note(companyId, contactId, "Orçamento excluído: " + budget.getTitle(), actorId);

            auditor.record(companyId, AuditAction.DELETE, AuditModule.CONTACTS, RESOURCE,
                    budgetId.toString(), "Orçamento excluído.", actorId, null);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Monta os itens na ordem pedida. No update, reaproveita o id e o preço do item
     * existente do mesmo procedimento (a UNIQUE budget_id+procedure_id estoura se o id mudar).
     */
    private List<BudgetItem> buildItems(UUID companyId, UUID contactId, BudgetRequest request, Budget current) {
        List<UUID> procedureIds = request.procedureIds();
        Map<UUID, Budget> active = repository.findActiveByProcedureIds(companyId, procedureIds);
        Map<UUID, BudgetItem> existing = current == null ? Map.of()
                : current.getItems().stream().collect(Collectors.toMap(BudgetItem::procedureId, Function.identity()));
        Map<UUID, Long> prices = request.itemPrices() == null ? Map.of() : request.itemPrices();
        List<BudgetItem> items = new ArrayList<>();
        for (UUID procedureId : procedureIds) {
            PatientProcedure procedure = procedureRepository.findById(procedureId)
                    .filter(p -> p.getCompanyId().equals(companyId) && p.getContactId().equals(contactId))
                    .orElseThrow(() -> new ContactValidationException("Procedimento não encontrado para este paciente."));
            if (procedure.getState() != ProcedureState.PLANNED) {
                throw new ContactValidationException("Só procedimentos \"A realizar\" podem entrar no orçamento.");
            }
            Budget other = active.get(procedureId);
            if (other != null && (current == null || !other.getId().equals(current.getId()))) {
                throw new ContactConflictException("O procedimento " + procedure.getNameSnapshot()
                        + " já está no orçamento " + other.getTitle() + ".");
            }
            BudgetItem previous = existing.get(procedureId);
            Long informed = prices.get(procedureId);
            long price = informed != null ? informed
                    : previous != null ? previous.priceCents() : procedure.getPriceCents();
            UUID itemId = previous != null ? previous.id() : UUID.randomUUID();
            items.add(new BudgetItem(itemId, procedureId, price, items.size()));
        }
        return items;
    }

    /** Evita cobrança dupla: procedimento concluído (ou já cobrado) depois de entrar no rascunho barra a aprovação. */
    private void requireItemsStillBillable(Budget budget) {
        for (BudgetItem item : budget.getItems()) {
            PatientProcedure procedure = procedureRepository.findById(item.procedureId()).orElse(null);
            if (procedure == null || procedure.getState() != ProcedureState.PLANNED
                    || procedure.getFinancialTransactionId() != null) {
                String name = procedure == null ? "removido" : procedure.getNameSnapshot();
                throw new ContactConflictException("O procedimento " + name
                        + " não está mais \"A realizar\". Atualize o orçamento antes de aprovar.");
            }
        }
    }

    private List<FinancialTransaction> loadTransactions(Budget budget) {
        List<FinancialTransaction> result = new ArrayList<>();
        for (UUID id : budget.getTransactionIds()) {
            transactionRepository.findById(id).ifPresent(result::add);
        }
        return result;
    }

    private void requireOwnedContact(UUID companyId, UUID contactId) {
        var contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ContactNotFoundException(contactId));
        if (!contact.getCompanyId().equals(companyId)) {
            throw new ContactNotFoundException(contactId);
        }
    }

    private Budget requireOwned(UUID companyId, UUID contactId, UUID budgetId, boolean forUpdate) {
        Budget budget = (forUpdate ? repository.findByIdForUpdate(budgetId) : repository.findById(budgetId))
                .orElseThrow(() -> new BudgetNotFoundException(budgetId));
        if (!budget.getCompanyId().equals(companyId) || !budget.getContactId().equals(contactId)) {
            throw new BudgetNotFoundException(budgetId);
        }
        return budget;
    }

    private BudgetResponse toResponse(Budget b) {
        Map<UUID, String> names = new HashMap<>();
        List<BudgetResponse.Item> items = new ArrayList<>();
        for (BudgetItem item : b.getItems()) {
            PatientProcedure p = procedureRepository.findById(item.procedureId()).orElse(null);
            if (p == null) {
                items.add(new BudgetResponse.Item(item.procedureId(), null, List.of(), null, item.priceCents()));
                continue;
            }
            items.add(new BudgetResponse.Item(item.procedureId(), p.getNameSnapshot(),
                    p.getTeeth().stream().map(t -> new ProcedureToothDto(t.tooth(), t.faces())).toList(),
                    userName(p.getProfessionalId(), names), item.priceCents()));
        }
        List<BudgetResponse.Installment> installments = loadTransactions(b).stream()
                .sorted(Comparator.comparing(FinancialTransaction::getDueDate))
                .map(tx -> new BudgetResponse.Installment(tx.getId(), tx.getDueDate(), tx.getAmountCents(),
                        tx.getStatus()))
                .toList();
        return new BudgetResponse(b.getId(), b.getContactId(), b.getTitle(), b.getStatus(), b.subtotalCents(),
                b.getDiscountCents(), b.totalCents(), b.getNotes(), b.getPaymentMethod(), b.getInstallments(),
                b.getFirstDueDate(), b.getApprovedAt(), b.getLostAt(), items, installments, b.getUpdatedAt(),
                userName(b.getUpdatedBy(), names));
    }

    private String userName(UUID userId, Map<UUID, String> cache) {
        if (userId == null) {
            return null;
        }
        if (cache.containsKey(userId)) {
            return cache.get(userId);
        }
        String name = userRepository.findById(userId).map(User::getName).orElse(null);
        cache.put(userId, name);
        return name;
    }
}
