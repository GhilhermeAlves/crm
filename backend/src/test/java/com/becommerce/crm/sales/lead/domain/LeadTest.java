package com.becommerce.crm.sales.lead.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LeadTest {

    private Lead lead(LeadStatus status) {
        return Lead.create(UUID.randomUUID(), UUID.randomUUID(), LeadSource.INSTAGRAM, status, 0, null,
                null, null, null);
    }

    @Test
    void perdidoReabreComoNovo() {
        Lead lead = lead(LeadStatus.LOST);
        lead.transitionTo(LeadStatus.NEW);
        assertEquals(LeadStatus.NEW, lead.getStatus());
    }

    @Test
    void perdidoPodeManterOMesmoStatus() {
        Lead lead = lead(LeadStatus.LOST);
        lead.transitionTo(LeadStatus.LOST);
        assertEquals(LeadStatus.LOST, lead.getStatus());
    }

    @Test
    void perdidoNaoPulaDiretoParaQualificado() {
        assertThrows(IllegalStateException.class, () -> lead(LeadStatus.LOST).transitionTo(LeadStatus.QUALIFIED));
    }

    @Test
    void novoViraPerdido() {
        Lead lead = lead(LeadStatus.NEW);
        lead.transitionTo(LeadStatus.LOST);
        assertEquals(LeadStatus.LOST, lead.getStatus());
    }

    @Test
    void convertidoNaoMuda() {
        assertThrows(IllegalStateException.class, () -> lead(LeadStatus.CONVERTED).transitionTo(LeadStatus.NEW));
    }
}
