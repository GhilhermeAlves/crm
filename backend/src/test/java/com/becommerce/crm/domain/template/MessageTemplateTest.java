package com.becommerce.crm.domain.template;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Variáveis e versionamento de templates (Sprint 17). */
class MessageTemplateTest {

    @Test
    void extractsDeclaredVariables() {
        MessageTemplate t = MessageTemplate.create(UUID.randomUUID(), "Boas-vindas",
                "WHATSAPP", null, "Olá {{primeiroNome}}, seu e-mail é {{email}}!", null, null);
        assertEquals(java.util.List.of("primeiroNome", "email"), t.extractVariables());
    }

    @Test
    void rendersVariablesWithValues() {
        MessageTemplate t = MessageTemplate.create(UUID.randomUUID(), "T", "WHATSAPP", null,
                "Olá {{nome}}!", null, null);
        assertEquals("Olá Maria!", t.render(Map.of("nome", "Maria")));
    }

    @Test
    void missingVariableRendersEmpty() {
        MessageTemplate t = MessageTemplate.create(UUID.randomUUID(), "T", "WHATSAPP", null,
                "Olá {{nome}}, tudo bem?", null, null);
        assertEquals("Olá , tudo bem?", t.render(Map.of()));
    }

    @Test
    void updateContentIncrementsVersion() {
        MessageTemplate t = MessageTemplate.create(UUID.randomUUID(), "T", "WHATSAPP", null,
                "v1", null, null);
        int v1 = t.getVersion();
        t.updateContent(null, "v2 {{var}}", null);
        assertEquals(v1 + 1, t.getVersion());
        assertTrue(t.extractVariables().contains("var"));
    }

    @Test
    void archiveDeactivatesTemplate() {
        MessageTemplate t = MessageTemplate.create(UUID.randomUUID(), "T", "WHATSAPP", null,
                "corpo", null, null);
        t.archive();
        assertEquals(TemplateStatus.ARCHIVED, t.getStatus());
    }

    @Test
    void createRequiresNameAndBody() {
        UUID companyId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> MessageTemplate.create(companyId, "", "WHATSAPP", null, "corpo", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> MessageTemplate.create(companyId, "Nome", "WHATSAPP", null, "", null, null));
    }
}
