package com.becommerce.crm.clinical.anamnesis.divergence;

import com.becommerce.crm.clinical.anamnesis.divergence.ProfileDivergenceService.ProfileDivergenceView;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Divergências entre a anamnese e o cadastro do paciente. */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/patients/{contactId}/profile-divergences")
public class ProfileDivergenceController {

    private static final String ACCESS_MESSAGE = "Você só pode acessar pacientes da sua própria empresa.";

    private final ProfileDivergenceService service;

    public ProfileDivergenceController(ProfileDivergenceService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('contact:read')")
    public List<ProfileDivergenceView> list(@CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
                                            @PathVariable UUID contactId) {
        return service.pending(companyId, contactId);
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('contact:update')")
    public List<ProfileDivergenceView> accept(@CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
                                              @PathVariable UUID contactId, @PathVariable UUID id,
                                              @AuthenticationPrincipal CurrentUser user) {
        return service.accept(companyId, contactId, id, user.userId());
    }

    @PostMapping("/{id}/dismiss")
    @PreAuthorize("hasAuthority('contact:update')")
    public List<ProfileDivergenceView> dismiss(@CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
                                               @PathVariable UUID contactId, @PathVariable UUID id,
                                               @AuthenticationPrincipal CurrentUser user) {
        return service.dismiss(companyId, contactId, id, user.userId());
    }
}
