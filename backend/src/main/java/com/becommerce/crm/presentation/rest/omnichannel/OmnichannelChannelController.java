package com.becommerce.crm.presentation.rest.omnichannel;

import com.becommerce.crm.application.omnichannel.dto.ChannelRequest;
import com.becommerce.crm.application.omnichannel.dto.ChannelResponse;
import com.becommerce.crm.application.omnichannel.port.input.OmnichannelChannelUseCase;
import com.becommerce.crm.domain.omnichannel.ChannelStatus;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Gestão de canais omnichannel (ex.: WhatsApp). Scoped à empresa ativa (RLS). */
@RestController
@RequestMapping("/api/v1/omnichannel/channels")
public class OmnichannelChannelController {

    private final OmnichannelChannelUseCase channelUseCase;

    public OmnichannelChannelController(OmnichannelChannelUseCase channelUseCase) {
        this.channelUseCase = channelUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('omnichannel:read')")
    public ResponseEntity<List<ChannelResponse>> list(@AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(channelUseCase.listByCompany(principal.companyId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('omnichannel:read')")
    public ResponseEntity<ChannelResponse> getById(@PathVariable UUID id,
                                                   @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(channelUseCase.getById(principal.companyId(), id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('omnichannel:create')")
    public ResponseEntity<ChannelResponse> create(@Valid @RequestBody ChannelRequest request,
                                                  @AuthenticationPrincipal CurrentUser principal) {
        ChannelResponse response = channelUseCase.create(principal.companyId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('omnichannel:update')")
    public ResponseEntity<ChannelResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody ChannelRequest request,
                                                  @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(channelUseCase.update(principal.companyId(), id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('omnichannel:update')")
    public ResponseEntity<ChannelResponse> setStatus(@PathVariable UUID id,
                                                     @Valid @RequestBody StatusRequest request,
                                                     @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(channelUseCase.setStatus(principal.companyId(), id, request.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('omnichannel:delete')")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal CurrentUser principal) {
        channelUseCase.delete(principal.companyId(), id);
        return ResponseEntity.noContent().build();
    }

    private record StatusRequest(@NotNull ChannelStatus status) {
    }
}
