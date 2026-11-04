package com.becommerce.crm.communication.notification.application;

import com.becommerce.crm.communication.notification.application.InvitationEmailTemplate.Custom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvitationEmailTemplateTest {

    @Test
    void memberInviteShowsCompanyRoleLogoAndLinkWithoutWatermark() {
        String html = InvitationEmailTemplate.html("https://crm.pg.com/", "Clínica <X>", "MANAGER",
                "https://crm.pg.com/convite/abc");

        assertTrue(html.contains("https://crm.pg.com/brand/pg-automacao-email.png"));
        assertFalse(html.contains("watermark"));
        assertTrue(html.contains("Clínica &lt;X&gt;"));
        assertTrue(html.contains("Gerente"));
        assertTrue(html.contains("href=\"https://crm.pg.com/convite/abc\""));
        assertTrue(html.contains("Como começar em 4 passos"));
    }

    @Test
    void companyInviteUsesAccountOpeningText() {
        String html = InvitationEmailTemplate.html("https://crm.pg.com", "", "ADMIN",
                "https://crm.pg.com/convite-empresa/xyz");

        assertTrue(html.contains("abrir a conta da sua empresa"));
        assertFalse(html.contains("com o perfil"));
        assertTrue(InvitationEmailTemplate.subject("").contains("abra a conta da sua empresa"));
        assertTrue(InvitationEmailTemplate.subject("Clínica").endsWith("Clínica"));
    }

    @Test
    void plainInviteHasNoPgBranding() {
        String member = InvitationEmailTemplate.plainHtml("Clínica", "AGENT", "https://crm/convite/a");
        String company = InvitationEmailTemplate.plainHtml("", "ADMIN", "https://crm/convite-empresa/b");

        assertFalse(member.contains("PG Automação"));
        assertTrue(member.contains("<strong>Clínica</strong>"));
        assertTrue(company.contains("abrir a conta da sua empresa"));
        assertFalse(InvitationEmailTemplate.plainSubject("Clínica").contains("PG"));
    }

    @Test
    void customInviteUsesCompanyFieldsAndEscapesMessage() {
        Custom custom = new Custom("cid:invite-logo", "#AA2233", "Olá, time!",
                "Bem-vindo à {empresa} como {perfil}.\n<script>x</script>", "Clínica Bela • Uberlândia");

        String html = InvitationEmailTemplate.customHtml(custom, "Clínica Bela", "Clínica Bela", "AGENT",
                "https://crm/convite/a");

        assertTrue(html.contains("src=\"cid:invite-logo\""));
        assertTrue(html.contains("#aa2233"));
        assertTrue(html.contains("Olá, time!"));
        assertTrue(html.contains("Bem-vindo à <strong>Clínica Bela</strong> como <strong>Agente</strong>.<br>"));
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("Clínica Bela • Uberlândia"));
        assertFalse(html.contains("PG Automação"));
        assertTrue(html.contains("Como começar em 4 passos"));
        assertTrue(html.contains("href=\"https://crm/convite/a\""));
    }

    @Test
    void customInviteFallsBackToDefaults() {
        String html = InvitationEmailTemplate.customHtml(new Custom(null, "vermelho", null, null, null),
                "Clínica Bela", "", "ADMIN", "https://crm/convite-empresa/b");

        assertTrue(html.contains(InvitationEmailTemplate.DEFAULT_COLOR));
        assertTrue(html.contains(InvitationEmailTemplate.DEFAULT_TITLE));
        assertTrue(html.contains("abrir a conta da sua empresa"));
        assertFalse(html.contains("<img"));
        assertEquals("Bem-vindo(a) a Clínica Bela: abra a conta da sua empresa",
                InvitationEmailTemplate.customSubject("Clínica Bela", ""));
    }
}
