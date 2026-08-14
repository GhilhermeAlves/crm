package com.becommerce.crm.infrastructure.activity.persistence;

import com.becommerce.crm.application.activity.port.output.ActivityRepository;
import com.becommerce.crm.domain.activity.Activity;
import com.becommerce.crm.domain.activity.ActivityType;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ActivityRepositoryImpl implements ActivityRepository {

    private final ActivityJpaRepository jpaRepository;

    public ActivityRepositoryImpl(ActivityJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Activity save(Activity activity) {
        return toDomain(jpaRepository.save(toEntity(activity)));
    }

    @Override
    public Optional<Activity> findById(UUID id) {
        return jpaRepository.findById(id).map(ActivityRepositoryImpl::toDomain);
    }

    @Override
    public List<Activity> findByCompanyId(UUID companyId) {
        return jpaRepository.findByCompanyId(companyId).stream()
                .map(ActivityRepositoryImpl::toDomain).toList();
    }

    @Override
    public List<Activity> findByContactId(UUID contactId) {
        return jpaRepository.findByContactId(contactId).stream()
                .map(ActivityRepositoryImpl::toDomain).toList();
    }

    @Override
    public List<Activity> findByOpportunityId(UUID opportunityId) {
        return jpaRepository.findByOpportunityId(opportunityId).stream()
                .map(ActivityRepositoryImpl::toDomain).toList();
    }

    @Override
    public List<Activity> findRecentByCompanyId(UUID companyId, int limit) {
        return jpaRepository.findByCompanyIdOrderByActivityAtDesc(companyId, PageRequest.of(0, limit))
                .stream().map(ActivityRepositoryImpl::toDomain).toList();
    }

    @Override
    public Optional<LocalDateTime> findLatestActivityAtByOpportunityId(UUID opportunityId) {
        return jpaRepository.findTopByOpportunityIdOrderByActivityAtDesc(opportunityId)
                .map(ActivityJpaEntity::getActivityAt);
    }

    @Override
    public void delete(Activity activity) {
        jpaRepository.deleteById(activity.getId());
    }

    private static ActivityJpaEntity toEntity(Activity a) {
        ActivityJpaEntity e = new ActivityJpaEntity();
        e.setId(a.getId());
        e.setCompanyId(a.getCompanyId());
        e.setContactId(a.getContactId());
        e.setOpportunityId(a.getOpportunityId());
        e.setType(a.getType() != null ? a.getType().name() : null);
        e.setSubject(a.getSubject());
        e.setDescription(a.getDescription());
        e.setActivityAt(a.getActivityAt());
        e.setCreatedBy(a.getCreatedBy());
        e.setCreatedAt(a.getCreatedAt());
        e.setUpdatedAt(a.getUpdatedAt());
        return e;
    }

    private static Activity toDomain(ActivityJpaEntity e) {
        return Activity.reconstitute(e.getId(), e.getCompanyId(), e.getContactId(), e.getOpportunityId(),
                e.getType() != null ? ActivityType.valueOf(e.getType()) : null, e.getSubject(),
                e.getDescription(), e.getActivityAt(), e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }
}