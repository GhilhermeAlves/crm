package com.becommerce.crm.finance.cardfee;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Modelo de taxa de uma maquininha do mercado (global, mantido pelo dono do CRM). */
@Entity
@Table(name = "card_fee_presets")
public class CardFeePresetJpaEntity {

    @Id
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "estimated")
    private boolean estimated;

    @Column(name = "checked_at")
    private LocalDate checkedAt;

    @Column(name = "source_note")
    private String sourceNote;

    @Column(name = "active")
    private boolean active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "card_fee_preset_rates", joinColumns = @JoinColumn(name = "preset_id"))
    @OrderBy("installments ASC")
    private List<Rate> rates = new ArrayList<>();

    protected CardFeePresetJpaEntity() {
    }

    public CardFeePresetJpaEntity(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String name, boolean estimated, LocalDate checkedAt, String sourceNote,
                       boolean active, List<Rate> rates) {
        this.name = name;
        this.estimated = estimated;
        this.checkedAt = checkedAt;
        this.sourceNote = sourceNote;
        this.active = active;
        this.rates.clear();
        this.rates.addAll(rates);
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public boolean isEstimated() { return estimated; }
    public LocalDate getCheckedAt() { return checkedAt; }
    public String getSourceNote() { return sourceNote; }
    public boolean isActive() { return active; }
    public List<Rate> getRates() { return rates; }

    @Embeddable
    public static class Rate {
        @Column(name = "installments")
        private short installments;

        @Column(name = "rate_percent")
        private BigDecimal ratePercent;

        protected Rate() {
        }

        public Rate(int installments, BigDecimal ratePercent) {
            this.installments = (short) installments;
            this.ratePercent = ratePercent;
        }

        public int getInstallments() { return installments; }
        public BigDecimal getRatePercent() { return ratePercent; }
    }
}
