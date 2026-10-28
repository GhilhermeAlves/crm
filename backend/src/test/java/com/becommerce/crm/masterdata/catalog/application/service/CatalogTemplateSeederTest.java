package com.becommerce.crm.masterdata.catalog.application.service;

import com.becommerce.crm.masterdata.catalog.application.port.output.CatalogItemRepository;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItem;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;
import com.becommerce.crm.masterdata.company.domain.CompanySegment;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogTemplateSeederTest {

    private final CatalogItemRepository repository = mock(CatalogItemRepository.class);
    private final CatalogTemplateSeeder seeder = new CatalogTemplateSeeder(repository);
    private final UUID companyId = UUID.randomUUID();

    @Test
    void shouldSeedTrichologyProceduresAsZeroPricedServices() {
        seeder.seedProcedures(companyId, CompanySegment.TRICHOLOGY);

        ArgumentCaptor<CatalogItem> captor = ArgumentCaptor.forClass(CatalogItem.class);
        int expected = CatalogTemplateSeeder.PROCEDURES.get(CompanySegment.TRICHOLOGY).size();
        verify(repository, times(expected)).save(captor.capture());
        CatalogItem first = captor.getAllValues().get(0);
        assertEquals(companyId, first.getCompanyId());
        assertEquals(CatalogItemType.SERVICE, first.getType());
        assertEquals(0, BigDecimal.ZERO.compareTo(first.getPrice()));
        assertEquals("Tricologia", first.getCategory());
    }

    @Test
    void shouldSeedOccupationalSafetyProcedures() {
        seeder.seedProcedures(companyId, CompanySegment.OCCUPATIONAL_SAFETY);
        int expected = CatalogTemplateSeeder.PROCEDURES.get(CompanySegment.OCCUPATIONAL_SAFETY).size();
        assertTrue(expected > 0);
        verify(repository, times(expected)).save(any());
    }

    @Test
    void shouldNotSeedSegmentsWithoutTemplate() {
        seeder.seedProcedures(companyId, CompanySegment.GENERIC);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldSeedDentalProceduresFromResource() {
        int created = seeder.seedProcedures(companyId, CompanySegment.DENTAL);

        assertEquals(341, created);
        verify(repository, times(341)).save(any());
    }

    @Test
    void shouldSkipProceduresTheCompanyAlreadyHas() {
        when(repository.findLowerCaseNames(companyId)).thenReturn(Set.of("tricograma"));

        int created = seeder.seedProcedures(companyId, CompanySegment.TRICHOLOGY);

        int total = CatalogTemplateSeeder.PROCEDURES.get(CompanySegment.TRICHOLOGY).size();
        assertEquals(total - 1, created);
        assertTrue(seeder.hasSegmentProcedures(companyId, CompanySegment.TRICHOLOGY));
        assertFalse(seeder.hasSegmentProcedures(companyId, CompanySegment.OCCUPATIONAL_SAFETY));
    }

    @Test
    void shouldActivateOnlyTheSegmentProcedures() {
        seeder.applySegment(companyId, CompanySegment.TRICHOLOGY);

        verify(repository).setActiveByCategories(eq(companyId),
                argThat(c -> c.containsAll(List.of("Odontologia", "Segurança do trabalho"))
                        && !c.contains("Tricologia")), eq(false));
        verify(repository).setActiveByCategories(companyId, List.of("Tricologia"), true);
    }
}
