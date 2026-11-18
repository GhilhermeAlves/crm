package com.becommerce.crm.finance.cardfee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Taxa da maquininha da clínica para um número de parcelas. */
@Entity
@Table(name = "card_fee_rates")
public class CardFeeRateJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "installments")
    private short installments;

    @Column(name = "rate_percent")
    private BigDecimal ratePercent;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected CardFeeRateJpaEntity() {
    }

    public CardFeeRateJpaEntity(UUID companyId, int installments, BigDecimal ratePercent) {
        this.id = UUID.randomUUID();
        this.companyId = companyId;
        this.installments = (short) installments;
        this.ratePercent = ratePercent;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public int getInstallments() {
        return installments;
    }

    public BigDecimal getRatePercent() {
        return ratePercent;
    }
}
