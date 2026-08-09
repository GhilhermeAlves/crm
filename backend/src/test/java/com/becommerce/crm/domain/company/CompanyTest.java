package com.becommerce.crm.domain.company;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CompanyTest {

    @Test
    void shouldCreateCompanyWithDefaults() {
        Company company = Company.create(
                "Empresa LTDA", "Empresa", "12345678000190",
                "123456789", "987654321",
                "contato@empresa.com", "(11) 99999-0000", "https://empresa.com",
                "01001000", "Rua Teste", "100", "Sala 1",
                "Centro", "São Paulo", "SP", "Brasil",
                CompanyPlan.STARTER, 5, 1024, 500, null, null
        );

        assertNotNull(company.getId());
        assertEquals("Empresa LTDA", company.getLegalName());
        assertEquals("Empresa", company.getTradingName());
        assertEquals("12345678000190", company.getCnpj());
        assertEquals(CompanyStatus.ACTIVE, company.getStatus());
        assertEquals(CompanyPlan.STARTER, company.getPlan());
        assertEquals(5, company.getMaxUsers());
        assertEquals(1024, company.getMaxStorageMb());
        assertEquals(500, company.getMaxContacts());
        assertNotNull(company.getCreatedAt());
        assertNotNull(company.getUpdatedAt());
    }

    @Test
    void shouldReconstituteCompany() {
        UUID id = UUID.randomUUID();
        Company company = Company.reconstitute(
                id, "Empresa LTDA", "Empresa", "12345678000190",
                "123456789", "987654321",
                "contato@empresa.com", "(11) 99999-0000", "https://empresa.com",
                "01001000", "Rua Teste", "100", "Sala 1",
                "Centro", "São Paulo", "SP", "Brasil",
                CompanyPlan.PROFESSIONAL, CompanyStatus.ACTIVE,
                10, 2048, 1000, "https://logo.png", "Notes",
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );

        assertEquals(id, company.getId());
        assertEquals(CompanyPlan.PROFESSIONAL, company.getPlan());
        assertEquals(10, company.getMaxUsers());
        assertEquals(1000, company.getMaxContacts());
    }

    @Test
    void shouldUpdateCompany() {
        Company company = Company.create(
                "Empresa LTDA", "Empresa", "12345678000190",
                "123456789", "987654321",
                "contato@empresa.com", "(11) 99999-0000", null,
                "01001000", "Rua Teste", "100", null,
                "Centro", "São Paulo", "SP", "Brasil",
                CompanyPlan.STARTER, 5, 1024, 500, null, null
        );

        company.update(
                "Empresa Updated LTDA", "Empresa Updated",
                "novo@empresa.com", "(11) 88888-0000", "https://novo.com",
                "02002000", "Rua Nova", "200", "Andar 2",
                "Novo Bairro", "Rio de Janeiro", "RJ", "Brasil",
                CompanyPlan.ENTERPRISE, CompanyStatus.SUSPENDED,
                50, 4096, 1000, "https://novo-logo.png", "Updated notes"
        );

        assertEquals("Empresa Updated LTDA", company.getLegalName());
        assertEquals("novo@empresa.com", company.getEmail());
        assertEquals(CompanyPlan.ENTERPRISE, company.getPlan());
        assertEquals(CompanyStatus.SUSPENDED, company.getStatus());
        assertEquals(50, company.getMaxUsers());
        assertEquals(1000, company.getMaxContacts());
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        UUID id = UUID.randomUUID();
        Company company1 = Company.reconstitute(
                id, "Empresa LTDA", "Empresa", "12345678000190",
                null, null, "contato@empresa.com", "(11) 99999-0000", null,
                "01001000", "Rua Teste", "100", null,
                "Centro", "São Paulo", "SP", "Brasil",
                CompanyPlan.STARTER, CompanyStatus.ACTIVE,
                5, 1024, 500, null, null,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );

        Company company2 = Company.reconstitute(
                id, "Outro Nome", "Outro", "99999999000199",
                null, null, "outro@empresa.com", "(11) 77777-0000", null,
                "02002000", "Rua Outra", "200", null,
                "Outro Bairro", "Rio de Janeiro", "RJ", "Brasil",
                CompanyPlan.ENTERPRISE, CompanyStatus.ACTIVE,
                50, 4096, 1000, null, null,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );

        assertEquals(company1, company2);
        assertEquals(company1.hashCode(), company2.hashCode());
    }

    @Test
    void companyStatusCanOperate() {
        assertTrue(CompanyStatus.ACTIVE.canOperate());
        assertFalse(CompanyStatus.INACTIVE.canOperate());
        assertFalse(CompanyStatus.SUSPENDED.canOperate());
        assertFalse(CompanyStatus.ONBOARDING.canOperate());
    }

    @Test
    void companyPlanCanAccessFeatures() {
        assertFalse(CompanyPlan.STARTER.canAccessAdvancedFeatures());
        assertTrue(CompanyPlan.PROFESSIONAL.canAccessAdvancedFeatures());
        assertTrue(CompanyPlan.BUSINESS.canAccessAdvancedFeatures());
        assertTrue(CompanyPlan.ENTERPRISE.canAccessAdvancedFeatures());
        assertFalse(CompanyPlan.STARTER.canAccessEnterpriseFeatures());
        assertFalse(CompanyPlan.PROFESSIONAL.canAccessEnterpriseFeatures());
        assertFalse(CompanyPlan.BUSINESS.canAccessEnterpriseFeatures());
        assertTrue(CompanyPlan.ENTERPRISE.canAccessEnterpriseFeatures());
    }
}
