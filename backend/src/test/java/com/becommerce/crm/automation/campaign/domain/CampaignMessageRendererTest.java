package com.becommerce.crm.automation.campaign.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CampaignMessageRendererTest {

    private static final CampaignMessageRenderer.Profile MARIA =
            new CampaignMessageRenderer.Profile("Maria Clara", "Souza", "maria@x.com", "11999990000");

    @Test
    void substituiVariaveisDoContato() {
        String out = CampaignMessageRenderer.render(
                "Olá {{primeiroNome}}! {{nome}} | {{ sobrenome }} | {{email}} | {{telefone}}", MARIA);
        assertThat(out).isEqualTo("Olá Maria! Maria Clara Souza | Souza | maria@x.com | 11999990000");
    }

    @Test
    void variavelDesconhecidaOuSemContatoViraVazio() {
        assertThat(CampaignMessageRenderer.render("A{{xyz}}B", MARIA)).isEqualTo("AB");
        assertThat(CampaignMessageRenderer.render("Olá {{nome}}!", null)).isEqualTo("Olá !");
    }

    @Test
    void sobrenomeNuloNaoDeixaEspaco() {
        var p = new CampaignMessageRenderer.Profile("João", null, null, null);
        assertThat(CampaignMessageRenderer.render("[{{nome}}]", p)).isEqualTo("[João]");
    }

    @Test
    void valorComCifraoNaoQuebraSubstituicao() {
        var p = new CampaignMessageRenderer.Profile("R$ Zé", null, null, null);
        assertThat(CampaignMessageRenderer.render("{{nome}}", p)).isEqualTo("R$ Zé");
    }
}
