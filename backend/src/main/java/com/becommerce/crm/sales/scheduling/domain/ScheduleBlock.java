package com.becommerce.crm.sales.scheduling.domain;

import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingValidationException;

import java.time.Instant;
import java.util.UUID;

public class ScheduleBlock {

    private final UUID id;
    private final UUID companyId;
    private final UUID hostId;
    private Instant startAt;
    private Instant endAt;
    private String reason;
    private final BlockSource source;
    private final String externalId;
    private final UUID createdBy;
    private final Instant createdAt;

    private ScheduleBlock(UUID id, UUID companyId, UUID hostId, Instant startAt, Instant endAt,
                          String reason, BlockSource source, String externalId, UUID createdBy,
                          Instant createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.hostId = hostId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.reason = reason;
        this.source = source;
        this.externalId = externalId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public static ScheduleBlock create(UUID companyId, UUID hostId, Instant startAt, Instant endAt,
                                       String reason, UUID createdBy) {
        validateRange(startAt, endAt);
        return new ScheduleBlock(UUID.randomUUID(), companyId, hostId, startAt, endAt,
                reason, BlockSource.INTERNAL, null, createdBy, Instant.now());
    }

    public static ScheduleBlock reconstitute(UUID id, UUID companyId, UUID hostId, Instant startAt,
                                             Instant endAt, String reason, BlockSource source,
                                             String externalId, UUID createdBy, Instant createdAt) {
        return new ScheduleBlock(id, companyId, hostId, startAt, endAt, reason, source,
                externalId, createdBy, createdAt);
    }

    public void update(Instant startAt, Instant endAt, String reason) {
        if (source == BlockSource.GOOGLE) {
            throw new SchedulingValidationException("Bloqueios do Google Calendar não podem ser editados.");
        }
        validateRange(startAt, endAt);
        this.startAt = startAt;
        this.endAt = endAt;
        this.reason = reason;
    }

    private static void validateRange(Instant start, Instant end) {
        if (start == null || end == null) {
            throw new SchedulingValidationException("Data de início e fim são obrigatórias.");
        }
        if (!end.isAfter(start)) {
            throw new SchedulingValidationException("A data de fim deve ser posterior à de início.");
        }
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getHostId() { return hostId; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public String getReason() { return reason; }
    public BlockSource getSource() { return source; }
    public String getExternalId() { return externalId; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
