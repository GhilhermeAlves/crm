package com.becommerce.crm.communication.notification.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvitationEmailTemplateTest {

    @Test
    void memberInviteShowsCompanyRoleLogoAndLink() {
        String html = InvitationEmailTemplate.html("https://crm.pg.com/", "Clínica <X>", "MANAGER",
                "https://crm.pg.com/convite/abc");

        assertTrue(html.contains("https://crm.pg.com/brand/pg-automacao-email.png"));
        assertTrue(html.contains("https://crm.pg.com/brand/pg-automacao-watermark.png"));
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
}
