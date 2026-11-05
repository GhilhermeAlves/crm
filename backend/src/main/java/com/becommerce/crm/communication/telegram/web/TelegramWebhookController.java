package com.becommerce.crm.communication.telegram.web;

import com.becommerce.crm.communication.telegram.application.TelegramConfigService;
import com.becommerce.crm.communication.telegram.application.TelegramUpdateDispatcher;
import com.becommerce.crm.communication.telegram.infrastructure.TelegramUpdateSeenJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
public class TelegramWebhookController {

    public static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final TelegramConfigService config;
    private final TelegramUpdateSeenJpaRepository seen;
    private final TelegramUpdateDispatcher dispatcher;
    private final ObjectMapper mapper;

    public TelegramWebhookController(TelegramConfigService config, TelegramUpdateSeenJpaRepository seen,
                                     TelegramUpdateDispatcher dispatcher, ObjectMapper mapper) {
        this.config = config;
        this.seen = seen;
        this.dispatcher = dispatcher;
        this.mapper = mapper;
    }

    @PostMapping("/api/v1/telegram/webhook/{companyId}")
    public ResponseEntity<Void> receive(@PathVariable UUID companyId,
                                        @RequestHeader(value = SECRET_HEADER, required = false) String secret,
                                        @RequestBody String body) throws JsonProcessingException {
        Optional<String> token = config.tokenIfValidSecret(companyId, secret);
        if (token.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        JsonNode update;
        try {
            update = mapper.readTree(body);
        } catch (JsonProcessingException e) {
            return ResponseEntity.ok().build();
        }
        long updateId = update.path("update_id").asLong(-1);
        if (updateId < 0) return ResponseEntity.ok().build();
        int fresh = TelegramConfigService.withTenant(companyId, () -> seen.markSeen(companyId, updateId));
        if (fresh == 1) dispatcher.dispatch(companyId, token.get(), update);
        return ResponseEntity.ok().build();
    }
}
