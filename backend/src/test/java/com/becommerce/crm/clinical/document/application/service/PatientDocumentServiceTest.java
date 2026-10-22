package com.becommerce.crm.clinical.document.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.clinical.document.application.dto.IssuePatientDocumentRequest;
import com.becommerce.crm.clinical.document.application.dto.PatientDocumentResponse;
import com.becommerce.crm.clinical.document.application.dto.PreviewPatientDocumentRequest;
import com.becommerce.crm.clinical.document.application.dto.PreviewPatientDocumentResponse;
import com.becommerce.crm.clinical.document.application.port.out.PatientDocumentRepository;
import com.becommerce.crm.clinical.document.domain.PatientDocument;
import com.becommerce.crm.clinical.document.domain.exception.PatientDocumentNotFoundException;
import com.becommerce.crm.clinical.documenttemplate.application.dto.DocumentTemplateResponse;
import com.becommerce.crm.clinical.documenttemplate.application.port.in.DocumentTemplateUseCase;
import com.becommerce.crm.clinical.documenttemplate.domain.DocumentType;
import com.becommerce.crm.clinical.documenttemplate.domain.exception.DocumentTemplateNotFoundException;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException;
import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.Contact;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientDocumentServiceTest {

    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID CONTACT_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private PatientDocumentRepository repository;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private DocumentTemplateUseCase templateUseCase;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientDocumentPdfRenderer pdfRenderer;

    @Mock
    private TenantAuditRecorder auditor;

    private PatientDocumentService service;

    @BeforeEach
    void setUp() {
        service = new PatientDocumentService(repository, contactRepository, companyRepository,
                templateUseCase, userRepository, pdfRenderer, auditor);

        Contact contact = mock(Contact.class);
        lenient().when(contact.getCompanyId()).thenReturn(COMPANY_ID);
        lenient().when(contact.getFirstName()).thenReturn("Maria");
        lenient().when(contact.getLastName()).thenReturn("Silva");
        lenient().when(contact.getCpf()).thenReturn("12345678901");
        lenient().when(contact.getBirthDate()).thenReturn(LocalDate.of(1990, 5, 10));
        lenient().when(contact.getCity()).thenReturn("São Paulo");
        lenient().when(contact.getRecordNumber()).thenReturn("P-001");
        lenient().when(contactRepository.findById(CONTACT_ID)).thenReturn(Optional.of(contact));

        Company company = mock(Company.class);
        lenient().when(company.getTradingName()).thenReturn("Clínica Bem");
        lenient().when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        User user = mock(User.class);
        lenient().when(user.getName()).thenReturn("Dra. Ana");
        lenient().when(userRepository.findById(ACTOR_ID)).thenReturn(Optional.of(user));

        lenient().when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void previewRendersTemplateWithoutSaving() {
        UUID templateId = UUID.randomUUID();
        when(templateUseCase.get(COMPANY_ID, templateId)).thenReturn(new DocumentTemplateResponse(
                templateId, DocumentType.PRESCRIPTION, "Receita",
                "Paciente {{ paciente.nome }} ({{paciente.cpf}}), nascido em {{paciente.nascimento}}.",
                true, LocalDateTime.now(), LocalDateTime.now()));

        PreviewPatientDocumentResponse response = service.preview(COMPANY_ID, CONTACT_ID,
                new PreviewPatientDocumentRequest(templateId), ACTOR_ID);

        assertThat(response.type()).isEqualTo(DocumentType.PRESCRIPTION);
        assertThat(response.title()).isEqualTo("Receita");
        assertThat(response.body()).contains("Maria Silva", "123.456.789-01", "10/05/1990");
        verify(repository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void previewRejectsInactiveTemplate() {
        UUID templateId = UUID.randomUUID();
        when(templateUseCase.get(COMPANY_ID, templateId)).thenReturn(new DocumentTemplateResponse(
                templateId, DocumentType.CONSENT, "Termo", "Corpo", false,
                LocalDateTime.now(), LocalDateTime.now()));

        assertThatThrownBy(() -> service.preview(COMPANY_ID, CONTACT_ID,
                new PreviewPatientDocumentRequest(templateId), ACTOR_ID))
                .isInstanceOf(ContactValidationException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void previewFromAnotherCompanyTemplateIsNotFound() {
        UUID templateId = UUID.randomUUID();
        when(templateUseCase.get(COMPANY_ID, templateId))
                .thenThrow(new DocumentTemplateNotFoundException(templateId));

        assertThatThrownBy(() -> service.preview(COMPANY_ID, CONTACT_ID,
                new PreviewPatientDocumentRequest(templateId), ACTOR_ID))
                .isInstanceOf(DocumentTemplateNotFoundException.class);
    }

    @Test
    void issueKeepsEditedBodyExactly() {
        String body = "Texto editado pelo profissional.\nSegunda linha. ";
        PatientDocumentResponse response = service.issue(COMPANY_ID, CONTACT_ID,
                new IssuePatientDocumentRequest(null, DocumentType.OTHER, "Documento", body,
                        "Dra. Ana", null), ACTOR_ID);

        assertThat(response.body()).isEqualTo(body);
        assertThat(response.issuedBy()).isEqualTo(ACTOR_ID);
        assertThat(response.issuedAt()).isNotNull();
    }

    @Test
    void nonAdminCannotDeleteIssuedDocument() {
        PatientDocument document = document(COMPANY_ID);

        assertThatThrownBy(() -> service.delete(COMPANY_ID, CONTACT_ID, document.getId(), false,
                ACTOR_ID))
                .isInstanceOf(CrmAccessDeniedException.class)
                .hasMessageContaining("administradores");
    }

    @Test
    void adminCanDeleteIssuedDocument() {
        PatientDocument document = document(COMPANY_ID);
        when(repository.findById(document.getId())).thenReturn(Optional.of(document));

        service.delete(COMPANY_ID, CONTACT_ID, document.getId(), true, ACTOR_ID);

        verify(repository).delete(document);
    }

    @Test
    void pdfOfAnotherCompanyIsNotFound() {
        PatientDocument document = document(UUID.randomUUID());
        when(repository.findById(document.getId())).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service.pdf(COMPANY_ID, CONTACT_ID, document.getId()))
                .isInstanceOf(PatientDocumentNotFoundException.class);
    }

    private PatientDocument document(UUID companyId) {
        PatientDocument document = PatientDocument.issue(companyId, CONTACT_ID, null,
                DocumentType.PRESCRIPTION, "Receita", "Conteúdo", "Dra. Ana", null, ACTOR_ID);
        lenient().when(repository.findById(document.getId())).thenReturn(Optional.of(document));
        return document;
    }
}
