package com.becommerce.crm.clinical.budget.application.service;

import com.becommerce.crm.clinical.budget.domain.Budget;
import com.becommerce.crm.finance.transaction.application.port.output.FinancialTransactionRepository;
import com.becommerce.crm.finance.transaction.domain.FinancialTransaction;
import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Orçamento em rascunho aparece no financeiro como receita prevista (pendente, valor
 * total, descrição "Orçamento: título"), como no Capim. Criar/editar mantém esse
 * lançamento em dia; a aprovação troca ele pelas parcelas. Chamar antes de salvar o orçamento.
 */
@Component
public class BudgetDraftTransactionSync {

    private final FinancialTransactionRepository transactionRepository;

    public BudgetDraftTransactionSync(FinancialTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public void sync(Budget budget, UUID actorId) {
        if (!budget.isDraft()) {
            return;
        }
        FinancialTransaction current = budget.getTransactionIds().stream()
                .map(transactionRepository::findById)
                .flatMap(java.util.Optional::stream)
                .findFirst()
                .orElse(null);
        String description = "Orçamento: " + budget.getTitle();
        long total = budget.totalCents();
        if (total <= 0) {
            if (current != null && current.getStatus() != TransactionStatus.PAID) {
                transactionRepository.delete(current);
                budget.linkTransactions(List.of());
            }
            return;
        }
        if (current == null) {
            FinancialTransaction tx = FinancialTransaction.create(UUID.randomUUID(), budget.getCompanyId(),
                    TransactionType.INCOME, description, TransactionCategory.PROCEDURE, total, 0L,
                    PaymentMethod.OTHER, LocalDate.now(), budget.getContactId(), null, null, null, null, actorId);
            transactionRepository.save(tx);
            budget.linkTransactions(List.of(tx.getId()));
            return;
        }
        if (current.getStatus() == TransactionStatus.PENDING) {
            current.update(current.getType(), description, current.getCategory(), total, current.getFeeCents(),
                    current.getPaymentMethod(), current.getDueDate(), current.getContactId(),
                    current.getProfessionalUserId(), current.getCatalogItemId(), current.getNotes(), null);
            transactionRepository.save(current);
        }
    }

    /** Remove o lançamento previsto (orçamento excluído). Pago fica no financeiro. */
    public void discard(Budget budget) {
        budget.getTransactionIds().stream()
                .map(transactionRepository::findById)
                .flatMap(java.util.Optional::stream)
                .filter(tx -> tx.getStatus() != TransactionStatus.PAID)
                .forEach(transactionRepository::delete);
    }
}
