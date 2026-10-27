package com.becommerce.crm.clinical.budget.title;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetTitleJpaRepository extends JpaRepository<BudgetTitleJpaEntity, UUID> {

    List<BudgetTitleJpaEntity> findByCompanyIdOrderByTitleAsc(UUID companyId);

    Optional<BudgetTitleJpaEntity> findFirstByCompanyIdAndTitleIgnoreCase(UUID companyId, String title);

    Optional<BudgetTitleJpaEntity> findByIdAndCompanyId(UUID id, UUID companyId);
}
