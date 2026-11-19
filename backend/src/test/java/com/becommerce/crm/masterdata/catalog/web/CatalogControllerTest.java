package com.becommerce.crm.masterdata.catalog.web;

import com.becommerce.crm.masterdata.catalog.application.dto.CatalogLookupResponse;
import com.becommerce.crm.masterdata.catalog.application.service.CatalogService;
import com.becommerce.crm.shared.application.dto.PageResponse;
import com.becommerce.crm.shared.security.authorization.CurrentUserAuthorities;
import com.becommerce.crm.shared.security.authorization.PermissionEvaluatorBean;
import com.becommerce.crm.shared.security.config.CurrentCompanyIdArgumentResolver;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import com.becommerce.crm.shared.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.authorization.method.AuthorizationAdvisorProxyFactory;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CatalogControllerTest {

    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();

    @Mock private CatalogService catalogService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        StaticApplicationContext appContext = new StaticApplicationContext();
        appContext.getBeanFactory().registerSingleton("perm",
                new PermissionEvaluatorBean(new CurrentUserAuthorities()));
        appContext.refresh();
        DefaultMethodSecurityExpressionHandler expressionHandler =
                new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setApplicationContext(appContext);
        PreAuthorizeAuthorizationManager manager = new PreAuthorizeAuthorizationManager();
        manager.setExpressionHandler(expressionHandler);
        CatalogController secured = (CatalogController) new AuthorizationAdvisorProxyFactory(
                List.of(AuthorizationManagerBeforeMethodInterceptor.preAuthorize(manager)))
                .proxy(new CatalogController(catalogService));
        mockMvc = MockMvcBuilders.standaloneSetup(secured)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new CurrentCompanyIdArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void login(List<String> roles, String... permissions) {
        CurrentUser principal = new CurrentUser(USER_ID, "user@empresa.com", COMPANY_ID, COMPANY_ID,
                roles, List.of(permissions), "sub-1", null, "keycloak", "User", null);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        for (String permission : permissions) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    private String lookupUrl() {
        return "/api/v1/companies/" + COMPANY_ID + "/catalog/lookup";
    }

    private String listUrl() {
        return "/api/v1/companies/" + COMPANY_ID + "/catalog";
    }

    @Test
    void lookupAllowsBudgetScreenAndReturnsOnlySelectFields() throws Exception {
        login(List.of(), "contact:read");
        when(catalogService.lookup(eq(COMPANY_ID), any(), any(), any())).thenReturn(List.of(
                new CatalogLookupResponse(ITEM_ID, "Restauração", new BigDecimal("150.00"), "BRL")));

        mockMvc.perform(get(lookupUrl()).param("type", "SERVICE").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ITEM_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("Restauração"))
                .andExpect(jsonPath("$[0].price").exists())
                .andExpect(jsonPath("$[0].currency").value("BRL"))
                .andExpect(jsonPath("$[0].quantity").doesNotExist());
    }

    @Test
    void lookupAllowsAgendaScreen() throws Exception {
        login(List.of(), "appointment:read");
        when(catalogService.lookup(eq(COMPANY_ID), any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get(lookupUrl()).param("type", "SERVICE").param("active", "true"))
                .andExpect(status().isOk());
    }

    @Test
    void fullListDeniesDependentScreen() throws Exception {
        login(List.of(), "contact:read");

        mockMvc.perform(get(listUrl())).andExpect(status().isForbidden());
    }

    @Test
    void fullListStillAllowsOwner() throws Exception {
        login(List.of(), "catalog:read");
        when(catalogService.list(eq(COMPANY_ID), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get(listUrl())).andExpect(status().isOk());
    }

    @Test
    void lookupDeniesUnrelatedPermission() throws Exception {
        login(List.of(), "lead:read");

        mockMvc.perform(get(lookupUrl())).andExpect(status().isForbidden());
    }

    @Test
    void lookupAllowsCrmAdmin() throws Exception {
        login(List.of("CRM_ADMIN"), "contact:read");
        when(catalogService.lookup(eq(COMPANY_ID), any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get(lookupUrl())).andExpect(status().isOk());
    }
}
