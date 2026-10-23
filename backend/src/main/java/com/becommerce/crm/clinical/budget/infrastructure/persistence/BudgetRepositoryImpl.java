package com.becommerce.crm.clinical.budget.infrastructure.persistence;

import com.becommerce.crm.clinical.budget.application.port.out.BudgetRepository;
import com.becommerce.crm.clinical.budget.domain.Budget;
import com.becommerce.crm.clinical.budget.domain.BudgetItem;
import com.becommerce.crm.clinical.budget.domain.BudgetStatus;
import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class BudgetRepositoryImpl implements BudgetRepository {

    private final BudgetJpaRepository jpaRepository;

    public BudgetRepositoryImpl(BudgetJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Budget save(Budget budget) {
        return toDomain(jpaRepository.save(toEntity(budget)));
    }

    @Override
    public Optional<Budget> findById(UUID id) {
        return jpaRepository.findById(id).map(BudgetRepositoryImpl::toDomain);
    }

    @Override
    public Optional<Budget> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(BudgetRepositoryImpl::toDomain);
    }

    @Override
    public List<Budget> findByCompanyIdAndContactIdOrderByUpdatedAtDesc(UUID companyId, UUID contactId) {
        return jpaRepository.findByCompanyIdAndContactIdOrderByUpdatedAtDesc(companyId, contactId).stream()
                .map(BudgetRepositoryImpl::toDomain).toList();
    }

    @Override
    public void delete(Budget budget) {
        jpaRepository.deleteById(budget.getId());
    }

    @Override
    public Map<UUID, Budget> findActiveByProcedureIds(UUID companyId, Collection<UUID> procedureIds) {
        Map<UUID, Budget> result = new HashMap<>();
        if (procedureIds == null || procedureIds.isEmpty()) {
            return result;
        }
        Set<UUID> wanted = Set.copyOf(procedureIds);
        for (BudgetJpaEntity entity : jpaRepository.findActiveByProcedureIds(companyId, wanted)) {
            Budget budget = toDomain(entity);
            for (BudgetItem item : budget.getItems()) {
                if (wanted.contains(item.procedureId())) {
                    result.put(item.procedureId(), budget);
                }
            }
        }
        return result;
    }

    private static BudgetJpaEntity toEntity(Budget b) {
        BudgetJpaEntity e = new BudgetJpaEntity();
        e.setId(b.getId());
        e.setCompanyId(b.getCompanyId());
        e.setContactId(b.getContactId());
        e.setTitle(b.getTitle());
        e.setStatus(b.getStatus().name());
        e.setDiscountCents(b.getDiscountCents());
        e.setNotes(b.getNotes());
        e.setPaymentMethod(b.getPaymentMethod() == null ? null : b.getPaymentMethod().name());
        e.setInstallments(b.getInstallments());
        e.setFirstDueDate(b.getFirstDueDate());
        e.setApprovedAt(b.getApprovedAt());
        e.setApprovedBy(b.getApprovedBy());
        e.setLostAt(b.getLostAt());
        e.setCreatedBy(b.getCreatedBy());
        e.setUpdatedBy(b.getUpdatedBy());
        e.setCreatedAt(b.getCreatedAt());
        e.setUpdatedAt(b.getUpdatedAt());
        List<BudgetItemJpaEntity> items = new ArrayList<>();
        for (BudgetItem item : b.getItems()) {
            BudgetItemJpaEntity ie = new BudgetItemJpaEntity();
            ie.setId(item.id());
            ie.setCompanyId(b.getCompanyId());
            ie.setProcedureId(item.procedureId());
            ie.setPriceCents(item.priceCents());
            ie.setPosition(item.position());
            ie.setBudget(e);
            items.add(ie);
        }
        e.setItems(items);
        e.setTransactions(b.getTransactionIds().stream()
                .map(id -> new BudgetTransactionLink(b.getCompanyId(), id))
                .collect(Collectors.toCollection(HashSet::new)));
        return e;
    }

    private static Budget toDomain(BudgetJpaEntity e) {
        List<BudgetItem> items = e.getItems().stream()
                .sorted(Comparator.comparingInt(BudgetItemJpaEntity::getPosition))
                .map(i -> new BudgetItem(i.getId(), i.getProcedureId(), i.getPriceCents(), i.getPosition()))
                .toList();
        List<UUID> transactionIds = e.getTransactions().stream()
                .map(BudgetTransactionLink::getTransactionId).toList();
        return Budget.reconstitute(e.getId(), e.getCompanyId(), e.getContactId(), e.getTitle(),
                BudgetStatus.valueOf(e.getStatus()), e.getDiscountCents(), e.getNotes(),
                e.getPaymentMethod() == null ? null : PaymentMethod.valueOf(e.getPaymentMethod()),
                e.getInstallments(), e.getFirstDueDate(), e.getApprovedAt(), e.getApprovedBy(), e.getLostAt(),
                items, transactionIds, e.getCreatedBy(), e.getUpdatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
