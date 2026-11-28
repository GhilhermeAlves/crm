package com.becommerce.crm.clinical.anamnesis.divergence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Divergência entre um dado respondido na anamnese e o cadastro (V163). */
@Entity
@Table(name = "person_profile_divergences")
public class ProfileDivergenceJpaEntity {

    @Id
    public UUID id;

    @Column(name = "company_id", nullable = false)
    public UUID companyId;

    @Column(name = "person_id", nullable = false)
    public UUID personId;

    @Column(name = "anamnesis_id")
    public UUID anamnesisId;

    @Column(nullable = false, length = 40)
    public String field;

    @Column(name = "current_value")
    public String currentValue;

    @Column(name = "proposed_value", nullable = false)
    public String proposedValue;

    @Column(length = 255)
    public String reason;

    @Column(nullable = false, length = 20)
    public String status = "PENDING";

    @Column(name = "resolved_by")
    public UUID resolvedBy;

    @Column(name = "resolved_at")
    public Instant resolvedAt;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}
