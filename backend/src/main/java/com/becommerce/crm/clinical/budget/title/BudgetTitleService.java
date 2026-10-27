package com.becommerce.crm.clinical.budget.title;

import com.becommerce.crm.clinical.budget.title.BudgetTitleDtos.BudgetTitleResponse;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Títulos de orçamento salvos por empresa. Mesmo padrão do {@code BudgetService}:
 * fixa a empresa no {@link TenantContext} para a RLS de {@code budget_titles}.
 */
@Service
public class BudgetTitleService {

    private final BudgetTitleJpaRepository repository;

    public BudgetTitleService(BudgetTitleJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<BudgetTitleResponse> list(UUID companyId) {
        try {
            TenantContext.setCompanyId(companyId);
            return repository.findByCompanyIdOrderByTitleAsc(companyId).stream()
                    .map(BudgetTitleResponse::of)
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    /** Idempotente: um título já salvo (ignorando maiúsculas) é devolvido como está. */
    @Transactional
    public BudgetTitleResponse create(UUID companyId, String title, UUID userId) {
        String normalized = title.trim().replaceAll("\\s+", " ");
        try {
            TenantContext.setCompanyId(companyId);
            return repository.findFirstByCompanyIdAndTitleIgnoreCase(companyId, normalized)
                    .map(BudgetTitleResponse::of)
                    .orElseGet(() -> BudgetTitleResponse.of(
                            repository.save(new BudgetTitleJpaEntity(companyId, normalized, userId))));
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public void delete(UUID companyId, UUID titleId) {
        try {
            TenantContext.setCompanyId(companyId);
            BudgetTitleJpaEntity entity = repository.findByIdAndCompanyId(titleId, companyId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Título de orçamento não encontrado."));
            repository.delete(entity);
        } finally {
            TenantContext.clear();
        }
    }
}
