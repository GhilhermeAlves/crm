package com.becommerce.crm.finance.transaction.infrastructure.persistence;

import com.becommerce.crm.finance.transaction.application.port.output.FinancialTransactionQuery;
import com.becommerce.crm.finance.transaction.application.port.output.FinancialTransactionRepository;
import com.becommerce.crm.finance.transaction.domain.FinancialTransaction;
import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class FinancialTransactionRepositoryImpl implements FinancialTransactionRepository {

    private final FinancialTransactionJpaRepository jpaRepository;

    public FinancialTransactionRepositoryImpl(FinancialTransactionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public FinancialTransaction save(FinancialTransaction transaction) {
        return toDomain(jpaRepository.save(toEntity(transaction)));
    }

    @Override
    public List<FinancialTransaction> saveAll(List<FinancialTransaction> transactions) {
        List<FinancialTransactionJpaEntity> entities = transactions.stream()
                .map(FinancialTransactionRepositoryImpl::toEntity)
                .toList();
        return jpaRepository.saveAll(entities).stream()
                .map(FinancialTransactionRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public Optional<FinancialTransaction> findById(UUID id) {
        return jpaRepository.findById(id).map(FinancialTransactionRepositoryImpl::toDomain);
    }

    @Override
    public void delete(FinancialTransaction transaction) {
        jpaRepository.deleteById(transaction.getId());
    }

    @Override
    public PageResult search(UUID companyId, FinancialTransactionQuery query, int page, int pageSize) {
        Page<FinancialTransactionJpaEntity> result = jpaRepository.search(companyId,
                query.type() == null ? null : query.type().name(),
                query.status(), query.overdueBefore(), query.from(), query.to(),
                query.paymentMethod() == null ? null : query.paymentMethod().name(),
                query.category() == null ? null : query.category().name(),
                query.professionalUserId(), query.contactId(), query.pattern(),
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "dueDate")));
        return new PageResult(result.getContent().stream().map(FinancialTransactionRepositoryImpl::toDomain).toList(),
                result.getTotalElements());
    }

    @Override
    public Map<UUID, TransactionReference> findReferences(Collection<UUID> transactionIds) {
        if (transactionIds == null || transactionIds.isEmpty()) {
            return Map.of();
        }
        List<FinancialTransactionJpaEntity> rows = jpaRepository.findAllById(transactionIds);
        Set<UUID> contactIds = new HashSet<>();
        Set<UUID> userIds = new HashSet<>();
        Set<UUID> catalogItemIds = new HashSet<>();
        for (FinancialTransactionJpaEntity row : rows) {
            addIfPresent(contactIds, row.getContactId());
            addIfPresent(userIds, row.getProfessionalUserId());
            addIfPresent(catalogItemIds, row.getCatalogItemId());
        }
        Map<UUID, String> contactNames = lookup(contactIds, "contact");
        Map<UUID, String> userNames = lookup(userIds, "user");
        Map<UUID, String> catalogNames = lookup(catalogItemIds, "catalog");
        Map<UUID, TransactionReference> references = new HashMap<>();
        for (FinancialTransactionJpaEntity row : rows) {
            references.put(row.getId(), new TransactionReference(
                    contactNames.get(row.getContactId()),
                    userNames.get(row.getProfessionalUserId()),
                    catalogNames.get(row.getCatalogItemId())));
        }
        return references;
    }

    private Map<UUID, String> lookup(Set<UUID> ids, String kind) {
        Map<UUID, String> names = new HashMap<>();
        if (ids.isEmpty()) {
            return names;
        }
        switch (kind) {
            case "contact" -> {
                for (Object[] row : jpaRepository.findContactNames(ids)) {
                    names.put((UUID) row[0], joinName((String) row[1], (String) row[2]));
                }
            }
            case "user" -> {
                for (Object[] row : jpaRepository.findUserNames(ids)) {
                    names.put((UUID) row[0], (String) row[1]);
                }
            }
            default -> {
                for (Object[] row : jpaRepository.findCatalogItemNames(ids)) {
                    names.put((UUID) row[0], (String) row[1]);
                }
            }
        }
        return names;
    }

    private static String joinName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? null : full;
    }

    private static void addIfPresent(Set<UUID> ids, UUID id) {
        if (id != null) {
            ids.add(id);
        }
    }

    static FinancialTransaction toDomain(FinancialTransactionJpaEntity e) {
        return FinancialTransaction.reconstitute(e.getId(), e.getCompanyId(),
                TransactionType.valueOf(e.getType()), e.getDescription(),
                TransactionCategory.valueOf(e.getCategory()), e.getAmountCents(), e.getFeeCents(),
                PaymentMethod.valueOf(e.getPaymentMethod()), e.getDueDate(), e.getPaidAt(),
                TransactionStatus.valueOf(e.getStatus()), e.getContactId(), e.getProfessionalUserId(),
                e.getCatalogItemId(), e.getInstallmentGroupId(), e.getInstallmentNumber(),
                e.getInstallmentTotal(), e.getNotes(), e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }

    static FinancialTransactionJpaEntity toEntity(FinancialTransaction t) {
        FinancialTransactionJpaEntity e = new FinancialTransactionJpaEntity();
        e.setId(t.getId());
        e.setCompanyId(t.getCompanyId());
        e.setType(t.getType().name());
        e.setDescription(t.getDescription());
        e.setCategory(t.getCategory().name());
        e.setAmountCents(t.getAmountCents());
        e.setFeeCents(t.getFeeCents());
        e.setPaymentMethod(t.getPaymentMethod().name());
        e.setDueDate(t.getDueDate());
        e.setPaidAt(t.getPaidAt());
        e.setStatus(t.getStatus().name());
        e.setContactId(t.getContactId());
        e.setProfessionalUserId(t.getProfessionalUserId());
        e.setCatalogItemId(t.getCatalogItemId());
        e.setInstallmentGroupId(t.getInstallmentGroupId());
        e.setInstallmentNumber(t.getInstallmentNumber());
        e.setInstallmentTotal(t.getInstallmentTotal());
        e.setNotes(t.getNotes());
        e.setCreatedBy(t.getCreatedBy());
        e.setCreatedAt(t.getCreatedAt());
        e.setUpdatedAt(t.getUpdatedAt());
        return e;
    }
}
