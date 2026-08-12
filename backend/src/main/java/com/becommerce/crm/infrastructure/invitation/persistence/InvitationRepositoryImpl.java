package com.becommerce.crm.infrastructure.invitation.persistence;

import com.becommerce.crm.application.invitation.port.output.InvitationRepository;
import com.becommerce.crm.domain.invitation.Invitation;
import com.becommerce.crm.domain.invitation.InvitationStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InvitationRepositoryImpl implements InvitationRepository {

    private final InvitationJpaRepository jpaRepository;

    public InvitationRepositoryImpl(InvitationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Invitation save(Invitation invitation) {
        return toDomain(jpaRepository.save(toEntity(invitation)));
    }

    @Override
    public Optional<Invitation> findById(UUID id) {
        return jpaRepository.findById(id).map(InvitationRepositoryImpl::toDomain);
    }

    @Override
    public Optional<Invitation> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(InvitationRepositoryImpl::toDomain);
    }

    @Override
    public List<Invitation> findByCompanyId(UUID companyId, InvitationStatus status) {
        List<JpaInvitation> entities = (status == null
                ? jpaRepository.findByCompanyId(companyId)
                : jpaRepository.findByCompanyIdAndStatus(companyId, status));
        return entities.stream().map(InvitationRepositoryImpl::toDomain).toList();
    }

    private static JpaInvitation toEntity(Invitation i) {
        JpaInvitation e = new JpaInvitation();
        e.setId(i.getId());
        e.setCompanyId(i.getCompanyId());
        e.setEmail(i.getEmail());
        e.setRole(i.getRole());
        e.setTokenHash(i.getTokenHash());
        e.setInvitedBy(i.getInvitedBy());
        e.setStatus(i.getStatus());
        e.setExpiresAt(i.getExpiresAt());
        e.setCreatedAt(i.getCreatedAt());
        e.setUpdatedAt(i.getUpdatedAt());
        return e;
    }

    private static Invitation toDomain(JpaInvitation e) {
        return new Invitation(e.getId(), e.getCompanyId(), e.getEmail(), e.getRole(),
                e.getTokenHash(), e.getInvitedBy(), e.getStatus(), e.getExpiresAt(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}