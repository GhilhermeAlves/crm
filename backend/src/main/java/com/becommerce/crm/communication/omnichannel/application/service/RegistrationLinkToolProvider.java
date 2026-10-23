package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.automation.ai.application.agent.tool.AgentToolProvider;
import com.becommerce.crm.automation.ai.application.agent.tool.AgentToolSession;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.domain.AgentConfig;
import com.becommerce.crm.masterdata.contact.registration.application.dto.RegistrationLinkResponse;
import com.becommerce.crm.masterdata.contact.registration.application.service.RegistrationLinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code enviar_link_cadastro}: gera o link público para o próprio paciente
 * preencher/corrigir seus dados cadastrais, sem pedir CPF pela conversa.
 * O contato vem sempre da sessão, nunca dos argumentos do modelo.
 */
@Component
public class RegistrationLinkToolProvider implements AgentToolProvider {

    private static final Logger log = LoggerFactory.getLogger(RegistrationLinkToolProvider.class);

    static final String SEND_LINK = "enviar_link_cadastro";

    private static final String FAILURE =
            "Não foi possível gerar o link agora. Diga que a equipe vai retornar em breve.";

    private final RegistrationLinkService registrationLinkService;

    public RegistrationLinkToolProvider(RegistrationLinkService registrationLinkService) {
        this.registrationLinkService = registrationLinkService;
    }

    @Override
    public String id() {
        return "registration_link";
    }

    @Override
    public boolean isAvailable(AgentConfig agent, AgentToolSession session) {
        return session.contactId() != null;
    }

    @Override
    public List<AiProvider.ToolDefinition> definitions() {
        Map<String, Object> schema = Map.of("type", "object", "properties", Map.of());
        return List.of(new AiProvider.ToolDefinition(SEND_LINK,
                "Gera o link para o paciente preencher ou corrigir os próprios dados cadastrais.",
                schema));
    }

    @Override
    public Optional<String> guidance(AgentConfig agent, AgentToolSession session) {
        return Optional.of("Se o paciente precisar informar ou corrigir dados cadastrais (CPF, data de nascimento, "
                + "e-mail, endereço), use enviar_link_cadastro e envie o link; não peça CPF pela conversa.");
    }

    @Override
    public String execute(AgentToolSession session, AiProvider.ToolCall call) {
        try {
            if (session.contactId() == null) {
                return FAILURE;
            }
            RegistrationLinkResponse link = registrationLinkService.create(
                    session.companyId(), session.contactId(), null);
            return "Link de cadastro: " + link.url() + " (válido por 7 dias). Envie ao paciente.";
        } catch (RuntimeException e) {
            log.warn("[AGENT][CADASTRO] falha ao gerar link (company={}): {}", session.companyId(), e.getMessage());
            return FAILURE;
        }
    }
}
