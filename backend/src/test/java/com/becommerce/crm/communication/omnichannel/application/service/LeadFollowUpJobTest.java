package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.communication.omnichannel.application.service.LeadFollowUpJob.MarkLost;
import com.becommerce.crm.communication.omnichannel.application.service.LeadFollowUpJob.Nothing;
import com.becommerce.crm.communication.omnichannel.application.service.LeadFollowUpJob.SendFollowUp;
import com.becommerce.crm.sales.followup.application.port.in.FollowUpUseCase;
import com.becommerce.crm.sales.lead.application.port.in.LeadUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class LeadFollowUpJobTest {

    private final LeadFollowUpJob job = new LeadFollowUpJob(mock(NamedParameterJdbcTemplate.class),
            mock(FollowUpUseCase.class), mock(LeadUseCase.class), true, 20, 48, 24, 72, 168);

    @Test
    void instagramUmFollowUpAntesDe24hEPerdidoDepoisDe48h() {
        LeadFollowUpJob.Plan ig = job.planFor("INSTAGRAM");
        assertInstanceOf(Nothing.class, LeadFollowUpJob.decide(ig, 5));
        SendFollowUp first = assertInstanceOf(SendFollowUp.class, LeadFollowUpJob.decide(ig, 21));
        assertEquals(1, first.step());
        assertInstanceOf(SendFollowUp.class, LeadFollowUpJob.decide(ig, 40));
        assertInstanceOf(MarkLost.class, LeadFollowUpJob.decide(ig, 48));
    }

    @Test
    void whatsappDoisFollowUpsEPerdidoEmSeteDias() {
        LeadFollowUpJob.Plan wa = job.planFor("WHATSAPP");
        assertInstanceOf(Nothing.class, LeadFollowUpJob.decide(wa, 23));
        assertEquals(1, assertInstanceOf(SendFollowUp.class, LeadFollowUpJob.decide(wa, 24)).step());
        assertEquals(2, assertInstanceOf(SendFollowUp.class, LeadFollowUpJob.decide(wa, 80)).step());
        assertInstanceOf(MarkLost.class, LeadFollowUpJob.decide(wa, 170));
    }

    @Test
    void personalizaComPrimeiroNomeOuSemNome() {
        assertEquals("Oi, Ana! Tudo bem?", LeadFollowUpJob.personalize("Oi, {nome}! Tudo bem?", " Ana "));
        assertEquals("Oi! Tudo bem?", LeadFollowUpJob.personalize("Oi, {nome}! Tudo bem?", null));
    }
}
