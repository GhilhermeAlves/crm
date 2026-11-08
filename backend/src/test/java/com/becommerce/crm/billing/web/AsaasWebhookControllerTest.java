package com.becommerce.crm.billing.web;

import com.becommerce.crm.billing.application.AsaasWebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AsaasWebhookControllerTest {
    private static final String URL = "/api/v1/public/billing/asaas/webhook";
    private final AsaasWebhookService service = mock(AsaasWebhookService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AsaasWebhookController(service)).build();

    @Test
    void invalidTokenIs401() throws Exception {
        when(service.tokenMatches("bad")).thenReturn(false);
        mvc.perform(post(URL).header("asaas-access-token", "bad")
                        .contentType("application/json").content("{\"id\":\"evt_1\"}"))
           .andExpect(status().isUnauthorized());
        verify(service, never()).handle(any());
    }

    @Test
    void validTokenIs200() throws Exception {
        when(service.tokenMatches("ok")).thenReturn(true);
        mvc.perform(post(URL).header("asaas-access-token", "ok")
                        .contentType("application/json").content("{\"id\":\"evt_1\",\"event\":\"PAYMENT_RECEIVED\"}"))
           .andExpect(status().isOk());
        verify(service).handle(any());
    }
}
