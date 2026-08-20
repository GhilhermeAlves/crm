package com.becommerce.crm.presentation.rest.ai;

import com.becommerce.crm.application.ai.dto.AiAnalysisRequest;
import com.becommerce.crm.application.ai.dto.AiAnalysisResponse;
import com.becommerce.crm.application.ai.port.input.AiContextualAnalysisUseCase;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AiAnalysisControllerTest {

    private final AiContextualAnalysisUseCase useCase = mock(AiContextualAnalysisUseCase.class);
    private final AiAnalysisController controller = new AiAnalysisController(useCase);

    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final List<String> permissions = List.of("ai:chat", "opportunity:read");

    private CurrentUser principal() {
        return CurrentUser.fromKeycloak(userId, "user@e.com", companyId,
                List.of("MANAGER"), permissions, "keycloak-sub", "User");
    }

    @Test
    void shouldDeriveIdentityFromCurrentUserNotRequest() {
        AiAnalysisResponse expected = new AiAnalysisResponse("resumo", List.of(), List.of(), List.of());
        when(useCase.analyze(eq(companyId), eq(userId), eq(permissions), any()))
                .thenReturn(expected);

        AiAnalysisRequest request = new AiAnalysisRequest("Resuma.",
                new com.becommerce.crm.application.ai.dto.AiContextPayload("opportunity",
                        "/opportunities/x", "OPPORTUNITY", UUID.randomUUID()));

        var response = controller.analyze(request, principal());

        assertEquals(expected, response.getBody());
        verify(useCase).analyze(companyId, userId, permissions, request);
    }

    @Test
    void shouldRejectWhenNoActiveCompany() {
        CurrentUser noCompany = CurrentUser.fromKeycloak(userId, "user@e.com", null,
                List.of("MANAGER"), permissions, "keycloak-sub", "User");
        assertThrows(CrmAccessDeniedException.class,
                () -> controller.analyze(new AiAnalysisRequest("oi", null), noCompany));
        verify(useCase, never()).analyze(any(), any(), any(), any());
    }
}