package com.becommerce.crm.presentation.rest.company;

import com.becommerce.crm.application.company.dto.*;
import com.becommerce.crm.application.company.port.input.CompanyUseCase;
import com.becommerce.crm.domain.company.CompanyAlreadyExistsException;
import com.becommerce.crm.domain.company.CompanyNotFoundException;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import com.becommerce.crm.presentation.rest.handler.GlobalExceptionHandler;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CompanyControllerTest {

    @Mock
    private CompanyUseCase companyUseCase;

    @InjectMocks
    private CompanyController companyController;

    private MockMvc mockMvc;

    private UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(companyController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void login(boolean superAdmin) {
        CurrentUser principal = new CurrentUser(
                UUID.randomUUID(),
                "admin@empresa.com",
                companyId,
                companyId,
                List.of(superAdmin ? "SUPER_ADMIN" : "ADMIN"),
                List.of("company:create", "company:view", "company:update", "settings:view", "settings:update"),
                "keycloak-sub",
                null,
                "keycloak",
                "Admin",
                null
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.permissions().stream().map(SimpleGrantedAuthority::new).toList()));
    }

    private CompanyResponse sampleResponse() {
        return new CompanyResponse(
                companyId.toString(),
                "Empresa LTDA", "Empresa", "12345678000190",
                "123456789", "987654321",
                "contato@empresa.com", "(11) 99999-0000", "https://empresa.com",
                new CompanyResponse.AddressResponse(
                        "01001000", "Rua Teste", "100", "Sala 1",
                        "Centro", "São Paulo", "SP", "Brasil"
                ),
                "active", "starter", 5, 1024, 500, null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    void shouldListCompaniesViaCompaniesRoute() throws Exception {
        login(false);
        CompanySummaryResponse summary = new CompanySummaryResponse(
                companyId.toString(),
                "Empresa LTDA", "Empresa", "12345678000190",
                "contato@empresa.com", "(11) 99999-0000",
                "active", "starter"
        );

        when(companyUseCase.listCompanies(eq(companyId), eq(false))).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].legalName").value("Empresa LTDA"))
                .andExpect(jsonPath("$[0].cnpj").value("12345678000190"));
    }

    @Test
    void shouldListCompaniesViaTenantsAlias() throws Exception {
        login(true);
        CompanySummaryResponse summary = new CompanySummaryResponse(
                companyId.toString(),
                "Empresa LTDA", "Empresa", "12345678000190",
                "contato@empresa.com", "(11) 99999-0000",
                "active", "starter"
        );

        when(companyUseCase.listCompanies(eq(companyId), eq(true))).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].legalName").value("Empresa LTDA"));
    }

    @Test
    void shouldGetOwnCompany() throws Exception {
        login(false);
        CompanyResponse response = sampleResponse();
        when(companyUseCase.getCompanyById(eq(companyId), eq(companyId), eq(false))).thenReturn(response);

        mockMvc.perform(get("/api/v1/companies/" + companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalName").value("Empresa LTDA"))
                .andExpect(jsonPath("$.address.city").value("São Paulo"))
                .andExpect(jsonPath("$.maxContacts").value(500));
    }

    @Test
    void shouldGetMe() throws Exception {
        login(false);
        CompanyResponse response = sampleResponse();
        when(companyUseCase.getCompanyById(eq(companyId), eq(companyId), eq(false))).thenReturn(response);

        mockMvc.perform(get("/api/v1/companies/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(companyId.toString()));
    }

    @Test
    void shouldReturn404WhenCompanyNotFound() throws Exception {
        login(false);
        UUID id = UUID.randomUUID();
        when(companyUseCase.getCompanyById(eq(id), eq(companyId), eq(false)))
                .thenThrow(new CompanyNotFoundException(id));

        mockMvc.perform(get("/api/v1/companies/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void shouldReturn403OnCrossTenantAccess() throws Exception {
        login(false);
        UUID otherCompanyId = UUID.randomUUID();
        when(companyUseCase.getCompanyById(eq(otherCompanyId), eq(companyId), eq(false)))
                .thenThrow(new CrmAccessDeniedException("Acesso a esta empresa não permitido."));

        mockMvc.perform(get("/api/v1/companies/" + otherCompanyId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CRM_ACCESS_DENIED"));
    }

    @Test
    void shouldCreateCompany() throws Exception {
        login(false);
        CompanyResponse response = sampleResponse();
        when(companyUseCase.createCompany(any(CreateCompanyRequest.class), any(UUID.class))).thenReturn(response);

        String json = """
                {
                    "legalName": "Empresa LTDA",
                    "tradingName": "Empresa",
                    "cnpj": "12345678000190",
                    "email": "contato@empresa.com",
                    "phone": "(11) 99999-0000",
                    "addressZipCode": "01001000",
                    "addressStreet": "Rua Teste",
                    "addressNumber": "100",
                    "addressNeighborhood": "Centro",
                    "addressCity": "São Paulo",
                    "addressState": "SP",
                    "plan": "STARTER"
                }
                """;

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.legalName").value("Empresa LTDA"));
    }

    @Test
    void shouldReturn409WhenCnpjAlreadyExists() throws Exception {
        login(false);
        when(companyUseCase.createCompany(any(CreateCompanyRequest.class), any(UUID.class)))
                .thenThrow(new CompanyAlreadyExistsException("CNPJ", "12345678000190"));

        String json = """
                {
                    "legalName": "Empresa LTDA",
                    "tradingName": "Empresa",
                    "cnpj": "12345678000190",
                    "email": "contato@empresa.com",
                    "phone": "(11) 99999-0000",
                    "addressZipCode": "01001000",
                    "addressStreet": "Rua Teste",
                    "addressNumber": "100",
                    "addressNeighborhood": "Centro",
                    "addressCity": "São Paulo",
                    "addressState": "SP",
                    "plan": "STARTER"
                }
                """;

        mockMvc.perform(post("/api/v1/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void shouldReturn400WhenMissingRequiredFields() throws Exception {
        String json = """
                {
                    "tradingName": "Empresa"
                }
                """;

        mockMvc.perform(post("/api/v1/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateCompany() throws Exception {
        login(false);
        CompanyResponse response = sampleResponse();
        when(companyUseCase.updateCompany(eq(companyId), any(UpdateCompanyRequest.class), eq(companyId), eq(false)))
                .thenReturn(response);

        String json = """
                {
                    "legalName": "Empresa Updated"
                }
                """;

        mockMvc.perform(put("/api/v1/companies/" + companyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalName").value("Empresa LTDA"));
    }

    @Test
    void shouldGetSettings() throws Exception {
        login(false);
        CompanySettingsResponse settings = new CompanySettingsResponse(
                companyId.toString(), "America/Sao_Paulo", "pt-BR", "BRL", null, null, LocalDateTime.now());
        when(companyUseCase.getCompanySettings(eq(companyId), eq(companyId))).thenReturn(settings);

        mockMvc.perform(get("/api/v1/companies/" + companyId + "/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("America/Sao_Paulo"))
                .andExpect(jsonPath("$.currency").value("BRL"));
    }

    @Test
    void shouldUpdateSettings() throws Exception {
        login(false);
        CompanySettingsResponse settings = new CompanySettingsResponse(
                companyId.toString(), "Europe/Lisbon", "pt-BR", "BRL", null, null, LocalDateTime.now());
        when(companyUseCase.updateCompanySettings(eq(companyId), any(UpdateCompanySettingsRequest.class), eq(companyId)))
                .thenReturn(settings);

        String json = """
                {
                    "timezone": "Europe/Lisbon"
                }
                """;

        mockMvc.perform(put("/api/v1/companies/" + companyId + "/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("Europe/Lisbon"));
    }

    @Test
    void shouldDeleteCompany() throws Exception {
        login(true);
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/companies/" + id))
                .andExpect(status().isNoContent());
    }
}
