package com.becommerce.crm.sales.scheduling.domain;

import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingValidationException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AppointmentType {

    private final UUID id;
    private final UUID companyId;
    private String name;
    private String slug;
    private String description;
    private int durationMinutes;
    private int bufferBeforeMinutes;
    private int bufferAfterMinutes;
    private int minNoticeHours;
    private int maxDaysAhead;
    private int slotIntervalMinutes;
    private String color;
    private LocationKind locationKind;
    private String locationDetail;
    private AssignmentMode assignmentMode;
    private boolean publicBookingEnabled;
    private boolean active;
    private final List<UUID> hostIds;
    private final Instant createdAt;
    private Instant updatedAt;

    private AppointmentType(UUID id, UUID companyId, String name, String slug, String description,
                            int durationMinutes, int bufferBeforeMinutes, int bufferAfterMinutes,
                            int minNoticeHours, int maxDaysAhead, int slotIntervalMinutes,
                            String color, LocationKind locationKind, String locationDetail,
                            AssignmentMode assignmentMode, boolean publicBookingEnabled, boolean active,
                            List<UUID> hostIds, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.bufferBeforeMinutes = bufferBeforeMinutes;
        this.bufferAfterMinutes = bufferAfterMinutes;
        this.minNoticeHours = minNoticeHours;
        this.maxDaysAhead = maxDaysAhead;
        this.slotIntervalMinutes = slotIntervalMinutes;
        this.color = color;
        this.locationKind = locationKind;
        this.locationDetail = locationDetail;
        this.assignmentMode = assignmentMode;
        this.publicBookingEnabled = publicBookingEnabled;
        this.active = active;
        this.hostIds = hostIds;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AppointmentType create(UUID companyId, String name, String slug, String description,
                                         int durationMinutes, int bufferBeforeMinutes, int bufferAfterMinutes,
                                         int minNoticeHours, int maxDaysAhead, int slotIntervalMinutes,
                                         String color, LocationKind locationKind, String locationDetail,
                                         AssignmentMode assignmentMode, List<UUID> hostIds) {
        validateName(name);
        validateSlug(slug);
        validateDuration(durationMinutes);
        validateBuffers(bufferBeforeMinutes, bufferAfterMinutes);
        validateSlotInterval(slotIntervalMinutes);
        Instant now = Instant.now();
        return new AppointmentType(UUID.randomUUID(), companyId, name.trim(), slug.trim().toLowerCase(),
                description, durationMinutes, bufferBeforeMinutes, bufferAfterMinutes,
                minNoticeHours, maxDaysAhead, slotIntervalMinutes, color,
                locationKind != null ? locationKind : LocationKind.GOOGLE_MEET, locationDetail,
                assignmentMode != null ? assignmentMode : AssignmentMode.ROUND_ROBIN,
                false, true, new ArrayList<>(hostIds != null ? hostIds : List.of()), now, now);
    }

    public static AppointmentType reconstitute(UUID id, UUID companyId, String name, String slug,
                                               String description, int durationMinutes,
                                               int bufferBeforeMinutes, int bufferAfterMinutes,
                                               int minNoticeHours, int maxDaysAhead, int slotIntervalMinutes,
                                               String color, LocationKind locationKind, String locationDetail,
                                               AssignmentMode assignmentMode, boolean publicBookingEnabled,
                                               boolean active, List<UUID> hostIds,
                                               Instant createdAt, Instant updatedAt) {
        return new AppointmentType(id, companyId, name, slug, description, durationMinutes,
                bufferBeforeMinutes, bufferAfterMinutes, minNoticeHours, maxDaysAhead,
                slotIntervalMinutes, color, locationKind, locationDetail, assignmentMode,
                publicBookingEnabled, active, new ArrayList<>(hostIds != null ? hostIds : List.of()),
                createdAt, updatedAt);
    }

    public void update(String name, String slug, String description, int durationMinutes,
                       int bufferBeforeMinutes, int bufferAfterMinutes, int minNoticeHours,
                       int maxDaysAhead, int slotIntervalMinutes, String color,
                       LocationKind locationKind, String locationDetail,
                       AssignmentMode assignmentMode, boolean publicBookingEnabled,
                       boolean active, List<UUID> hostIds) {
        validateName(name);
        validateSlug(slug);
        validateDuration(durationMinutes);
        validateBuffers(bufferBeforeMinutes, bufferAfterMinutes);
        validateSlotInterval(slotIntervalMinutes);
        this.name = name.trim();
        this.slug = slug.trim().toLowerCase();
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.bufferBeforeMinutes = bufferBeforeMinutes;
        this.bufferAfterMinutes = bufferAfterMinutes;
        this.minNoticeHours = minNoticeHours;
        this.maxDaysAhead = maxDaysAhead;
        this.slotIntervalMinutes = slotIntervalMinutes;
        this.color = color;
        this.locationKind = locationKind != null ? locationKind : LocationKind.GOOGLE_MEET;
        this.locationDetail = locationDetail;
        this.assignmentMode = assignmentMode != null ? assignmentMode : AssignmentMode.ROUND_ROBIN;
        this.publicBookingEnabled = publicBookingEnabled;
        this.active = active;
        this.hostIds.clear();
        if (hostIds != null) {
            this.hostIds.addAll(hostIds);
        }
        touch();
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new SchedulingValidationException("O nome do tipo de agendamento é obrigatório.");
        }
        if (name.trim().length() > 120) {
            throw new SchedulingValidationException("O nome deve ter no máximo 120 caracteres.");
        }
    }

    private static void validateSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new SchedulingValidationException("O slug é obrigatório.");
        }
        if (slug.trim().length() > 80) {
            throw new SchedulingValidationException("O slug deve ter no máximo 80 caracteres.");
        }
    }

    private static void validateDuration(int minutes) {
        if (minutes < 5 || minutes > 480) {
            throw new SchedulingValidationException("A duração deve ser entre 5 e 480 minutos.");
        }
    }

    private static void validateBuffers(int before, int after) {
        if (before < 0 || before > 240 || after < 0 || after > 240) {
            throw new SchedulingValidationException("Os buffers devem ser entre 0 e 240 minutos.");
        }
    }

    private static void validateSlotInterval(int interval) {
        if (interval != 5 && interval != 10 && interval != 15 && interval != 20
                && interval != 30 && interval != 60) {
            throw new SchedulingValidationException("O intervalo de slots deve ser 5, 10, 15, 20, 30 ou 60 minutos.");
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public int getDurationMinutes() { return durationMinutes; }
    public int getBufferBeforeMinutes() { return bufferBeforeMinutes; }
    public int getBufferAfterMinutes() { return bufferAfterMinutes; }
    public int getMinNoticeHours() { return minNoticeHours; }
    public int getMaxDaysAhead() { return maxDaysAhead; }
    public int getSlotIntervalMinutes() { return slotIntervalMinutes; }
    public String getColor() { return color; }
    public LocationKind getLocationKind() { return locationKind; }
    public String getLocationDetail() { return locationDetail; }
    public AssignmentMode getAssignmentMode() { return assignmentMode; }
    public boolean isPublicBookingEnabled() { return publicBookingEnabled; }
    public boolean isActive() { return active; }
    public List<UUID> getHostIds() { return List.copyOf(hostIds); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
