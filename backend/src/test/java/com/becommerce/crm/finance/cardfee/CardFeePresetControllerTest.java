package com.becommerce.crm.finance.cardfee;

import com.becommerce.crm.finance.cardfee.CardFeeDtos.PresetResponse;
import com.becommerce.crm.finance.cardfee.CardFeeDtos.RateItem;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import com.becommerce.crm.shared.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CardFeePresetControllerTest {

    private final CardFeePresetService service = mock(CardFeePresetService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new CardFeePresetController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();

    private static final String BODY = "{\"name\":\"X\",\"estimated\":true,\"rates\":[]}";

    private void loginAs(String role) {
        UUID company = UUID.randomUUID();
        CurrentUser u = new CurrentUser(UUID.randomUUID(), "x@y.com", company, company,
                List.of(role), List.of(), "sub", null, "keycloak", "X", role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, List.of()));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private static PresetResponse preset(UUID id) {
        return new PresetResponse(id, "Ton", true, LocalDate.of(2026, 11, 13), null, true,
                List.of(new RateItem(1, new BigDecimal("3.86"))));
    }

    @Test
    void anyLoggedUserListsActivePresets() throws Exception {
        loginAs("USER");
        UUID id = UUID.randomUUID();
        when(service.listActive()).thenReturn(List.of(preset(id)));
        mvc.perform(get("/api/v1/card-fee-presets"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].id").value(id.toString()))
           .andExpect(jsonPath("$[0].rates[0].ratePercent").value(3.86));
    }

    @Test
    void crmAdminCreatesUpdatesAndDeactivates() throws Exception {
        loginAs("SUPER_ADMIN");
        UUID id = UUID.randomUUID();
        when(service.create(any())).thenReturn(preset(id));
        when(service.update(eq(id), any())).thenReturn(preset(id));

        mvc.perform(post("/api/v1/admin/card-fee-presets").contentType(MediaType.APPLICATION_JSON).content(BODY))
           .andExpect(status().isCreated());
        mvc.perform(put("/api/v1/admin/card-fee-presets/" + id).contentType(MediaType.APPLICATION_JSON).content(BODY))
           .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/admin/card-fee-presets/" + id))
           .andExpect(status().isNoContent());
        verify(service).deactivate(id);
    }

    @Test
    void clinicAdminCannotManagePresets() throws Exception {
        loginAs("ADMIN");
        UUID id = UUID.randomUUID();
        mvc.perform(get("/api/v1/admin/card-fee-presets")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/card-fee-presets").contentType(MediaType.APPLICATION_JSON).content(BODY))
           .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/admin/card-fee-presets/" + id).contentType(MediaType.APPLICATION_JSON).content(BODY))
           .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/admin/card-fee-presets/" + id)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
