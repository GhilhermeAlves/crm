package com.becommerce.crm.sales.scheduling.infrastructure.persistence;

import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentTypeRepository;
import com.becommerce.crm.sales.scheduling.domain.AppointmentType;
import com.becommerce.crm.sales.scheduling.domain.AssignmentMode;
import com.becommerce.crm.sales.scheduling.domain.LocationKind;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AppointmentTypeRepositoryImpl implements AppointmentTypeRepository {

    private final AppointmentTypeJpaRepository jpa;
    private final AppointmentTypeHostJpaRepository hostJpa;

    public AppointmentTypeRepositoryImpl(AppointmentTypeJpaRepository jpa,
                                         AppointmentTypeHostJpaRepository hostJpa) {
        this.jpa = jpa;
        this.hostJpa = hostJpa;
    }

    @Override
    public AppointmentType save(AppointmentType type) {
        jpa.save(toEntity(type));
        hostJpa.deleteByAppointmentTypeId(type.getId());
        for (UUID hostId : type.getHostIds()) {
            AppointmentTypeHostJpaEntity host = new AppointmentTypeHostJpaEntity();
            host.setAppointmentTypeId(type.getId());
            host.setUserId(hostId);
            host.setCompanyId(type.getCompanyId());
            hostJpa.save(host);
        }
        return type;
    }

    @Override
    public Optional<AppointmentType> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<AppointmentType> findByCompanyIdAndSlug(UUID companyId, String slug) {
        return jpa.findByCompanyIdAndSlug(companyId, slug).map(this::toDomain);
    }

    @Override
    public List<AppointmentType> findByCompanyId(UUID companyId) {
        return jpa.findByCompanyId(companyId).stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(AppointmentType type) {
        hostJpa.deleteByAppointmentTypeId(type.getId());
        jpa.deleteById(type.getId());
    }

    private AppointmentTypeJpaEntity toEntity(AppointmentType t) {
        AppointmentTypeJpaEntity e = new AppointmentTypeJpaEntity();
        e.setId(t.getId());
        e.setCompanyId(t.getCompanyId());
        e.setName(t.getName());
        e.setSlug(t.getSlug());
        e.setDescription(t.getDescription());
        e.setDurationMinutes(t.getDurationMinutes());
        e.setBufferBeforeMinutes(t.getBufferBeforeMinutes());
        e.setBufferAfterMinutes(t.getBufferAfterMinutes());
        e.setMinNoticeHours(t.getMinNoticeHours());
        e.setMaxDaysAhead(t.getMaxDaysAhead());
        e.setSlotIntervalMinutes(t.getSlotIntervalMinutes());
        e.setColor(t.getColor());
        e.setLocationKind(t.getLocationKind() != null ? t.getLocationKind().name() : null);
        e.setLocationDetail(t.getLocationDetail());
        e.setAssignmentMode(t.getAssignmentMode() != null ? t.getAssignmentMode().name() : null);
        e.setPublicBookingEnabled(t.isPublicBookingEnabled());
        e.setActive(t.isActive());
        e.setCreatedAt(t.getCreatedAt());
        e.setUpdatedAt(t.getUpdatedAt());
        return e;
    }

    private AppointmentType toDomain(AppointmentTypeJpaEntity e) {
        List<UUID> hostIds = hostJpa.findByAppointmentTypeId(e.getId()).stream()
                .map(AppointmentTypeHostJpaEntity::getUserId).toList();
        return AppointmentType.reconstitute(e.getId(), e.getCompanyId(), e.getName(), e.getSlug(),
                e.getDescription(), e.getDurationMinutes(), e.getBufferBeforeMinutes(),
                e.getBufferAfterMinutes(), e.getMinNoticeHours(), e.getMaxDaysAhead(),
                e.getSlotIntervalMinutes(), e.getColor(),
                e.getLocationKind() != null ? LocationKind.valueOf(e.getLocationKind()) : null,
                e.getLocationDetail(),
                e.getAssignmentMode() != null ? AssignmentMode.valueOf(e.getAssignmentMode()) : null,
                e.isPublicBookingEnabled(), e.isActive(), hostIds, e.getCreatedAt(), e.getUpdatedAt());
    }
}
