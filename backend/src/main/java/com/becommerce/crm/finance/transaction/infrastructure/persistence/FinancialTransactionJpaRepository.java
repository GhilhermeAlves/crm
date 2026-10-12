package com.becommerce.crm.finance.transaction.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FinancialTransactionJpaRepository
        extends JpaRepository<FinancialTransactionJpaEntity, UUID>,
        JpaSpecificationExecutor<FinancialTransactionJpaEntity> {

    @Query("SELECT c.id, c.firstName, c.lastName FROM ContactJpaEntity c WHERE c.id IN :ids")
    List<Object[]> findContactNames(@Param("ids") Collection<UUID> ids);

    @Query("SELECT u.id, u.name FROM UserJpaEntity u WHERE u.id IN :ids")
    List<Object[]> findUserNames(@Param("ids") Collection<UUID> ids);

    @Query("SELECT i.id, i.name FROM CatalogItemJpaEntity i WHERE i.id IN :ids")
    List<Object[]> findCatalogItemNames(@Param("ids") Collection<UUID> ids);
}
