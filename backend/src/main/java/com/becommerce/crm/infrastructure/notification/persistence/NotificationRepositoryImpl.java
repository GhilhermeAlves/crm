package com.becommerce.crm.infrastructure.notification.persistence;

import com.becommerce.crm.application.notification.port.output.NotificationRepository;
import com.becommerce.crm.domain.notification.Notification;
import com.becommerce.crm.domain.notification.NotificationType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationJpaRepository jpaRepository;

    public NotificationRepositoryImpl(NotificationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Notification save(Notification notification) {
        return toDomain(jpaRepository.save(toEntity(notification)));
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return jpaRepository.findById(id).map(NotificationRepositoryImpl::toDomain);
    }

    @Override
    public List<Notification> findByCompanyIdAndUserId(UUID companyId, UUID userId) {
        return jpaRepository.findByCompanyIdAndUserIdOrderByCreatedAtDesc(companyId, userId).stream()
                .map(NotificationRepositoryImpl::toDomain).toList();
    }

    @Override
    public long countUnreadByCompanyIdAndUserId(UUID companyId, UUID userId) {
        return jpaRepository.countByCompanyIdAndUserIdAndReadAtIsNull(companyId, userId);
    }

    @Override
    public void markAllRead(UUID companyId, UUID userId) {
        jpaRepository.markAllRead(companyId, userId);
    }

    private static NotificationJpaEntity toEntity(Notification n) {
        NotificationJpaEntity e = new NotificationJpaEntity();
        e.setId(n.getId());
        e.setCompanyId(n.getCompanyId());
        e.setUserId(n.getUserId());
        e.setType(n.getType() != null ? n.getType().name() : null);
        e.setTitle(n.getTitle());
        e.setBody(n.getBody());
        e.setMetadata(n.getMetadata());
        e.setReadAt(n.getReadAt());
        e.setCreatedBy(n.getCreatedBy());
        e.setCreatedAt(n.getCreatedAt());
        return e;
    }

    private static Notification toDomain(NotificationJpaEntity e) {
        return Notification.reconstitute(e.getId(), e.getCompanyId(), e.getUserId(),
                e.getType() != null ? NotificationType.valueOf(e.getType()) : null,
                e.getTitle(), e.getBody(), e.getMetadata(), e.getReadAt(), e.getCreatedBy(), e.getCreatedAt());
    }
}