package com.becommerce.crm.masterdata.company.mail;

import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.domain.AiProviderException;
import com.becommerce.crm.masterdata.company.mail.CompanyMailDtos.InviteSuggestion;
import com.becommerce.crm.masterdata.company.mail.CompanyMailDtos.InviteSuggestionRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InviteTemplateLeoServiceTest {

    private static final UUID COMPANY = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();

    @SuppressWarnings("unchecked")
    private static InviteTemplateLeoService service(AiProvider provider) {
        ObjectProvider<AiProvider> holder = mock(ObjectProvider.class);
        when(holder.getIfAvailable()).thenReturn(provider);
        return new InviteTemplateLeoService(holder);
    }

    @Test
    void suggestsTextsFromModelJsonWithToneAndNotes() {
        AiProvider provider = mock(AiProvider.class);
        when(provider.chatStructured(any())).thenReturn("""
                ```json
                {"title":"Bem-vinda!","message":"Que bom ter você na {empresa} como {perfil}.","footer":"Clínica Bela"}
                ```""");

        InviteSuggestion s = service(provider).suggest(COMPANY, USER, "Clínica Bela",
                new InviteSuggestionRequest("PROFISSIONAL", "Clínica de estética em Uberlândia"));

        assertThat(s.title()).isEqualTo("Bem-vinda!");
        assertThat(s.message()).contains("{empresa}").contains("{perfil}");
        assertThat(s.footer()).isEqualTo("Clínica Bela");
        ArgumentCaptor<AiProvider.ChatRequest> captor = ArgumentCaptor.forClass(AiProvider.ChatRequest.class);
        verify(provider).chatStructured(captor.capture());
        String user = captor.getValue().messages().get(1).content();
        assertThat(user).contains("Clínica Bela").contains("profissional").contains("Uberlândia");
    }

    @Test
    void clipsOversizedFields() {
        AiProvider provider = mock(AiProvider.class);
        when(provider.chatStructured(any())).thenReturn(
                "{\"title\":\"" + "a".repeat(300) + "\",\"message\":\"m\",\"footer\":\"f\"}");

        InviteSuggestion s = service(provider).suggest(COMPANY, USER, null, new InviteSuggestionRequest(null, null));

        assertThat(s.title()).hasSize(150);
    }

    @Test
    void unavailableWithoutProvider() {
        InviteTemplateLeoService leo = service(null);

        assertThat(leo.isAvailable()).isFalse();
        assertThatThrownBy(() -> leo.suggest(COMPANY, USER, "X", new InviteSuggestionRequest(null, null)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("não está disponível");
    }

    @Test
    void providerFailureAndBadJsonBecomeFriendlyErrors() {
        AiProvider failing = mock(AiProvider.class);
        when(failing.chatStructured(any())).thenThrow(new AiProviderException("timeout"));
        assertThatThrownBy(() -> service(failing).suggest(COMPANY, USER, "X",
                new InviteSuggestionRequest(null, null)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Tente de novo");

        AiProvider garbage = mock(AiProvider.class);
        when(garbage.chatStructured(any())).thenReturn("não sei");
        assertThatThrownBy(() -> service(garbage).suggest(COMPANY, USER, "X",
                new InviteSuggestionRequest(null, null)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("formato inesperado");
    }
}
