package com.becommerce.crm.communication.omnichannel.web;

import com.becommerce.crm.communication.omnichannel.application.dto.IgnoredContactRequest;
import com.becommerce.crm.communication.omnichannel.application.port.input.OmnichannelIgnoredContactUseCase;
import com.becommerce.crm.communication.omnichannel.domain.IgnoredContact;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Números que a IA do WhatsApp ignora (não grava nem responde). Scoped à empresa ativa. */
@RestController
@RequestMapping("/api/v1/omnichannel/ignored-contacts")
public class OmnichannelIgnoredContactController {

    private final OmnichannelIgnoredContactUseCase useCase;

    public OmnichannelIgnoredContactController(OmnichannelIgnoredContactUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('omnichannel:read')")
    public ResponseEntity<List<IgnoredContact>> list(@AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(useCase.list(principal.companyId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('omnichannel:update')")
    public ResponseEntity<IgnoredContact> add(@Valid @RequestBody IgnoredContactRequest request,
                                              @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.add(principal.companyId(), request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('omnichannel:update')")
    public ResponseEntity<Void> remove(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser principal) {
        useCase.remove(principal.companyId(), id);
        return ResponseEntity.noContent().build();
    }
}
