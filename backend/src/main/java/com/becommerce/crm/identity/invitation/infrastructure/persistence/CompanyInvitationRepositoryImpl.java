package com.becommerce.crm.identity.invitation.infrastructure.persistence;

import com.becommerce.crm.identity.invitation.application.port.output.CompanyInvitationRepository;
import com.becommerce.crm.identity.invitation.domain.CompanyInvitation;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CompanyInvitationRepositoryImpl implements CompanyInvitationRepository {

    private final CompanyInvitationJpaRepository jpaRepository;

    public CompanyInvitationRepositoryImpl(CompanyInvitationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CompanyInvitation save(CompanyInvitation invitation) {
        // saveAndFlush: o UPDATE do aceite depende do token do convite no RLS
        // (app.invitation_token_hash, local à transação), que é limpo ao fim de
        // withToken, ANTES do commit. Sem o flush aqui, o UPDATE sairia no commit
        // sem o token, afetaria 0 linhas e o cadastro falharia (stale/500).
        return toDomain(jpaRepository.saveAndFlush(toEntity(invitation)));
    }

    @Override
    public Optional<CompanyInvitation> findById(UUID id) {
        return jpaRepository.findById(id).map(CompanyInvitationRepositoryImpl::toDomain);
    }

    @Override
    public Optional<CompanyInvitation> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(CompanyInvitationRepositoryImpl::toDomain);
    }

    @Override
    public Optional<CompanyInvitation> findByTokenHashForUpdate(String tokenHash) {
        return jpaRepository.findByTokenHashForUpdate(tokenHash).map(CompanyInvitationRepositoryImpl::toDomain);
    }

    @Override
    public List<CompanyInvitation> findByInviterCompanyId(UUID inviterCompanyId) {
        return jpaRepository.findByInviterCompanyIdOrderByCreatedAtDesc(inviterCompanyId).stream()
                .map(CompanyInvitationRepositoryImpl::toDomain).toList();
    }

    private static JpaCompanyInvitation toEntity(CompanyInvitation i) {
        JpaCompanyInvitation e = new JpaCompanyInvitation();
        e.setId(i.getId());
        e.setInviterCompanyId(i.getInviterCompanyId());
        e.setEmail(i.getEmail());
        e.setInviteeName(i.getInviteeName());
        e.setSegment(i.getSegment());
        e.setEnabledModules(i.getEnabledModules());
        e.setPlan(i.getPlan());
        e.setMaxUsers(i.getMaxUsers());
        e.setCompanyPrefill(i.getCompanyPrefill());
        e.setTokenHash(i.getTokenHash());
        e.setInvitedBy(i.getInvitedBy());
        e.setStatus(i.getStatus());
        e.setCompanyId(i.getCompanyId());
        e.setExpiresAt(i.getExpiresAt());
        e.setCreatedAt(i.getCreatedAt());
        e.setUpdatedAt(i.getUpdatedAt());
        return e;
    }

    private static CompanyInvitation toDomain(JpaCompanyInvitation e) {
        CompanyInvitation i = new CompanyInvitation();
        i.setId(e.getId());
        i.setInviterCompanyId(e.getInviterCompanyId());
        i.setEmail(e.getEmail());
        i.setInviteeName(e.getInviteeName());
        i.setSegment(e.getSegment());
        i.setEnabledModules(e.getEnabledModules());
        i.setPlan(e.getPlan());
        i.setMaxUsers(e.getMaxUsers());
        i.setCompanyPrefill(e.getCompanyPrefill());
        i.setTokenHash(e.getTokenHash());
        i.setInvitedBy(e.getInvitedBy());
        i.setStatus(e.getStatus());
        i.setCompanyId(e.getCompanyId());
        i.setExpiresAt(e.getExpiresAt());
        i.setCreatedAt(e.getCreatedAt());
        i.setUpdatedAt(e.getUpdatedAt());
        return i;
    }
}
