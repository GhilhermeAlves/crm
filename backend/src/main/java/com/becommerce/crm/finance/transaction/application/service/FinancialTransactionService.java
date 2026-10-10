package com.becommerce.crm.finance.transaction.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.finance.transaction.application.dto.CreateFinancialTransactionRequest;
import com.becommerce.crm.finance.transaction.application.dto.FinancialTransactionResponse;
import com.becommerce.crm.finance.transaction.application.dto.PayTransactionRequest;
import com.becommerce.crm.finance.transaction.application.dto.UpdateFinancialTransactionRequest;
import com.becommerce.crm.finance.transaction.application.port.output.FinancialTransactionQuery;
import com.becommerce.crm.finance.transaction.application.port.output.FinancialTransactionRepository;
import com.becommerce.crm.finance.transaction.domain.FinancialTransaction;
import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.finance.transaction.domain.exception.FinancialTransactionNotFoundException;
import com.becommerce.crm.finance.transaction.domain.exception.FinancialTransactionValidationException;
import com.becommerce.crm.shared.application.dto.PageResponse;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Lançamentos financeiros (V092). Isolamento por {@code TenantContext} + RLS FORCE.
 * Valores em centavos; "atrasado" é calculado na leitura (PENDING com vencimento passado).
 */
@Service
public class FinancialTransactionService {

    private static final String ENTITY = "FinancialTransaction";
    private static final String OVERDUE = "OVERDUE";
    private static final int MAX_PAGE_SIZE = 100;

    private final FinancialTransactionRepository repository;
    private final TenantAuditRecorder auditor;

    public FinancialTransactionService(FinancialTransactionRepository repository, TenantAuditRecorder auditor) {
        this.repository = repository;
        this.auditor = auditor;
    }

    @Transactional(readOnly = true)
    public PageResponse<FinancialTransactionResponse> list(UUID companyId, TransactionType type, String status,
                                                           LocalDate from, LocalDate to,
                                                           PaymentMethod paymentMethod,
                                                           TransactionCategory category,
                                                           UUID professionalUserId, UUID contactId, String query,
                                                           int page, int pageSize) {
        int size = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        LocalDate today = LocalDate.now();
        try {
            TenantContext.setCompanyId(companyId);
            String normalizedStatus = null;
            LocalDate overdueBefore = null;
            if (isOverdue(status)) {
                normalizedStatus = TransactionStatus.PENDING.name();
                overdueBefore = today;
            } else if (status != null && !status.isBlank()) {
                normalizedStatus = parseStatus(status).name();
            }
            FinancialTransactionQuery filter = new FinancialTransactionQuery(type, normalizedStatus,
                    overdueBefore, from, to, paymentMethod, category, professionalUserId, contactId,
                    pattern(query));
            var result = repository.search(companyId, filter, safePage, size);
            List<FinancialTransaction> content = result.content();
            Map<UUID, FinancialTransactionRepository.TransactionReference> references =
                    repository.findReferences(content.stream().map(FinancialTransaction::getId).toList());
            List<FinancialTransactionResponse> responses = new ArrayList<>();
            for (FinancialTransaction transaction : content) {
                responses.add(FinancialTransactionResponse.from(transaction, references.get(transaction.getId()),
                        today));
            }
            return PageResponse.of(responses, safePage, size, result.totalElements());
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional(readOnly = true)
    public FinancialTransactionResponse getById(UUID companyId, UUID transactionId) {
        try {
            TenantContext.setCompanyId(companyId);
            return toResponse(requireOwned(companyId, transactionId));
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public List<FinancialTransactionResponse> create(UUID companyId, CreateFinancialTransactionRequest request,
                                                     UUID createdBy) {
        int installments = request.installments() == null ? 1 : request.installments();
        try {
            TenantContext.setCompanyId(companyId);
            List<FinancialTransaction> created = repository.saveAll(FinancialTransaction.createSeries(companyId,
                    request.type(), request.description(), request.category(), request.amountCents(),
                    request.feeCents() == null ? 0L : request.feeCents(), request.paymentMethod(),
                    request.dueDate(), request.contactId(), request.professionalUserId(), request.catalogItemId(),
                    request.notes(), request.paidAt(), installments, createdBy));
            FinancialTransaction first = created.get(0);
            String description = "Lançamento financeiro criado: " + first.getDescription()
                    + (installments > 1 ? " (" + installments + " parcelas)" : "");
            auditor.record(companyId, AuditAction.CREATE, AuditModule.FINANCE, ENTITY, first.getId().toString(),
                    description, createdBy, Map.of("type", first.getType().name()));
            return toResponses(created);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public FinancialTransactionResponse update(UUID companyId, UUID transactionId,
                                               UpdateFinancialTransactionRequest request, UUID actor) {
        try {
            TenantContext.setCompanyId(companyId);
            FinancialTransaction transaction = requireOwned(companyId, transactionId);
            transaction.update(request.type(), request.description(), request.category(), request.amountCents(),
                    request.feeCents() == null ? 0L : request.feeCents(), request.paymentMethod(),
                    request.dueDate(), request.contactId(), request.professionalUserId(), request.catalogItemId(),
                    request.notes(), request.paidAt());
            FinancialTransaction saved = repository.save(transaction);
            auditor.record(companyId, AuditAction.UPDATE, AuditModule.FINANCE, ENTITY, saved.getId().toString(),
                    "Lançamento financeiro atualizado: " + saved.getDescription(), actor, null);
            return toResponse(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public FinancialTransactionResponse pay(UUID companyId, UUID transactionId, PayTransactionRequest request,
                                            UUID actor) {
        try {
            TenantContext.setCompanyId(companyId);
            FinancialTransaction transaction = requireOwned(companyId, transactionId);
            transaction.pay(request.paidAt());
            FinancialTransaction saved = repository.save(transaction);
            auditor.record(companyId, AuditAction.UPDATE, AuditModule.FINANCE, ENTITY, saved.getId().toString(),
                    "Lançamento financeiro pago: " + saved.getDescription(), actor,
                    Map.of("paidAt", String.valueOf(saved.getPaidAt())));
            return toResponse(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public FinancialTransactionResponse cancel(UUID companyId, UUID transactionId, UUID actor) {
        try {
            TenantContext.setCompanyId(companyId);
            FinancialTransaction transaction = requireOwned(companyId, transactionId);
            transaction.cancel();
            FinancialTransaction saved = repository.save(transaction);
            auditor.record(companyId, AuditAction.UPDATE, AuditModule.FINANCE, ENTITY, saved.getId().toString(),
                    "Lançamento financeiro cancelado: " + saved.getDescription(), actor, null);
            return toResponse(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public void delete(UUID companyId, UUID transactionId, UUID actor) {
        try {
            TenantContext.setCompanyId(companyId);
            FinancialTransaction transaction = requireOwned(companyId, transactionId);
            repository.delete(transaction);
            auditor.record(companyId, AuditAction.DELETE, AuditModule.FINANCE, ENTITY, transactionId.toString(),
                    "Lançamento financeiro excluído: " + transaction.getDescription(), actor, null);
        } finally {
            TenantContext.clear();
        }
    }

    private FinancialTransaction requireOwned(UUID companyId, UUID transactionId) {
        FinancialTransaction transaction = repository.findById(transactionId)
                .orElseThrow(() -> new FinancialTransactionNotFoundException(transactionId));
        if (!transaction.getCompanyId().equals(companyId)) {
            throw new FinancialTransactionNotFoundException(transactionId);
        }
        return transaction;
    }

    private FinancialTransactionResponse toResponse(FinancialTransaction transaction) {
        Map<UUID, FinancialTransactionRepository.TransactionReference> references =
                repository.findReferences(List.of(transaction.getId()));
        return FinancialTransactionResponse.from(transaction, references.get(transaction.getId()),
                LocalDate.now());
    }

    private List<FinancialTransactionResponse> toResponses(List<FinancialTransaction> transactions) {
        Map<UUID, FinancialTransactionRepository.TransactionReference> references =
                repository.findReferences(transactions.stream().map(FinancialTransaction::getId).toList());
        LocalDate today = LocalDate.now();
        List<FinancialTransactionResponse> responses = new ArrayList<>();
        for (FinancialTransaction transaction : transactions) {
            responses.add(FinancialTransactionResponse.from(transaction, references.get(transaction.getId()),
                    today));
        }
        return responses;
    }

    private static boolean isOverdue(String status) {
        return status != null && OVERDUE.equalsIgnoreCase(status.trim());
    }

    private static TransactionStatus parseStatus(String status) {
        try {
            return TransactionStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new FinancialTransactionValidationException("Status inválido: " + status + ".");
        }
    }

    /** {@code pattern} já vem em minúsculas com {@code %...%} (ou nulo = sem busca textual). */
    private static String pattern(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return "%" + escapeLike(query.trim().toLowerCase(Locale.ROOT)) + "%";
    }

    /** '%' e '_' digitados pelo usuário são literais, não curingas. */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
