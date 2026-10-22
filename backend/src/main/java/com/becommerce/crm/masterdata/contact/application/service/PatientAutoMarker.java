package com.becommerce.crm.masterdata.contact.application.service;

import com.becommerce.crm.masterdata.company.application.service.CompanyModulesService;
import com.becommerce.crm.masterdata.company.domain.CompanyModule;
import com.becommerce.crm.masterdata.contact.application.port.in.ContactUseCase;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Agendar uma consulta para um contato o torna paciente — apenas em empresas
 * com o módulo PATIENTS (fora da área da saúde o contato segue só contato).
 * Atenção: {@link ContactUseCase#markAsPatientIfExists} limpa o TenantContext no
 * {@code finally}; quem chama deve reaplicá-lo.
 */
@Component
public class PatientAutoMarker {

    private final CompanyModulesService modulesService;
    private final ContactUseCase contactUseCase;

    public PatientAutoMarker(CompanyModulesService modulesService, ContactUseCase contactUseCase) {
        this.modulesService = modulesService;
        this.contactUseCase = contactUseCase;
    }

    /** @return {@code true} se a marcação foi aplicada (ou o contato já era paciente). */
    public boolean markOnAppointment(UUID companyId, UUID contactId) {
        if (contactId == null || !modulesService.isEnabled(companyId, CompanyModule.PATIENTS)) {
            return false;
        }
        return contactUseCase.markAsPatientIfExists(companyId, contactId);
    }
}
