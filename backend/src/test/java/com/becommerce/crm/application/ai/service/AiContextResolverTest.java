package com.becommerce.crm.application.ai.service;

import com.becommerce.crm.application.ai.dto.AiContextPayload;
import com.becommerce.crm.application.customer360.dto.ContactSummaryResponse;
import com.becommerce.crm.application.customer360.dto.Customer360Response;
import com.becommerce.crm.application.customer360.dto.NextActionResponse;
import com.becommerce.crm.application.customer360.dto.OpportunityItemResponse;
import com.becommerce.crm.application.customer360.dto.TaskItemResponse;
import com.becommerce.crm.application.customer360.service.Customer360Service;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;
import com.becommerce.crm.domain.task.TaskPriority;
import com.becommerce.crm.domain.task.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiContextResolverTest {

    @Mock Customer360Service customer360Service;

    private final UUID companyId = UUID.randomUUID();
    private final UUID contactId = UUID.randomUUID();

    @Test
    void shouldBuildCustomerContextForCustomerScreen() {
        Customer360Response c360 = new Customer360Response(
                companyId,
                new ContactSummaryResponse(contactId, "João Silva", "joao@x.com", "+5511999999999",
                        "cliente importante", "JS", LocalDateTime.now(), LocalDateTime.now().minusDays(1),
                        true, "Sem interação há 8 dias"),
                2,
                new BigDecimal("15000.00"),
                List.of(new OpportunityItemResponse(UUID.randomUUID(), "Proposta A", new BigDecimal("10000"),
                        "Proposta", 70, OpportunityStatus.OPEN, "ABERTA", "Funil Principal", UUID.randomUUID(),
                        LocalDateTime.now().plusDays(10))),
                List.of(new TaskItemResponse(UUID.randomUUID(), "Ligar para João", TaskStatus.PENDING,
                        TaskPriority.HIGH, LocalDateTime.now().plusDays(1), UUID.randomUUID(), null, false)),
                List.of(),
                new NextActionResponse("FOLLOW_UP", "Agendar follow-up", "Retome o contato.", 90));

        when(customer360Service.build(eq(companyId), eq(contactId))).thenReturn(c360);

        String context = new AiContextResolver(customer360Service).resolve(companyId,
                new AiContextPayload("customer360", contactId));

        assertNotNull(context);
        assertTrue(context.contains("João Silva"));
        assertTrue(context.contains("Oportunidades abertas: 2"));
        assertTrue(context.contains("15000"));
        assertTrue(context.contains("Proposta A"));
        assertTrue(context.contains("Risco: ALTO"));
    }

    @Test
    void shouldReturnNullWhenNoRecordId() {
        String context = new AiContextResolver(customer360Service).resolve(companyId,
                new AiContextPayload("customer360", null));
        assertNull(context);
        verify(customer360Service, never()).build(any(), any());
    }

    @Test
    void shouldReturnNullWhenScreenNotSupported() {
        String context = new AiContextResolver(customer360Service).resolve(companyId,
                new AiContextPayload("pipeline", contactId));
        assertNull(context);
        verify(customer360Service, never()).build(any(), any());
    }

    @Test
    void shouldReturnNullWhenNullContext() {
        assertNull(new AiContextResolver(customer360Service).resolve(companyId, null));
    }
}