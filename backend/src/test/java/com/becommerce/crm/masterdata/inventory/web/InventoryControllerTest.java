package com.becommerce.crm.masterdata.inventory.web;

import com.becommerce.crm.masterdata.inventory.application.dto.RegisterMovementRequest;
import com.becommerce.crm.masterdata.inventory.application.dto.StockMovementResponse;
import com.becommerce.crm.masterdata.inventory.application.service.InventoryService;
import com.becommerce.crm.masterdata.inventory.domain.StockMovementType;
import com.becommerce.crm.shared.application.dto.PageResponse;
import com.becommerce.crm.shared.security.config.CurrentCompanyIdArgumentResolver;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import com.becommerce.crm.shared.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock private InventoryService inventoryService;
    @InjectMocks private InventoryController inventoryController;

    private MockMvc mockMvc;
    private UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(inventoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new CurrentCompanyIdArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void login(UUID activeCompanyId) {
        CurrentUser principal = new CurrentUser(UUID.randomUUID(), "admin@empresa.com",
                activeCompanyId, activeCompanyId, List.of("ADMIN"),
                List.of("inventory:read", "inventory:manage"), "k", null, "keycloak", "Admin", null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null,
                principal.permissions().stream().map(SimpleGrantedAuthority::new).toList()));
    }

    @Test
    void registersMovementInOwnCompany() throws Exception {
        login(companyId);
        when(inventoryService.registerMovement(any(), any(), any(RegisterMovementRequest.class)))
                .thenReturn(new StockMovementResponse(UUID.randomUUID(), UUID.randomUUID(),
                        StockMovementType.ENTRADA, 10, "Compra", 10, java.time.LocalDateTime.now()));

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/inventory/" + UUID.randomUUID() + "/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ENTRADA\",\"quantity\":10,\"note\":\"Compra\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.delta").value(10))
                .andExpect(jsonPath("$.balanceAfter").value(10));
    }

    @Test
    void listsInventoryOfOwnCompany() throws Exception {
        login(companyId);
        when(inventoryService.listProducts(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/companies/" + companyId + "/inventory"))
                .andExpect(status().isOk());
    }

    @Test
    void returns403ForOtherCompany() throws Exception {
        UUID other = UUID.randomUUID();
        login(companyId);

        mockMvc.perform(post("/api/v1/companies/" + other + "/inventory/" + UUID.randomUUID() + "/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ENTRADA\",\"quantity\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CRM_ACCESS_DENIED"));
    }
}