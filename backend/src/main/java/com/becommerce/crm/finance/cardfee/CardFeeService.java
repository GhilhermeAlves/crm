package com.becommerce.crm.finance.cardfee;

import com.becommerce.crm.finance.cardfee.CardFeeDtos.RateItem;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Taxas da maquininha da clínica. Fixa a empresa no TenantContext para a RLS. */
@Service
public class CardFeeService {

    private final CardFeeRateJpaRepository repository;

    public CardFeeService(CardFeeRateJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<RateItem> list(UUID companyId) {
        try {
            TenantContext.setCompanyId(companyId);
            return repository.findByCompanyIdOrderByInstallmentsAsc(companyId).stream()
                    .map(r -> new RateItem(r.getInstallments(), r.getRatePercent()))
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    /** Substitui as 12 taxas da clínica de uma vez. */
    @Transactional
    public List<RateItem> save(UUID companyId, List<RateItem> rates) {
        List<RateItem> valid = CardFeeDtos.validate(rates);
        try {
            TenantContext.setCompanyId(companyId);
            repository.deleteAllOfCompany(companyId);
            repository.saveAll(valid.stream()
                    .map(r -> new CardFeeRateJpaEntity(companyId, r.installments(), r.ratePercent()))
                    .toList());
            return valid;
        } finally {
            TenantContext.clear();
        }
    }
}
