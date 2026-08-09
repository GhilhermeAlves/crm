package com.becommerce.crm.infrastructure.membership.persistence;

import com.becommerce.crm.application.membership.port.output.MemberProjection;
import com.becommerce.crm.application.membership.port.output.MembershipProjection;
import com.becommerce.crm.application.membership.port.output.MembershipRepository;
import com.becommerce.crm.domain.membership.Membership;
import com.becommerce.crm.domain.membership.MembershipStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MembershipRepositoryImpl implements MembershipRepository {

    private final SpringDataMembershipRepository repository;

    public MembershipRepositoryImpl(SpringDataMembershipRepository repository) {
        this.repository = repository;
    }

    @Override
    public Membership save(Membership membership) {
        MembershipJpaEntity entity = new MembershipJpaEntity();
        entity.setId(membership.getId());
        entity.setUserId(membership.getUserId());
        entity.setCompanyId(membership.getCompanyId());
        entity.setRole(membership.getRole());
        entity.setStatus(membership.getStatus().name());
        entity.setInvitedBy(membership.getInvitedBy());
        entity.setJoinedAt(membership.getJoinedAt());
        entity.setCreatedAt(membership.getCreatedAt());
        entity.setUpdatedAt(membership.getUpdatedAt());

        MembershipJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Membership> findByUserIdAndCompanyId(UUID userId, UUID companyId) {
        return repository.findFirstByUserIdAndCompanyIdOrderByJoinedAt(userId, companyId)
                .map(this::toDomain);
    }

    @Override
    public Optional<Membership> findActiveByUserIdAndCompanyId(UUID userId, UUID companyId) {
        return repository.findFirstByUserIdAndCompanyIdAndStatusOrderByJoinedAt(
                        userId, companyId, MembershipStatus.ACTIVE.name())
                .map(this::toDomain);
    }

    @Override
    public List<Membership> findByUserId(UUID userId) {
        return repository.findByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<String> findMembershipRoleByUserIdAndCompanyId(UUID userId, UUID companyId) {
        return repository.findMembershipRoleByUserIdAndCompanyId(userId, companyId);
    }

    @Override
    public List<MemberProjection> findActiveMembersByCompanyId(UUID companyId) {
        return repository.findActiveMembersByCompanyId(companyId);
    }

    @Override
    public List<MembershipProjection> findMembershipsByUserId(UUID userId) {
        return repository.findMembershipsByUserId(userId);
    }

    @Override
    public long countActiveByCompanyId(UUID companyId) {
        return repository.countByCompanyIdAndStatus(companyId, MembershipStatus.ACTIVE.name());
    }

    @Override
    public long countActiveAdminByCompanyId(UUID companyId) {
        return repository.countActiveAdminsByCompanyId(companyId);
    }

    @Override
    public boolean existsActiveByUserIdAndCompanyId(UUID userId, UUID companyId) {
        return repository.existsByUserIdAndCompanyIdAndStatus(
                userId, companyId, MembershipStatus.ACTIVE.name());
    }

    private Membership toDomain(MembershipJpaEntity entity) {
        Membership membership = new Membership();
        membership.setId(entity.getId());
        membership.setUserId(entity.getUserId());
        membership.setCompanyId(entity.getCompanyId());
        membership.setRole(entity.getRole());
        membership.setStatus(MembershipStatus.valueOf(entity.getStatus()));
        membership.setInvitedBy(entity.getInvitedBy());
        membership.setJoinedAt(entity.getJoinedAt());
        membership.setCreatedAt(entity.getCreatedAt());
        membership.setUpdatedAt(entity.getUpdatedAt());
        return membership;
    }
}
