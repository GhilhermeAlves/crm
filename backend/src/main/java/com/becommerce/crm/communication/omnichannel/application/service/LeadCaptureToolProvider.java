package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.automation.ai.application.agent.tool.AgentToolProvider;
import com.becommerce.crm.automation.ai.application.agent.tool.AgentToolSession;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.domain.AgentConfig;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.communication.omnichannel.domain.Channel;
import com.becommerce.crm.communication.omnichannel.domain.ChannelType;
import com.becommerce.crm.communication.omnichannel.domain.Conversation;
import com.becommerce.crm.communication.omnichannel.domain.IgnoredContact;
import com.becommerce.crm.masterdata.contact.application.dto.request.CreateContactRequest;
import com.becommerce.crm.masterdata.contact.application.port.in.ContactUseCase;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.Contact;
import com.becommerce.crm.sales.lead.application.dto.CreateLeadRequest;
import com.becommerce.crm.sales.lead.application.port.in.LeadUseCase;
import com.becommerce.crm.sales.lead.application.port.out.LeadRepository;
import com.becommerce.crm.sales.lead.domain.LeadSource;
import com.becommerce.crm.sales.lead.domain.LeadStatus;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code registrar_lead}: transforma o interessado da conversa (WhatsApp ou
 * Instagram) em lead no funil. Cria o contato quando a conversa ainda não tem
 * um, vincula o contato à conversa e registra o lead com a origem do canal e
 * o resumo do pedido nas notas. Escopo (empresa, conversa) vem sempre da
 * sessão, nunca dos argumentos do modelo.
 */
@Component
public class LeadCaptureToolProvider implements AgentToolProvider {

    private static final Logger log = LoggerFactory.getLogger(LeadCaptureToolProvider.class);

    static final String REGISTER_LEAD = "registrar_lead";
    static final int MAX_NOTES = 1000;

    private static final String FAILURE =
            "Não foi possível registrar agora. Diga que a equipe vai retornar em breve.";

    private final OmnichannelConversationRepository conversationRepository;
    private final OmnichannelChannelRepository channelRepository;
    private final ContactRepository contactRepository;
    private final ContactUseCase contactUseCase;
    private final LeadUseCase leadUseCase;
    private final LeadRepository leadRepository;

    public LeadCaptureToolProvider(OmnichannelConversationRepository conversationRepository,
                                   OmnichannelChannelRepository channelRepository,
                                   ContactRepository contactRepository,
                                   ContactUseCase contactUseCase,
                                   LeadUseCase leadUseCase,
                                   LeadRepository leadRepository) {
        this.conversationRepository = conversationRepository;
        this.channelRepository = channelRepository;
        this.contactRepository = contactRepository;
        this.contactUseCase = contactUseCase;
        this.leadUseCase = leadUseCase;
        this.leadRepository = leadRepository;
    }

    @Override
    public String id() {
        return "lead_capture";
    }

    @Override
    public boolean isAvailable(AgentConfig agent, AgentToolSession session) {
        return session.conversationId() != null;
    }

    @Override
    public List<AiProvider.ToolDefinition> definitions() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("nome", Map.of("type", "string", "description", "Nome da pessoa"));
        props.put("interesse", Map.of("type", "string", "description", "Produto ou serviço de interesse"));
        props.put("quantidade", Map.of("type", "string", "description", "Quantidade desejada"));
        props.put("personalizacao", Map.of("type", "string", "description", "Nome, frase, cor ou outro detalhe"));
        props.put("data_desejada", Map.of("type", "string", "description", "Quando precisa"));
        props.put("entrega", Map.of("type", "string", "description", "Entrega (com bairro) ou retirada"));
        props.put("telefone", Map.of("type", "string", "description", "Telefone, se a pessoa informou"));
        props.put("observacoes", Map.of("type", "string", "description", "Outros detalhes relevantes"));
        Map<String, Object> schema = Map.of("type", "object", "properties", props,
                "required", List.of("nome", "interesse"));
        return List.of(new AiProvider.ToolDefinition(REGISTER_LEAD,
                "Registra a pessoa como lead no CRM com o resumo do que ela quer.", schema));
    }

    @Override
    public Optional<String> guidance(AgentConfig agent, AgentToolSession session) {
        return Optional.of("Quando souber o nome da pessoa e o que ela quer, use registrar_lead uma vez com o "
                + "resumo do pedido. Não comente com a pessoa que registrou.");
    }

    @Override
    public String execute(AgentToolSession session, AiProvider.ToolCall call) {
        UUID previousTenant = TenantContext.hasCompanyId() ? TenantContext.getCompanyId() : null;
        try {
            Map<String, Object> args = call.arguments() == null ? Map.of() : call.arguments();
            String name = text(args.get("nome"));
            String interest = text(args.get("interesse"));
            if (name == null || interest == null) {
                return "Faltam dados: pergunte o nome da pessoa e o que ela procura antes de registrar.";
            }
            Conversation conversation = conversationRepository.findById(session.conversationId()).orElse(null);
            if (conversation == null) {
                return FAILURE;
            }
            ChannelType channelType = channelRepository.findById(conversation.getChannelId())
                    .map(Channel::getType).orElse(ChannelType.WHATSAPP);
            LeadSource source = channelType == ChannelType.INSTAGRAM ? LeadSource.INSTAGRAM : LeadSource.WHATSAPP;

            String phone = channelType == ChannelType.WHATSAPP
                    ? conversation.getExternalPhone() : text(args.get("telefone"));
            UUID contactId = resolveContact(session, conversation, name, phone, channelType);

            TenantContext.setCompanyId(session.companyId());
            if (leadRepository.existsByContactIdAndCompanyId(contactId, session.companyId())) {
                return "Esta pessoa já é um lead no CRM. Siga o atendimento normalmente.";
            }
            leadUseCase.create(session.companyId(), new CreateLeadRequest(contactId, LeadStatus.NEW, null, null,
                    source, null, null, notes(args, channelType)), null);
            log.info("[AGENT][LEAD] lead registrado (company={}, conversation={}, source={})",
                    session.companyId(), session.conversationId(), source);
            return "Lead registrado. Continue o atendimento normalmente.";
        } catch (RuntimeException e) {
            log.warn("[AGENT][LEAD] falha ao registrar lead (company={}): {}", session.companyId(), e.getMessage());
            return FAILURE;
        } finally {
            if (previousTenant != null) {
                TenantContext.setCompanyId(previousTenant);
            } else {
                TenantContext.clear();
            }
        }
    }

    private UUID resolveContact(AgentToolSession session, Conversation conversation, String name, String phone,
                                ChannelType channelType) {
        if (conversation.getContactId() != null) {
            return conversation.getContactId();
        }
        TenantContext.setCompanyId(session.companyId());
        UUID contactId = channelType == ChannelType.WHATSAPP ? findByPhone(session.companyId(), phone) : null;
        if (contactId == null) {
            String[] parts = name.trim().split("\\s+", 2);
            String notes = channelType == ChannelType.INSTAGRAM ? "Contato vindo do Instagram Direct." : null;
            contactId = contactUseCase.create(session.companyId(), new CreateContactRequest(
                    truncate(parts[0], 100), parts.length > 1 ? truncate(parts[1], 100) : null, null,
                    null, phone == null ? null : truncate(phone, 20), notes, null, null,
                    null, null, null, null, null,
                    null, null, null, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null), null).id();
        }
        conversation.assignContact(contactId);
        conversationRepository.save(conversation);
        return contactId;
    }

    private UUID findByPhone(UUID companyId, String phone) {
        if (phone == null) {
            return null;
        }
        for (String variant : IgnoredContact.phoneVariants(phone)) {
            for (String candidate : List.of(variant, "+" + variant)) {
                Optional<Contact> contact = contactRepository.findByCompanyIdAndPhone(companyId, candidate);
                if (contact.isPresent()) {
                    return contact.get().getId();
                }
            }
        }
        return null;
    }

    static String notes(Map<String, Object> args, ChannelType channelType) {
        StringBuilder sb = new StringBuilder("Pedido via ")
                .append(channelType == ChannelType.INSTAGRAM ? "Instagram" : "WhatsApp").append(" (agente)");
        appendLine(sb, "Interesse", args.get("interesse"));
        appendLine(sb, "Quantidade", args.get("quantidade"));
        appendLine(sb, "Personalização", args.get("personalizacao"));
        appendLine(sb, "Data desejada", args.get("data_desejada"));
        appendLine(sb, "Entrega", args.get("entrega"));
        appendLine(sb, "Telefone", args.get("telefone"));
        appendLine(sb, "Observações", args.get("observacoes"));
        return truncate(sb.toString(), MAX_NOTES);
    }

    private static void appendLine(StringBuilder sb, String label, Object value) {
        String v = text(value);
        if (v != null) {
            sb.append('\n').append(label).append(": ").append(v);
        }
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
