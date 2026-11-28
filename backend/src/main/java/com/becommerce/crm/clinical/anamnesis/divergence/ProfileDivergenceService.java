package com.becommerce.crm.clinical.anamnesis.divergence;

import com.becommerce.crm.masterdata.person.PersonException;
import com.becommerce.crm.masterdata.person.PersonService;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Divergências entre anamnese e cadastro: listar, aceitar (aplica no cadastro) ou ignorar. */
@Service
public class ProfileDivergenceService {

    public record ProfileDivergenceView(UUID id, String field, String currentValue, String proposedValue,
                                        String reason, Instant createdAt) {
    }

    private final ProfileDivergenceJpaRepository repository;
    private final PersonService persons;

    public ProfileDivergenceService(ProfileDivergenceJpaRepository repository, PersonService persons) {
        this.repository = repository;
        this.persons = persons;
    }

    @Transactional(readOnly = true)
    public List<ProfileDivergenceView> pending(UUID companyId, UUID personId) {
        try {
            TenantContext.setCompanyId(companyId);
            return repository.findByCompanyIdAndPersonIdAndStatusOrderByCreatedAtDesc(companyId, personId, "PENDING")
                    .stream().map(ProfileDivergenceService::view).toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public List<ProfileDivergenceView> accept(UUID companyId, UUID personId, UUID id, UUID userId) {
        try {
            TenantContext.setCompanyId(companyId);
            ProfileDivergenceJpaEntity d = requirePending(companyId, personId, id);
            persons.applyAnamnesisPatch(companyId, personId, PersonService.ProfileField.valueOf(d.field),
                    d.proposedValue, userId, "Anamnese (divergência aceita)");
            resolve(d, "ACCEPTED", userId);
        } finally {
            TenantContext.clear();
        }
        return pending(companyId, personId);
    }

    @Transactional
    public List<ProfileDivergenceView> dismiss(UUID companyId, UUID personId, UUID id, UUID userId) {
        try {
            TenantContext.setCompanyId(companyId);
            resolve(requirePending(companyId, personId, id), "DISMISSED", userId);
        } finally {
            TenantContext.clear();
        }
        return pending(companyId, personId);
    }

    private ProfileDivergenceJpaEntity requirePending(UUID companyId, UUID personId, UUID id) {
        ProfileDivergenceJpaEntity d = repository.findByIdAndCompanyIdAndPersonId(id, companyId, personId)
                .orElseThrow(() -> PersonException.notFound("Divergência"));
        if (!"PENDING".equals(d.status)) {
            throw PersonException.badRequest("Esta divergência já foi resolvida.");
        }
        return d;
    }

    private void resolve(ProfileDivergenceJpaEntity d, String status, UUID userId) {
        d.status = status;
        d.resolvedBy = userId;
        d.resolvedAt = Instant.now();
        repository.save(d);
    }

    private static ProfileDivergenceView view(ProfileDivergenceJpaEntity d) {
        return new ProfileDivergenceView(d.id, d.field, d.currentValue, d.proposedValue, d.reason, d.createdAt);
    }
}
