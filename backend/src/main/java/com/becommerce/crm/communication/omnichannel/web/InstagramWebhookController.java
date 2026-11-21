package com.becommerce.crm.communication.omnichannel.web;

import com.becommerce.crm.communication.omnichannel.application.port.input.WhatsAppWebhookUseCase;
import com.becommerce.crm.communication.omnichannel.infrastructure.instagram.MetaInstagramWebhookParser;
import com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp.WhatsAppWebhookTokenVerifier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Webhook do Instagram Direct (app da Meta com a Instagram API e login do Instagram).
 *
 * <p>No painel da Meta: Callback URL {@code https://<dominio>/api/v1/omnichannel/instagram/webhook},
 * Verify token = {@code META_INSTAGRAM_VERIFY_TOKEN} e campo {@code messages} assinado.
 * Cada POST é autenticado só pela assinatura {@code X-Hub-Signature-256}
 * (HMAC-SHA256 do corpo com o App Secret do app do Instagram, {@code META_INSTAGRAM_APP_SECRET});
 * não há {@code ?token=}, porque a Meta não o envia.
 *
 * <p>O processamento é o mesmo do WhatsApp: o roteador de parsers reconhece
 * {@code object=instagram} e o canal {@code META_INSTAGRAM} é resolvido pelo
 * {@code entry.id} (id da conta profissional = {@code externalId} do canal).
 */
@RestController
@RequestMapping(InstagramWebhookController.PATH)
public class InstagramWebhookController {

    public static final String PATH = "/api/v1/omnichannel/instagram/webhook";

    private static final Logger log = LoggerFactory.getLogger(InstagramWebhookController.class);

    private final WhatsAppWebhookUseCase webhookUseCase;
    private final WhatsAppWebhookTokenVerifier verifier;
    private final ObjectMapper objectMapper;

    @Autowired
    public InstagramWebhookController(WhatsAppWebhookUseCase webhookUseCase, ObjectMapper objectMapper,
                                      @Value("${omnichannel.instagram.verify-token:}") String verifyToken,
                                      @Value("${omnichannel.instagram.app-secret:}") String appSecret) {
        this(webhookUseCase, new WhatsAppWebhookTokenVerifier(false, "", verifyToken, appSecret), objectMapper);
    }

    InstagramWebhookController(WhatsAppWebhookUseCase webhookUseCase, WhatsAppWebhookTokenVerifier verifier,
                               ObjectMapper objectMapper) {
        this.webhookUseCase = webhookUseCase;
        this.verifier = verifier;
        this.objectMapper = objectMapper;
    }

    /** Handshake da Meta: devolve o {@code hub.challenge} quando o verify token confere. */
    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if (challenge == null || !verifier.isMetaSubscription(mode, verifyToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(challenge);
    }

    /** Evento do Instagram. Idempotente pelo {@code mid} da mensagem. */
    @PostMapping
    public ResponseEntity<Void> handle(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody byte[] rawPayload) {
        // A assinatura é sobre os bytes exatos recebidos, por isso o corpo chega como byte[].
        if (!verifier.isValidMetaSignature(rawPayload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(rawPayload, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("Webhook do Instagram com payload inválido; rejeitando");
            return ResponseEntity.badRequest().build();
        }
        if (!MetaInstagramWebhookParser.isInstagramPayload(payload)) {
            log.warn("Webhook do Instagram com object inesperado; ignorando");
            return ResponseEntity.ok().build();
        }
        webhookUseCase.handleEvent(payload);
        return ResponseEntity.ok().build();
    }
}
