package com.becommerce.crm.clinical.budget.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetJpaRepository extends JpaRepository<BudgetJpaEntity, UUID> {

    List<BudgetJpaEntity> findByCompanyIdAndContactIdOrderByUpdatedAtDesc(UUID companyId, UUID contactId);

    @Query("select distinct b from BudgetJpaEntity b join b.items i "
            + "where b.companyId = :companyId and b.status <> 'LOST' and i.procedureId in :procedureIds")
    List<BudgetJpaEntity> findActiveByProcedureIds(@Param("companyId") UUID companyId,
                                                   @Param("procedureIds") Collection<UUID> procedureIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BudgetJpaEntity b where b.id = :id")
    Optional<BudgetJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
