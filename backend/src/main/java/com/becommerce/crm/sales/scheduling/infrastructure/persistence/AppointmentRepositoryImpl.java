package com.becommerce.crm.sales.scheduling.infrastructure.persistence;

import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentRepository;
import com.becommerce.crm.sales.scheduling.domain.*;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AppointmentRepositoryImpl implements AppointmentRepository {

    private final AppointmentJpaRepository jpa;

    public AppointmentRepositoryImpl(AppointmentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Appointment save(Appointment appointment) {
        return toDomain(jpa.save(toEntity(appointment)));
    }

    @Override
    public Optional<Appointment> findById(UUID id) {
        return jpa.findById(id).map(AppointmentRepositoryImpl::toDomain);
    }

    @Override
    public List<Appointment> findByCompanyIdAndRange(UUID companyId, Instant from, Instant to, List<UUID> hostIds) {
        return jpa.findByCompanyIdAndRange(companyId, from, to, hostIds == null || hostIds.isEmpty() ? null : hostIds)
                .stream().map(AppointmentRepositoryImpl::toDomain).toList();
    }

    @Override
    public List<Appointment> findNonCanceledByHostAndRange(UUID hostId, Instant from, Instant to) {
        return jpa.findNonCanceledByHostAndRange(hostId, from, to)
                .stream().map(AppointmentRepositoryImpl::toDomain).toList();
    }

    @Override
    public Optional<Appointment> findByPublicToken(String publicToken) {
        return jpa.findByPublicToken(publicToken).map(AppointmentRepositoryImpl::toDomain);
    }

    @Override
    public List<Appointment> findUpcomingByContact(UUID companyId, UUID contactId, Instant from, int limit) {
        return jpa.findUpcomingByContact(companyId, contactId, from,
                        org.springframework.data.domain.PageRequest.of(0, Math.max(1, limit)))
                .stream().map(AppointmentRepositoryImpl::toDomain).toList();
    }

    @Override
    public long countByHostIdInWeek(UUID hostId, Instant weekStart, Instant weekEnd) {
        return jpa.countByHostIdInWeek(hostId, weekStart, weekEnd);
    }

    @Override
    public void delete(Appointment appointment) {
        jpa.deleteById(appointment.getId());
    }

    private static AppointmentJpaEntity toEntity(Appointment a) {
        AppointmentJpaEntity e = new AppointmentJpaEntity();
        e.setId(a.getId());
        e.setCompanyId(a.getCompanyId());
        e.setAppointmentTypeId(a.getAppointmentTypeId());
        e.setHostId(a.getHostId());
        e.setContactId(a.getContactId());
        e.setOpportunityId(a.getOpportunityId());
        e.setTitle(a.getTitle());
        e.setStartAt(a.getStartAt());
        e.setEndAt(a.getEndAt());
        e.setStatus(a.getStatus() != null ? a.getStatus().name() : null);
        e.setSource(a.getSource() != null ? a.getSource().name() : null);
        e.setLocationKind(a.getLocationKind() != null ? a.getLocationKind().name() : null);
        e.setLocationDetail(a.getLocationDetail());
        e.setMeetingUrl(a.getMeetingUrl());
        e.setNotes(a.getNotes());
        e.setCancelReason(a.getCancelReason());
        e.setPublicToken(a.getPublicToken());
        e.setGoogleEventId(a.getGoogleEventId());
        e.setCreatedBy(a.getCreatedBy());
        e.setCreatedAt(a.getCreatedAt());
        e.setUpdatedAt(a.getUpdatedAt());
        return e;
    }

    private static Appointment toDomain(AppointmentJpaEntity e) {
        return Appointment.reconstitute(e.getId(), e.getCompanyId(), e.getAppointmentTypeId(),
                e.getHostId(), e.getContactId(), e.getOpportunityId(), e.getTitle(),
                e.getStartAt(), e.getEndAt(),
                e.getStatus() != null ? AppointmentStatus.valueOf(e.getStatus()) : null,
                e.getSource() != null ? AppointmentSource.valueOf(e.getSource()) : null,
                e.getLocationKind() != null ? LocationKind.valueOf(e.getLocationKind()) : null,
                e.getLocationDetail(), e.getMeetingUrl(), e.getNotes(), e.getCancelReason(),
                e.getPublicToken(), e.getGoogleEventId(), e.getCreatedBy(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
