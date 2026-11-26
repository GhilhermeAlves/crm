package com.becommerce.crm.clinical.trichology.web;

import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.FindingRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.MarkerRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.ProtocolRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.SettingsUpdateRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.SettingsView;
import com.becommerce.crm.clinical.trichology.application.service.TrichologySettingsService;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Protocolos, catálogo de achados, marcadores e limites da clínica (doc v1.2, seção 7). */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/trichology/settings")
public class TrichologySettingsController {

    private static final String OWN_COMPANY = "Você só pode configurar a tricologia da sua própria empresa.";

    private final TrichologySettingsService service;

    public TrichologySettingsController(TrichologySettingsService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('trichology:read')")
    public SettingsView get(@CurrentCompanyId(OWN_COMPANY) UUID companyId) {
        return service.get(companyId);
    }

    @PutMapping
    @PreAuthorize("hasAuthority('trichology:settings')")
    public SettingsView update(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                               @RequestBody SettingsUpdateRequest request,
                               @AuthenticationPrincipal CurrentUser user) {
        return service.update(companyId, request, user.userId());
    }

    @PostMapping("/protocols")
    @PreAuthorize("hasAuthority('trichology:settings')")
    public SettingsView createProtocol(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                       @RequestBody ProtocolRequest request,
                                       @AuthenticationPrincipal CurrentUser user) {
        return service.saveProtocol(companyId, null, request, user.userId());
    }

    @PutMapping("/protocols/{protocolId}")
    @PreAuthorize("hasAuthority('trichology:settings')")
    public SettingsView updateProtocol(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                       @PathVariable UUID protocolId, @RequestBody ProtocolRequest request,
                                       @AuthenticationPrincipal CurrentUser user) {
        return service.saveProtocol(companyId, protocolId, request, user.userId());
    }

    @PostMapping("/findings")
    @PreAuthorize("hasAuthority('trichology:settings')")
    public SettingsView createFinding(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                      @RequestBody FindingRequest request,
                                      @AuthenticationPrincipal CurrentUser user) {
        return service.saveFinding(companyId, null, request, user.userId());
    }

    @PutMapping("/findings/{findingId}")
    @PreAuthorize("hasAuthority('trichology:settings')")
    public SettingsView updateFinding(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                      @PathVariable UUID findingId, @RequestBody FindingRequest request,
                                      @AuthenticationPrincipal CurrentUser user) {
        return service.saveFinding(companyId, findingId, request, user.userId());
    }

    @PostMapping("/markers")
    @PreAuthorize("hasAuthority('trichology:settings')")
    public SettingsView createMarker(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                     @RequestBody MarkerRequest request,
                                     @AuthenticationPrincipal CurrentUser user) {
        return service.saveMarker(companyId, null, request, user.userId());
    }

    @PutMapping("/markers/{markerId}")
    @PreAuthorize("hasAuthority('trichology:settings')")
    public SettingsView updateMarker(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                     @PathVariable UUID markerId, @RequestBody MarkerRequest request,
                                     @AuthenticationPrincipal CurrentUser user) {
        return service.saveMarker(companyId, markerId, request, user.userId());
    }
}
