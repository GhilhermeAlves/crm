package com.becommerce.crm.presentation.rest.onboarding;

import com.becommerce.crm.application.company.dto.CompanyResponse;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.application.onboarding.port.input.OnboardingUseCase;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.valueobject.Email;
import com.becommerce.crm.domain.identity.exception.UserNotFoundException;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OnboardingControllerTest {

    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");

    @Mock private OnboardingUseCase onboardingUseCase;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private OnboardingController onboardingController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(onboardingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void login() {
        CurrentUser principal = new CurrentUser(
                USER_ID,
                "owner@empresa.com",
                null,
                null,
                List.of(),
                List.of(),
                "keycloak-sub",
                "session-1",
                "keycloak",
                "Owner",
                null
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private User ownerWithoutCompany() {
        User user = User.create(new Email("owner@empresa.com"), null, "Owner", "Dono", null);
        user.setId(USER_ID);
        return user;
    }

    private CompanyResponse response() {
        return new CompanyResponse(
                UUID.randomUUID().toString(), "Minha Empresa LTDA", "Minha", "12345678000190",
                null, null, "contato@minha.com", "(11) 99999-0000", null,
                null, "active", "starter", 5, 1024, 500, null, null,
                null, null);
    }

    private String json() {
        return """
                {
                    "legalName": "Minha Empresa LTDA",
                    "tradingName": "Minha",
                    "cnpj": "12345678000190",
                    "email": "contato@minha.com",
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
    }

    @Test
    void shouldCreateCompanyForUserWithoutCompany() throws Exception {
        login();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(ownerWithoutCompany()));
        when(onboardingUseCase.onboard(any(), any())).thenReturn(response());

        mockMvc.perform(post("/api/v1/onboarding/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.legalName").value("Minha Empresa LTDA"));
    }

    @Test
    void shouldRejectUserWhoAlreadyHasCompany() throws Exception {
        login();
        User withCompany = ownerWithoutCompany();
        withCompany.setCompanyId(UUID.randomUUID());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(withCompany));

        mockMvc.perform(post("/api/v1/onboarding/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json()))
                .andExpect(status().isBadRequest());
        verify(onboardingUseCase, never()).onboard(any(), any());
    }

    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        login();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/onboarding/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json()))
                .andExpect(status().isNotFound());
        verify(onboardingUseCase, never()).onboard(any(), any());
    }

    @Test
    void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
        login();

        String invalid = """
                {
                    "tradingName": "Minha"
                }
                """;

        mockMvc.perform(post("/api/v1/onboarding/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());
        verify(onboardingUseCase, never()).onboard(any(), any());
    }

    @Test
    void shouldCallOnboardingWithDomainOwner() throws Exception {
        login();
        User owner = ownerWithoutCompany();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
        when(onboardingUseCase.onboard(any(), eq(owner))).thenReturn(response());

        mockMvc.perform(post("/api/v1/onboarding/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json()))
                .andExpect(status().isCreated());
    }
}