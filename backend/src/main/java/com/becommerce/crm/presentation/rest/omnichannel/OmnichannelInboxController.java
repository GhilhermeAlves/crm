package com.becommerce.crm.presentation.rest.omnichannel;

import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.omnichannel.dto.ConversationDetailResponse;
import com.becommerce.crm.application.omnichannel.dto.ConversationResponse;
import com.becommerce.crm.application.omnichannel.dto.MessageResponse;
import com.becommerce.crm.application.omnichannel.dto.SendMessageRequest;
import com.becommerce.crm.application.omnichannel.port.input.OmnichannelInboxUseCase;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Inbox omnichannel: conversas, mensagens e envio. Scoped à empresa ativa (RLS). */
@RestController
@RequestMapping("/api/v1/omnichannel/inbox")
public class OmnichannelInboxController {

    private final OmnichannelInboxUseCase inboxUseCase;

    public OmnichannelInboxController(OmnichannelInboxUseCase inboxUseCase) {
        this.inboxUseCase = inboxUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('omnichannel:read')")
    public ResponseEntity<PageResponse<ConversationResponse>> listConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(inboxUseCase.listConversations(principal.companyId(), page, pageSize));
    }

    @GetMapping("/{conversationId}")
    @PreAuthorize("hasAuthority('omnichannel:read')")
    public ResponseEntity<ConversationDetailResponse> getConversation(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int pageSize,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(inboxUseCase.getConversation(principal.companyId(), conversationId, page, pageSize));
    }

    @PostMapping("/{conversationId}/messages")
    @PreAuthorize("hasAuthority('omnichannel:send')")
    public ResponseEntity<MessageResponse> send(@PathVariable UUID conversationId,
                                                @Valid @RequestBody SendMessageRequest request,
                                                @AuthenticationPrincipal CurrentUser principal) {
        MessageResponse response = inboxUseCase.send(principal.companyId(), conversationId, request.body());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{conversationId}/read")
    @PreAuthorize("hasAuthority('omnichannel:update')")
    public ResponseEntity<Void> markRead(@PathVariable UUID conversationId,
                                         @AuthenticationPrincipal CurrentUser principal) {
        inboxUseCase.markRead(principal.companyId(), conversationId);
        return ResponseEntity.noContent().build();
    }
}
