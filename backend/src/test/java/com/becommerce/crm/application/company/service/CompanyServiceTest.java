package com.becommerce.crm.application.company.service;

import com.becommerce.crm.application.company.dto.*;
import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.application.identity.port.output.EventPublisher;
import com.becommerce.crm.domain.company.*;
import com.becommerce.crm.domain.company.event.CompanyCreatedEvent;
import com.becommerce.crm.domain.company.event.CompanyDeletedEvent;
import com.becommerce.crm.domain.company.event.CompanyUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private CompanyService companyService;

    private Company sampleCompany;

    @BeforeEach
    void setUp() {
        sampleCompany = Company.create(
                "Empresa LTDA", "Empresa", "12345678000190",
                "123456789", "987654321",
                "contato@empresa.com", "(11) 99999-0000", "https://empresa.com",
                "01001000", "Rua Teste", "100", "Sala 1",
                "Centro", "São Paulo", "SP", "Brasil",
                CompanyPlan.STARTER, 5, 1024, null, null
        );
    }

    @Test
    void shouldGetCompanyById() {
        when(companyRepository.findById(sampleCompany.getId())).thenReturn(Optional.of(sampleCompany));

        CompanyResponse response = companyService.getCompanyById(sampleCompany.getId());

        assertEquals(sampleCompany.getId().toString(), response.id());
        assertEquals("Empresa LTDA", response.legalName());
        assertEquals("12345678000190", response.cnpj());
        assertEquals("active", response.status());
        assertEquals("starter", response.plan());
    }

    @Test
    void shouldThrowWhenCompanyNotFound() {
        UUID id = UUID.randomUUID();
        when(companyRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CompanyNotFoundException.class, () -> companyService.getCompanyById(id));
    }

    @Test
    void shouldListCompanies() {
        Company another = Company.create(
                "Outra LTDA", "Outra", "99999999000199",
                null, null,
                "contato@outra.com", "(11) 88888-0000", null,
                "02002000", "Rua Outra", "200", null,
                "Bairro", "Rio de Janeiro", "RJ", "Brasil",
                CompanyPlan.PROFESSIONAL, 10, 2048, null, null
        );

        when(companyRepository.findAll()).thenReturn(List.of(sampleCompany, another));

        List<CompanySummaryResponse> result = companyService.listCompanies();

        assertEquals(2, result.size());
        assertEquals("Empresa LTDA", result.get(0).legalName());
        assertEquals("Outra LTDA", result.get(1).legalName());
    }

    @Test
    void shouldCreateCompany() {
        when(companyRepository.existsByCnpj("12345678000190")).thenReturn(false);
        when(companyRepository.existsByEmail("contato@empresa.com")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenReturn(sampleCompany);

        CreateCompanyRequest request = new CreateCompanyRequest(
                "Empresa LTDA", "Empresa", "12345678000190",
                "123456789", "987654321",
                "contato@empresa.com", "(11) 99999-0000", "https://empresa.com",
                "01001000", "Rua Teste", "100", "Sala 1",
                "Centro", "São Paulo", "SP", "Brasil",
                "STARTER", 5, 1024, null, null
        );

        CompanyResponse response = companyService.createCompany(request);

        assertNotNull(response.id());
        assertEquals("Empresa LTDA", response.legalName());

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(captor.capture());
        assertEquals(CompanyPlan.STARTER, captor.getValue().getPlan());
        assertEquals(CompanyStatus.ACTIVE, captor.getValue().getStatus());

        verify(eventPublisher).publish(any(CompanyCreatedEvent.class));
    }

    @Test
    void shouldThrowWhenCnpjAlreadyExists() {
        when(companyRepository.existsByCnpj("12345678000190")).thenReturn(true);

        CreateCompanyRequest request = new CreateCompanyRequest(
                "Empresa LTDA", "Empresa", "12345678000190",
                null, null,
                "contato@empresa.com", "(11) 99999-0000", null,
                "01001000", "Rua Teste", "100", null,
                "Centro", "São Paulo", "SP", "Brasil",
                "STARTER", null, null, null, null
        );

        assertThrows(CompanyAlreadyExistsException.class, () -> companyService.createCompany(request));
        verify(companyRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        when(companyRepository.existsByCnpj("12345678000190")).thenReturn(false);
        when(companyRepository.existsByEmail("contato@empresa.com")).thenReturn(true);

        CreateCompanyRequest request = new CreateCompanyRequest(
                "Empresa LTDA", "Empresa", "12345678000190",
                null, null,
                "contato@empresa.com", "(11) 99999-0000", null,
                "01001000", "Rua Teste", "100", null,
                "Centro", "São Paulo", "SP", "Brasil",
                "STARTER", null, null, null, null
        );

        assertThrows(CompanyAlreadyExistsException.class, () -> companyService.createCompany(request));
    }

    @Test
    void shouldUpdateCompany() {
        when(companyRepository.findById(sampleCompany.getId())).thenReturn(Optional.of(sampleCompany));
        when(companyRepository.save(any(Company.class))).thenReturn(sampleCompany);

        UpdateCompanyRequest request = new UpdateCompanyRequest(
                "Updated LTDA", null, null, null, null,
                null, null, null, null, null, null, null, null,
                "PROFESSIONAL", null, 10, null, null, null
        );

        CompanyResponse response = companyService.updateCompany(sampleCompany.getId(), request);

        assertEquals("Updated LTDA", response.legalName());
        verify(eventPublisher).publish(any(CompanyUpdatedEvent.class));
    }

    @Test
    void shouldDeleteCompany() {
        when(companyRepository.findById(sampleCompany.getId())).thenReturn(Optional.of(sampleCompany));
        doNothing().when(companyRepository).deleteById(sampleCompany.getId());

        companyService.deleteCompany(sampleCompany.getId());

        verify(companyRepository).deleteById(sampleCompany.getId());
        verify(eventPublisher).publish(any(CompanyDeletedEvent.class));
    }

    @Test
    void shouldThrowWhenDeletingNonexistentCompany() {
        UUID id = UUID.randomUUID();
        when(companyRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CompanyNotFoundException.class, () -> companyService.deleteCompany(id));
        verify(companyRepository, never()).deleteById(any());
    }
}
