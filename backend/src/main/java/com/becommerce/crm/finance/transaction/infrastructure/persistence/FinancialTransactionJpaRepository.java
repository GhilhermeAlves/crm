package com.becommerce.crm.finance.transaction.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FinancialTransactionJpaRepository
        extends JpaRepository<FinancialTransactionJpaEntity, UUID> {

    /**
     * Listagem filtrada. Todos os parâmetros nulos não filtram. {@code overdueBefore}
     * implementa o status calculado "OVERDUE" (PENDING + dueDate anterior à data).
     * {@code pattern} já vem em minúsculas com {@code %...%} (ou nulo = sem busca).
     */
    @Query("SELECT t FROM FinancialTransactionJpaEntity t WHERE t.companyId = :companyId "
            + "AND (:type IS NULL OR t.type = :type) "
            + "AND (:status IS NULL OR t.status = :status) "
            + "AND (:overdueBefore IS NULL OR t.dueDate < :overdueBefore) "
            + "AND (:from IS NULL OR t.dueDate >= :from) "
            + "AND (:to IS NULL OR t.dueDate <= :to) "
            + "AND (:paymentMethod IS NULL OR t.paymentMethod = :paymentMethod) "
            + "AND (:category IS NULL OR t.category = :category) "
            + "AND (:professionalUserId IS NULL OR t.professionalUserId = :professionalUserId) "
            + "AND (:contactId IS NULL OR t.contactId = :contactId) "
            + "AND (:pattern IS NULL OR LOWER(t.description) LIKE :pattern)")
    Page<FinancialTransactionJpaEntity> search(@Param("companyId") UUID companyId,
                                               @Param("type") String type,
                                               @Param("status") String status,
                                               @Param("overdueBefore") LocalDate overdueBefore,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to,
                                               @Param("paymentMethod") String paymentMethod,
                                               @Param("category") String category,
                                               @Param("professionalUserId") UUID professionalUserId,
                                               @Param("contactId") UUID contactId,
                                               @Param("pattern") String pattern,
                                               Pageable pageable);

    @Query("SELECT c.id, c.firstName, c.lastName FROM ContactJpaEntity c WHERE c.id IN :ids")
    List<Object[]> findContactNames(@Param("ids") Collection<UUID> ids);

    @Query("SELECT u.id, u.name FROM UserJpaEntity u WHERE u.id IN :ids")
    List<Object[]> findUserNames(@Param("ids") Collection<UUID> ids);

    @Query("SELECT i.id, i.name FROM CatalogItemJpaEntity i WHERE i.id IN :ids")
    List<Object[]> findCatalogItemNames(@Param("ids") Collection<UUID> ids);
}
