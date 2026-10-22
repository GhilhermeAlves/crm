package com.becommerce.crm.masterdata.company.application.service;

import com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException;
import com.becommerce.crm.masterdata.company.application.dto.CompanyModulesResponse;
import com.becommerce.crm.masterdata.company.application.dto.UpdateCompanyModulesRequest;
import com.becommerce.crm.masterdata.company.application.port.output.CompanyModulesRepository;
import com.becommerce.crm.masterdata.company.domain.CompanyModule;
import com.becommerce.crm.masterdata.company.domain.CompanyModules;
import com.becommerce.crm.masterdata.company.domain.CompanySegment;
import com.becommerce.crm.masterdata.company.domain.InvalidCompanyModulesException;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyModulesServiceTest {

    private final CompanyModulesRepository repository = mock(CompanyModulesRepository.class);
    private final CompanyModulesService service = new CompanyModulesService(repository);
    private final UUID companyId = UUID.randomUUID();

    @Test
    void shouldReturnOwnCompanyModules() {
        when(repository.findByCompanyId(companyId)).thenReturn(Optional.of(new CompanyModules(
                CompanySegment.DENTAL, EnumSet.of(CompanyModule.PATIENTS, CompanyModule.CRM))));

        CompanyModulesResponse response = service.get(companyId, companyId, false);

        assertEquals("DENTAL", response.segment());
        assertEquals(List.of("CRM", "PATIENTS"), response.enabledModules());
    }

    @Test
    void shouldDenyOtherCompanyModulesForNonSuperAdmin() {
        assertThrows(CrmAccessDeniedException.class, () -> service.get(companyId, UUID.randomUUID(), false));
    }

    @Test
    void shouldUpdateIgnoringUnknownModules() {
        when(repository.update(eq(companyId), argThat(m -> m.segment() == CompanySegment.OCCUPATIONAL_SAFETY
                && m.enabledModules().equals(EnumSet.of(CompanyModule.CRM, CompanyModule.AGENDA)))))
                .thenReturn(true);

        CompanyModulesResponse response = service.update(companyId,
                new UpdateCompanyModulesRequest("occupational_safety", List.of("agenda", "CRM", "XYZ")));

        assertEquals(List.of("CRM", "AGENDA"), response.enabledModules());
        verify(repository).update(eq(companyId), argThat(m -> "CRM,AGENDA".equals(m.toCsv())));
    }

    @Test
    void shouldRejectInvalidSegment() {
        assertThrows(InvalidCompanyModulesException.class,
                () -> service.update(companyId, new UpdateCompanyModulesRequest("BAKERY", List.of())));
    }

    @Test
    void isEnabledShouldReflectStoredModules() {
        when(repository.findByCompanyId(companyId)).thenReturn(Optional.of(new CompanyModules(
                CompanySegment.GENERIC, EnumSet.of(CompanyModule.CRM))));

        assertTrue(service.isEnabled(companyId, CompanyModule.CRM));
        assertFalse(service.isEnabled(companyId, CompanyModule.PATIENTS));
    }

    @Test
    void csvRoundTripShouldIgnoreBlanksAndUnknowns() {
        assertEquals(EnumSet.of(CompanyModule.CRM, CompanyModule.FINANCE),
                CompanyModules.parseCsv(" crm, ,FINANCE,foo"));
        assertTrue(CompanyModules.parseCsv(null).isEmpty());
    }
}
