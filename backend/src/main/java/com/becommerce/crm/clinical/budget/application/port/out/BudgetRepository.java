package com.becommerce.crm.clinical.budget.application.port.out;

import com.becommerce.crm.clinical.budget.domain.Budget;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository {

    Budget save(Budget budget);

    Optional<Budget> findById(UUID id);

    List<Budget> findByCompanyIdAndContactIdOrderByUpdatedAtDesc(UUID companyId, UUID contactId);

    void delete(Budget budget);

    /** Orçamentos não perdidos (DRAFT/APPROVED) que contêm algum destes procedimentos, por procedimento. */
    Map<UUID, Budget> findActiveByProcedureIds(UUID companyId, Collection<UUID> procedureIds);
}
