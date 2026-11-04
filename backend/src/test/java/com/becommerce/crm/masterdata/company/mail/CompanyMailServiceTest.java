package com.becommerce.crm.masterdata.company.mail;

import com.becommerce.crm.masterdata.company.mail.CompanyMailDtos.InviteTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyMailServiceTest {

    @Test
    void deducesServerFromCommonProviders() {
        assertThat(CompanyMailService.knownServer("clinica@Gmail.com"))
                .contains(new CompanyMailService.Server("smtp.gmail.com", 587));
        assertThat(CompanyMailService.knownServer("contato@hotmail.com"))
                .contains(new CompanyMailService.Server("smtp-mail.outlook.com", 587));
        assertThat(CompanyMailService.knownServer("contato@minhaclinica.com.br")).isEmpty();
    }

    @Test
    void explainsAuthenticationFailure() {
        assertThat(CompanyMailService.explain(new RuntimeException("535 Authentication failed")))
                .contains("senha de app");
    }

    @Test
    void cipherRoundTripsAndRejectsOtherKey() {
        MailPasswordCipher cipher = new MailPasswordCipher("chave-a", "");
        String stored = cipher.encrypt("abcd efgh");
        assertThat(stored).doesNotContain("abcd");
        assertThat(cipher.decrypt(stored)).isEqualTo("abcd efgh");
        assertThatThrownBy(() -> new MailPasswordCipher("chave-b", "").decrypt(stored))
                .isInstanceOf(IllegalStateException.class);
    }

    private static final String PNG = "data:image/png;base64,"
            + Base64.getEncoder().encodeToString(new byte[] {(byte) 0x89, 'P', 'N', 'G'});

    private final CompanyMailService service =
            new CompanyMailService(null, null, "https://crm.test", "pg@x.com");

    @Test
    void sanitizeTrimsBlanksAndLowercasesColor() {
        InviteTemplate clean = CompanyMailService.sanitize(
                new InviteTemplate(true, "  ", " #AABBCC ", " Olá ", "", null));

        assertThat(clean.logo()).isNull();
        assertThat(clean.color()).isEqualTo("#aabbcc");
        assertThat(clean.title()).isEqualTo("Olá");
        assertThat(clean.message()).isNull();
    }

    @Test
    void sanitizeRejectsInvalidColorAndLogo() {
        assertThatThrownBy(() -> CompanyMailService.sanitize(
                new InviteTemplate(true, null, "azul", null, null, null)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Cor");
        assertThatThrownBy(() -> CompanyMailService.sanitize(
                new InviteTemplate(true, "data:image/svg+xml;base64,PHN2Zz4=", null, null, null, null)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("PNG ou JPEG");
    }

    @Test
    void rejectsLogoOver500Kb() {
        String big = "data:image/png;base64," + Base64.getEncoder().encodeToString(new byte[500 * 1024 + 1]);
        assertThatThrownBy(() -> CompanyMailService.decodeLogo(big))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("500 KB");
    }

    @Test
    void customTemplateAttachesLogoInlineWhenSending() {
        CompanyMailService.InviteMail mail = service.buildInvite("clinica@x.com", "Clínica Bela",
                new InviteTemplate(true, PNG, "#aa2233", "Oi!", null, null), "Clínica Bela", "AGENT",
                "https://crm.test/convite/a", false);

        assertThat(mail.html()).contains("cid:" + CompanyMailService.INVITE_LOGO_CID).contains("#aa2233");
        assertThat(mail.logo()).isNotNull();
        assertThat(mail.logo().contentType()).isEqualTo("image/png");
        assertThat(mail.subject()).contains("Clínica Bela");
    }

    @Test
    void previewEmbedsLogoAsDataUrlWithoutAttachment() {
        CompanyMailService.InviteMail mail = service.buildInvite("clinica@x.com", "Clínica Bela",
                new InviteTemplate(true, PNG, null, null, null, null), "Clínica Bela", "AGENT",
                "https://crm.test/convite/a", true);

        assertThat(mail.html()).contains(PNG).doesNotContain("cid:");
        assertThat(mail.logo()).isNull();
    }

    @Test
    void disabledTemplateFallsBackToPgOrPlain() {
        InviteTemplate off = new InviteTemplate(false, PNG, "#aa2233", "Oi!", null, null);

        assertThat(service.buildInvite("PG@x.com", null, off, "Empresa", "AGENT", "u", false).html())
                .contains("PG Automação");
        assertThat(service.buildInvite("outra@x.com", null, off, "Empresa", "AGENT", "u", false).html())
                .doesNotContain("PG Automação").contains("Aceitar convite");
    }
}
