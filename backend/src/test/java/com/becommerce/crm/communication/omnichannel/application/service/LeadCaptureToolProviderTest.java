package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.automation.ai.application.agent.tool.AgentToolSession;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.domain.AgentConfig;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.communication.omnichannel.domain.Channel;
import com.becommerce.crm.communication.omnichannel.domain.ChannelProvider;
import com.becommerce.crm.communication.omnichannel.domain.ChannelType;
import com.becommerce.crm.communication.omnichannel.domain.Conversation;
import com.becommerce.crm.masterdata.contact.application.dto.request.CreateContactRequest;
import com.becommerce.crm.masterdata.contact.application.dto.response.ContactResponse;
import com.becommerce.crm.masterdata.contact.application.port.in.ContactUseCase;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.sales.lead.application.dto.CreateLeadRequest;
import com.becommerce.crm.sales.lead.application.dto.UpdateLeadRequest;
import com.becommerce.crm.sales.lead.application.port.in.LeadUseCase;
import com.becommerce.crm.sales.lead.application.port.out.LeadRepository;
import com.becommerce.crm.sales.lead.domain.Lead;
import com.becommerce.crm.sales.lead.domain.LeadSource;
import com.becommerce.crm.sales.lead.domain.LeadStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeadCaptureToolProviderTest {

    private final OmnichannelConversationRepository conversations = mock(OmnichannelConversationRepository.class);
    private final OmnichannelChannelRepository channels = mock(OmnichannelChannelRepository.class);
    private final ContactRepository contactRepository = mock(ContactRepository.class);
    private final ContactUseCase contactUseCase = mock(ContactUseCase.class);
    private final LeadUseCase leadUseCase = mock(LeadUseCase.class);
    private final LeadRepository leadRepository = mock(LeadRepository.class);
    private final LeadCaptureToolProvider provider = new LeadCaptureToolProvider(
            conversations, channels, contactRepository, contactUseCase, leadUseCase, leadRepository);

    private final UUID companyId = UUID.randomUUID();
    private final UUID newContactId = UUID.randomUUID();

    private Conversation conversation(ChannelType type, UUID contactId, String externalPhone) {
        Channel channel = Channel.create(companyId, type,
                type == ChannelType.INSTAGRAM ? ChannelProvider.META_INSTAGRAM : ChannelProvider.EVOLUTION,
                "Canal", "ext", null, null);
        Conversation c = Conversation.create(companyId, channel.getId(), contactId, externalPhone);
        when(conversations.findById(c.getId())).thenReturn(Optional.of(c));
        when(channels.findById(channel.getId())).thenReturn(Optional.of(channel));
        return c;
    }

    private AgentToolSession session(Conversation c) {
        return new AgentToolSession(companyId, UUID.randomUUID(), c.getId(), c.getContactId(),
                c.getExternalPhone(), "Ana", UUID.randomUUID());
    }

    private static AiProvider.ToolCall call(Map<String, Object> args) {
        return new AiProvider.ToolCall("c1", LeadCaptureToolProvider.REGISTER_LEAD, args);
    }

    private static Map<String, Object> args() {
        Map<String, Object> a = new HashMap<>();
        a.put("nome", "Ana Souza");
        a.put("interesse", "Kit Presépio");
        a.put("quantidade", "2");
        a.put("entrega", "Retirada");
        return a;
    }

    private void stubContactCreation() {
        ContactResponse response = mock(ContactResponse.class);
        when(response.id()).thenReturn(newContactId);
        when(contactUseCase.create(eq(companyId), any(), any())).thenReturn(response);
    }

    @Test
    void exposeFerramentaComCamposObrigatorios() {
        assertTrue(provider.handles("registrar_lead"));
        assertTrue(provider.isAvailable(mock(AgentConfig.class), new AgentToolSession(companyId, null,
                UUID.randomUUID(), null, null, null, null)));
    }

    @Test
    void instagramCriaContatoVinculaConversaELeadComOrigemInstagram() {
        Conversation c = conversation(ChannelType.INSTAGRAM, null, "1784000111");
        stubContactCreation();

        String result = provider.execute(session(c), call(args()));

        assertEquals("Lead registrado. Continue o atendimento normalmente.", result);
        ArgumentCaptor<CreateContactRequest> contact = ArgumentCaptor.forClass(CreateContactRequest.class);
        verify(contactUseCase).create(eq(companyId), contact.capture(), any());
        assertEquals("Ana", contact.getValue().firstName());
        assertEquals("Souza", contact.getValue().lastName());
        assertNull(contact.getValue().mobile(), "o id do Instagram não é telefone");
        assertEquals(newContactId, c.getContactId());
        verify(conversations).save(c);

        ArgumentCaptor<CreateLeadRequest> lead = ArgumentCaptor.forClass(CreateLeadRequest.class);
        verify(leadUseCase).create(eq(companyId), lead.capture(), any());
        assertEquals(LeadSource.INSTAGRAM, lead.getValue().source());
        assertEquals(newContactId, lead.getValue().contactId());
        assertTrue(lead.getValue().notes().contains("Interesse: Kit Presépio"));
        assertTrue(lead.getValue().notes().contains("Quantidade: 2"));
    }

    @Test
    void whatsappUsaTelefoneDaConversaEOrigemWhatsapp() {
        Conversation c = conversation(ChannelType.WHATSAPP, null, "5534999998888");
        when(contactRepository.findByCompanyIdAndPhone(eq(companyId), any())).thenReturn(Optional.empty());
        stubContactCreation();

        provider.execute(session(c), call(args()));

        ArgumentCaptor<CreateContactRequest> contact = ArgumentCaptor.forClass(CreateContactRequest.class);
        verify(contactUseCase).create(eq(companyId), contact.capture(), any());
        assertEquals("5534999998888", contact.getValue().mobile());
        ArgumentCaptor<CreateLeadRequest> lead = ArgumentCaptor.forClass(CreateLeadRequest.class);
        verify(leadUseCase).create(eq(companyId), lead.capture(), any());
        assertEquals(LeadSource.WHATSAPP, lead.getValue().source());
    }

    @Test
    void conversaComContatoNaoCriaOutroELeadExistenteNaoDuplica() {
        UUID existing = UUID.randomUUID();
        Conversation c = conversation(ChannelType.INSTAGRAM, existing, "1784000111");
        when(leadRepository.existsByContactIdAndCompanyId(existing, companyId)).thenReturn(true);

        String result = provider.execute(session(c), call(args()));

        assertTrue(result.contains("já é um lead"));
        verify(contactUseCase, never()).create(any(), any(), any());
        verify(leadUseCase, never()).create(any(), any(), any());
    }

    @Test
    void semNomeOuInteressePedeOsDados() {
        Conversation c = conversation(ChannelType.INSTAGRAM, null, "1784000111");

        String result = provider.execute(session(c), call(Map.of("nome", "Ana")));

        assertTrue(result.startsWith("Faltam dados"));
        verify(leadUseCase, never()).create(any(), any(), any());
    }

    @Test
    void falhaNoCadastroNaoLancaExcecao() {
        Conversation c = conversation(ChannelType.INSTAGRAM, null, "1784000111");
        when(contactUseCase.create(eq(companyId), any(), any())).thenThrow(new IllegalStateException("boom"));

        String result = provider.execute(session(c), call(args()));

        assertTrue(result.startsWith("Não foi possível registrar"));
        assertFalse(result.contains("boom"));
    }

    @Test
    void notasSaoLimitadasA1000Caracteres() {
        Map<String, Object> a = args();
        a.put("observacoes", "x".repeat(2000));

        String notes = LeadCaptureToolProvider.notes(a, ChannelType.INSTAGRAM);

        assertEquals(LeadCaptureToolProvider.MAX_NOTES, notes.length());
        assertTrue(notes.startsWith("Pedido via Instagram"));
    }

    private Lead lead(LeadStatus status, String notes) {
        Lead lead = mock(Lead.class);
        when(lead.getId()).thenReturn(UUID.randomUUID());
        when(lead.getStatus()).thenReturn(status);
        when(lead.getNotes()).thenReturn(notes);
        return lead;
    }

    @Test
    void atualizarLeadQualificadoAcrescentaResumoNasNotas() {
        UUID contact = UUID.randomUUID();
        Conversation c = conversation(ChannelType.INSTAGRAM, contact, "1784000111");
        Lead lead = lead(LeadStatus.NEW, "Pedido via Instagram (agente)");
        when(leadRepository.findByContactIdAndCompanyId(contact, companyId)).thenReturn(Optional.of(lead));

        String result = provider.execute(session(c), new AiProvider.ToolCall("c2", LeadCaptureToolProvider.UPDATE_LEAD,
                Map.of("situacao", "qualificado", "resumo", "2 Kit Presépio, retirada dia 20")));

        assertEquals("Lead atualizado. Continue o atendimento normalmente.", result);
        ArgumentCaptor<UpdateLeadRequest> update = ArgumentCaptor.forClass(UpdateLeadRequest.class);
        verify(leadUseCase).update(eq(companyId), eq(lead.getId()), update.capture());
        assertEquals(LeadStatus.QUALIFIED, update.getValue().status());
        assertEquals("Pedido via Instagram (agente)\nPedido completo: 2 Kit Presépio, retirada dia 20",
                update.getValue().notes());
    }

    @Test
    void atualizarLeadPerdidoESemLeadPedeRegistro() {
        UUID contact = UUID.randomUUID();
        Conversation c = conversation(ChannelType.WHATSAPP, contact, "5534999998888");
        when(leadRepository.findByContactIdAndCompanyId(contact, companyId)).thenReturn(Optional.empty());

        String result = provider.execute(session(c), new AiProvider.ToolCall("c3", LeadCaptureToolProvider.UPDATE_LEAD,
                Map.of("situacao", "perdido", "resumo", "achou caro")));

        assertTrue(result.contains("registrar_lead"));
        verify(leadUseCase, never()).update(any(), any(), any());
    }

    @Test
    void atualizarLeadIgnoraConvertidoESituacaoInvalida() {
        UUID contact = UUID.randomUUID();
        Conversation c = conversation(ChannelType.INSTAGRAM, contact, "1784000111");
        Lead lead = lead(LeadStatus.CONVERTED, null);
        when(leadRepository.findByContactIdAndCompanyId(contact, companyId)).thenReturn(Optional.of(lead));

        assertTrue(provider.execute(session(c), new AiProvider.ToolCall("c4", LeadCaptureToolProvider.UPDATE_LEAD,
                Map.of("situacao", "perdido", "resumo", "x"))).contains("já está atualizado"));
        assertTrue(provider.execute(session(c), new AiProvider.ToolCall("c5", LeadCaptureToolProvider.UPDATE_LEAD,
                Map.of("situacao", "talvez", "resumo", "x"))).startsWith("Situação inválida"));
        verify(leadUseCase, never()).update(any(), any(), any());
    }

    @Test
    void appendNoteMantemOsMaisRecentesNoLimite() {
        String merged = LeadCaptureToolProvider.appendNote("a".repeat(990), "Desistência: caro");
        assertEquals(LeadCaptureToolProvider.MAX_NOTES, merged.length());
        assertTrue(merged.endsWith("Desistência: caro"));
    }
}
