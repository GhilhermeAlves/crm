package com.becommerce.crm.automation.ai.application.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiScreenCatalogTest {

    @Test
    void findsByLongestPrefix() {
        assertEquals("Agenda", AiScreenCatalog.find("/agenda").name());
        assertEquals("Conciliação", AiScreenCatalog.find("/finance/reconciliation").name());
        assertEquals("Gestão financeira", AiScreenCatalog.find("/finance").name());
        assertEquals("Contatos", AiScreenCatalog.find("/contacts/123").name());
        assertEquals("Estoque", AiScreenCatalog.find("/estoque").name());
        assertEquals("Config. Agenda", AiScreenCatalog.find("/settings/agenda/availability").name());
    }

    @Test
    void unknownRouteIsNull() {
        assertNull(AiScreenCatalog.find("/nao-existe"));
        assertNull(AiScreenCatalog.find("/agendamentos"));
        assertNull(AiScreenCatalog.find(null));
    }

    @Test
    void rendersCatalog() {
        assertTrue(AiScreenCatalog.render().contains("- Agenda — /agenda: "));
    }
}
