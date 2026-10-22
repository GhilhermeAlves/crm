package com.becommerce.crm.masterdata.contact.web;

import com.becommerce.crm.masterdata.contact.application.dto.request.CreateContactRequest;
import com.becommerce.crm.masterdata.contact.application.dto.request.UpdateContactRequest;
import com.becommerce.crm.masterdata.contact.application.dto.response.ContactResponse;
import com.becommerce.crm.masterdata.contact.application.port.in.ContactUseCase;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Módulo Pacientes (V104). Paciente é um contato com {@code is_patient = true};
 * estas rotas existem para que a lista e a ficha funcionem mesmo com o módulo
 * comercial (CRM) desligado — exigem apenas o módulo PATIENTS (guarda de
 * módulos). Anotações, arquivos e consultas do paciente ficam em
 * {@code /patients/{id}/notes|files|appointments}, nos controllers de origem.
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/patients")
public class PatientController {

    private static final String OWN_COMPANY = "Você só pode acessar pacientes da sua própria empresa.";

    private final ContactUseCase contactUseCase;

    public PatientController(ContactUseCase contactUseCase) {
        this.contactUseCase = contactUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<List<ContactResponse>> list(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(contactUseCase.searchPatients(companyId, search));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('contact:create')")
    public ResponseEntity<ContactResponse> create(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @Valid @RequestBody CreateContactRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        ContactResponse response = contactUseCase.createPatient(companyId, request, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{patientId}")
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<ContactResponse> getById(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @PathVariable UUID patientId) {
        return ResponseEntity.ok(contactUseCase.getPatient(companyId, patientId));
    }

    @PutMapping("/{patientId}")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<ContactResponse> update(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @PathVariable UUID patientId,
            @Valid @RequestBody UpdateContactRequest request) {
        return ResponseEntity.ok(contactUseCase.updatePatient(companyId, patientId, request));
    }
}
