package com.becommerce.crm.presentation.rest.company;

import com.becommerce.crm.application.company.dto.*;
import com.becommerce.crm.application.company.port.input.CompanyUseCase;
import com.becommerce.crm.domain.company.CompanyAlreadyExistsException;
import com.becommerce.crm.domain.company.CompanyNotFoundException;
import com.becommerce.crm.presentation.rest.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(companyController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private CompanyResponse sampleResponse() {
        return new CompanyResponse(
                UUID.randomUUID().toString(),
                "Empresa LTDA", "Empresa", "12345678000190",
                "123456789", "987654321",
                "contato@empresa.com", "(11) 99999-0000", "https://empresa.com",
                new CompanyResponse.AddressResponse(
                        "01001000", "Rua Teste", "100", "Sala 1",
                        "Centro", "São Paulo", "SP", "Brasil"
                ),
                "active", "starter", 5, 1024, null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    void shouldListCompanies() throws Exception {
        CompanySummaryResponse summary = new CompanySummaryResponse(
                UUID.randomUUID().toString(),
                "Empresa LTDA", "Empresa", "12345678000190",
                "contato@empresa.com", "(11) 99999-0000",
                "active", "starter"
        );

        when(companyUseCase.listCompanies()).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].legalName").value("Empresa LTDA"))
                .andExpect(jsonPath("$[0].cnpj").value("12345678000190"));
    }

    @Test
    void shouldGetCompanyById() throws Exception {
        CompanyResponse response = sampleResponse();
        when(companyUseCase.getCompanyById(UUID.fromString(response.id()))).thenReturn(response);

        mockMvc.perform(get("/api/v1/tenants/" + response.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalName").value("Empresa LTDA"))
                .andExpect(jsonPath("$.cnpj").value("12345678000190"))
                .andExpect(jsonPath("$.address.city").value("São Paulo"));
    }

    @Test
    void shouldReturn404WhenCompanyNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(companyUseCase.getCompanyById(id)).thenThrow(new CompanyNotFoundException(id));

        mockMvc.perform(get("/api/v1/tenants/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void shouldCreateCompany() throws Exception {
        CompanyResponse response = sampleResponse();
        when(companyUseCase.createCompany(any(CreateCompanyRequest.class))).thenReturn(response);

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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.legalName").value("Empresa LTDA"));
    }

    @Test
    void shouldReturn409WhenCnpjAlreadyExists() throws Exception {
        when(companyUseCase.createCompany(any(CreateCompanyRequest.class)))
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
        CompanyResponse response = sampleResponse();
        when(companyUseCase.updateCompany(eq(UUID.fromString(response.id())), any(UpdateCompanyRequest.class)))
                .thenReturn(response);

        String json = """
                {
                    "legalName": "Empresa Updated"
                }
                """;

        mockMvc.perform(put("/api/v1/tenants/" + response.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalName").value("Empresa LTDA"));
    }

    @Test
    void shouldDeleteCompany() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/tenants/" + id))
                .andExpect(status().isNoContent());
    }
}
