package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.sales.followup.application.dto.FollowUpRequest;
import com.becommerce.crm.sales.followup.application.port.in.FollowUpUseCase;
import com.becommerce.crm.sales.lead.application.dto.UpdateLeadRequest;
import com.becommerce.crm.sales.lead.application.port.in.LeadUseCase;
import com.becommerce.crm.sales.lead.domain.LeadStatus;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Acompanha os leads captados pelo agente (WhatsApp/Instagram) até o fim:
 * quando o cliente para de responder, agenda follow-ups no motor de follow-up
 * (que os cancela sozinho se o cliente responder) e, sem resposta depois do
 * último, marca o lead como perdido.
 *
 * <p>Instagram só permite mensagem da empresa até 24h depois da última
 * mensagem do cliente: um follow-up só, antes de 24h. Só atua em conversas no
 * modo automático de empresas com auto-resposta ligada.
 */
@Component
public class LeadFollowUpJob {

    private static final Logger log = LoggerFactory.getLogger(LeadFollowUpJob.class);

    static final String NO_REPLY_NOTE = "Sem resposta depois dos follow-ups; marcado como perdido automaticamente.";

    private static final String CANDIDATES = """
            SELECT l.id AS lead_id, l.notes, c.first_name, conv.id AS conversation_id, ch.type AS channel_type,
                   EXTRACT(EPOCH FROM (LOCALTIMESTAMP - last_in.at)) / 3600.0 AS silent_hours,
                   last_in.at AS last_inbound_at
              FROM leads l
              JOIN contacts c ON c.id = l.contact_id
              JOIN omnichannel_conversations conv
                ON conv.contact_id = l.contact_id AND conv.company_id = l.company_id
              JOIN omnichannel_channels ch ON ch.id = conv.channel_id
              JOIN agent_config ac ON ac.company_id = l.company_id AND ac.ai_enabled AND ac.allow_auto_reply
              CROSS JOIN LATERAL (
                    SELECT max(m.created_at) AS at FROM omnichannel_messages m
                     WHERE m.conversation_id = conv.id AND m.direction = 'INBOUND') last_in
             WHERE l.company_id = :companyId
               AND l.status IN ('NEW', 'CONTACTED', 'QUALIFIED')
               AND l.source IN ('INSTAGRAM', 'WHATSAPP')
               AND conv.handoff_mode = 'AUTOMATIC'
               AND last_in.at IS NOT NULL
               AND EXISTS (SELECT 1 FROM omnichannel_messages o
                            WHERE o.conversation_id = conv.id AND o.direction = 'OUTBOUND'
                              AND o.created_at > last_in.at)
            """;

    /** Um passo da régua: depois de {@code afterHours} sem resposta, envia {@code message}. */
    record Step(double afterHours, String message) {}

    /** Régua de um canal: passos de follow-up e quando desistir. */
    record Plan(List<Step> steps, double lostAfterHours) {}

    record Candidate(UUID leadId, String notes, String firstName, UUID conversationId, String channelType,
                     double silentHours, LocalDateTime lastInboundAt) {}

    /** O que fazer com um candidato agora. */
    sealed interface Decision permits Nothing, SendFollowUp, MarkLost {}
    record Nothing() implements Decision {}
    record SendFollowUp(int step, String message) implements Decision {}
    record MarkLost() implements Decision {}

    private final NamedParameterJdbcTemplate jdbc;
    private final FollowUpUseCase followUpUseCase;
    private final LeadUseCase leadUseCase;
    private final Plan instagram;
    private final Plan whatsapp;
    private final boolean enabled;

    public LeadFollowUpJob(NamedParameterJdbcTemplate jdbc, FollowUpUseCase followUpUseCase, LeadUseCase leadUseCase,
                           @Value("${lead-followup.enabled:true}") boolean enabled,
                           @Value("${lead-followup.instagram.first-hours:20}") double igFirst,
                           @Value("${lead-followup.instagram.lost-hours:48}") double igLost,
                           @Value("${lead-followup.whatsapp.first-hours:24}") double waFirst,
                           @Value("${lead-followup.whatsapp.second-hours:72}") double waSecond,
                           @Value("${lead-followup.whatsapp.lost-hours:168}") double waLost) {
        this.jdbc = jdbc;
        this.followUpUseCase = followUpUseCase;
        this.leadUseCase = leadUseCase;
        this.enabled = enabled;
        this.instagram = new Plan(List.of(new Step(igFirst, FIRST_MESSAGE)), igLost);
        this.whatsapp = new Plan(List.of(new Step(waFirst, FIRST_MESSAGE), new Step(waSecond, SECOND_MESSAGE)),
                waLost);
    }

    static final String FIRST_MESSAGE =
            "Oi, {nome}! Passando para saber se ficou alguma dúvida. Quer que eu separe o seu pedido? 😊";
    static final String SECOND_MESSAGE =
            "Oi, {nome}! Ainda tenho as peças disponíveis. Se quiser seguir com o pedido, é só me responder por aqui.";

    @Scheduled(fixedDelayString = "${lead-followup.interval-ms:900000}", initialDelayString = "${lead-followup.initial-delay-ms:120000}")
    public void run() {
        if (!enabled) {
            return;
        }
        List<UUID> companyIds = jdbc.queryForList("SELECT id FROM app.all_company_ids()", Map.of(), UUID.class);
        for (UUID companyId : companyIds) {
            try {
                processCompany(companyId);
            } catch (Exception e) {
                log.error("[LEAD-FOLLOWUP] falha na empresa {}: {}", companyId, e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    void processCompany(UUID companyId) {
        TenantContext.setCompanyId(companyId);
        List<Candidate> candidates = jdbc.query(CANDIDATES, Map.of("companyId", companyId), (rs, n) -> new Candidate(
                rs.getObject("lead_id", UUID.class), rs.getString("notes"), rs.getString("first_name"),
                rs.getObject("conversation_id", UUID.class), rs.getString("channel_type"),
                rs.getDouble("silent_hours"), rs.getObject("last_inbound_at", LocalDateTime.class)));
        for (Candidate candidate : candidates) {
            try {
                apply(companyId, candidate, decide(planFor(candidate.channelType()), candidate.silentHours()));
            } catch (Exception e) {
                log.warn("[LEAD-FOLLOWUP] lead {} (company={}): {}", candidate.leadId(), companyId, e.getMessage());
            }
        }
    }

    Plan planFor(String channelType) {
        return "INSTAGRAM".equals(channelType) ? instagram : whatsapp;
    }

    /** Passo mais avançado já vencido; perdido quando passou do limite. */
    static Decision decide(Plan plan, double silentHours) {
        if (silentHours >= plan.lostAfterHours()) {
            return new MarkLost();
        }
        for (int i = plan.steps().size() - 1; i >= 0; i--) {
            Step step = plan.steps().get(i);
            if (silentHours >= step.afterHours()) {
                return new SendFollowUp(i + 1, step.message());
            }
        }
        return new Nothing();
    }

    private void apply(UUID companyId, Candidate candidate, Decision decision) {
        switch (decision) {
            case Nothing n -> { }
            case SendFollowUp send -> {
                // Uma chave por conversa, silêncio e passo: rodar de novo não duplica o follow-up.
                UUID key = UUID.nameUUIDFromBytes(("lead-followup:" + candidate.conversationId() + ":"
                        + candidate.lastInboundAt() + ":" + send.step()).getBytes(StandardCharsets.UTF_8));
                followUpUseCase.create(companyId, new FollowUpRequest(candidate.conversationId(),
                        personalize(send.message(), candidate.firstName()), LocalDateTime.now(), key, null));
                TenantContext.setCompanyId(companyId);
            }
            case MarkLost lost -> {
                leadUseCase.update(companyId, candidate.leadId(), new UpdateLeadRequest(LeadStatus.LOST,
                        null, null, null, null, LeadCaptureToolProvider.appendNote(candidate.notes(), NO_REPLY_NOTE)));
                TenantContext.setCompanyId(companyId);
                log.info("[LEAD-FOLLOWUP] lead {} marcado como perdido (company={})", candidate.leadId(), companyId);
            }
        }
    }

    static String personalize(String template, String firstName) {
        String name = firstName == null || firstName.isBlank() ? "" : firstName.trim();
        return name.isEmpty() ? template.replace(", {nome}", "") : template.replace("{nome}", name);
    }
}
