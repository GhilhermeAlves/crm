package com.becommerce.crm.finance.transaction.application.port.output;

import com.becommerce.crm.finance.transaction.domain.FinancialTransaction;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída dos lançamentos financeiros ({@code financial_transactions},
 * V092). RLS FORCE isola o tenant. Ordenação padrão da listagem: {@code due_date} desc.
 */
public interface FinancialTransactionRepository {

    FinancialTransaction save(FinancialTransaction transaction);

    List<FinancialTransaction> saveAll(List<FinancialTransaction> transactions);

    Optional<FinancialTransaction> findById(UUID id);

    void delete(FinancialTransaction transaction);

    PageResult search(UUID companyId, FinancialTransactionQuery query, int page, int pageSize);

    /** Nomes resolvidos (contato, profissional e item de catálogo) por lançamento. */
    Map<UUID, TransactionReference> findReferences(Collection<UUID> transactionIds);

    record PageResult(List<FinancialTransaction> content, long totalElements) {}

    record TransactionReference(String contactName, String professionalName, String catalogItemName) {}
}
