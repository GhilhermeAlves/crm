package com.becommerce.crm.clinical.documenttemplate.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.clinical.documenttemplate.application.dto.DocumentTemplateRequest;
import com.becommerce.crm.clinical.documenttemplate.application.dto.DocumentTemplateResponse;
import com.becommerce.crm.clinical.documenttemplate.application.port.out.DocumentTemplateRepository;
import com.becommerce.crm.clinical.documenttemplate.domain.DocumentTemplate;
import com.becommerce.crm.clinical.documenttemplate.domain.DocumentType;
import com.becommerce.crm.clinical.documenttemplate.domain.exception.DocumentTemplateNotFoundException;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentTemplateServiceTest {

    @Mock DocumentTemplateRepository templateRepository;
    @Mock TenantAuditRecorder auditor;
    @InjectMocks DocumentTemplateService service;

    private final UUID companyId = UUID.randomUUID();
    private final UUID authorId = UUID.randomUUID();

    private DocumentTemplate template(UUID id, String title) {
        return DocumentTemplate.reconstitute(id, companyId, DocumentType.CERTIFICATE, title,
                "Corpo do documento", true, authorId, java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now());
    }

    @Test
    void createsTemplate() {
        when(templateRepository.save(any(DocumentTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentTemplateResponse response = service.create(companyId,
                new DocumentTemplateRequest(DocumentType.PRESCRIPTION, "  Receita  ", "  Corpo  ", true),
                authorId);

        assertEquals("Receita", response.title());
        assertEquals("Corpo", response.body());
        assertEquals(DocumentType.PRESCRIPTION, response.type());
        verify(templateRepository).save(any(DocumentTemplate.class));
    }

    @Test
    void rejectsBlankTitle() {
        assertThrows(ContactValidationException.class, () -> service.create(companyId,
                new DocumentTemplateRequest(DocumentType.CERTIFICATE, "   ", "Corpo", true), authorId));
        verify(templateRepository, never()).save(any(DocumentTemplate.class));
    }

    @Test
    void getFailsWhenTemplateBelongsToAnotherCompany() {
        UUID templateId = UUID.randomUUID();
        DocumentTemplate other = DocumentTemplate.reconstitute(templateId, UUID.randomUUID(),
                DocumentType.CERTIFICATE, "Outro", "Corpo", true, authorId,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(other));

        assertThrows(DocumentTemplateNotFoundException.class, () -> service.get(companyId, templateId));
    }

    @Test
    void listSeedsThreeDefaultsWhenCompanyHasNoTemplates() {
        when(templateRepository.countByCompanyId(companyId)).thenReturn(0L);
        when(templateRepository.save(any(DocumentTemplate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(templateRepository.findByCompanyIdOrderByTypeAscTitleAsc(companyId)).thenReturn(List.of(
                template(UUID.randomUUID(), "Atestado de afastamento"),
                template(UUID.randomUUID(), "Atestado de comparecimento"),
                template(UUID.randomUUID(), "Receituário simples")));

        List<DocumentTemplateResponse> result = service.list(companyId, null, false);

        assertEquals(3, result.size());
        verify(templateRepository, org.mockito.Mockito.times(3)).save(any(DocumentTemplate.class));
    }

    @Test
    void listDoesNotSeedWhenCompanyAlreadyHasTemplates() {
        when(templateRepository.countByCompanyId(companyId)).thenReturn(1L);
        when(templateRepository.findByCompanyIdOrderByTypeAscTitleAsc(companyId))
                .thenReturn(List.of(template(UUID.randomUUID(), "Atestado")));

        List<DocumentTemplateResponse> result = service.list(companyId, null, false);

        assertEquals(1, result.size());
        verify(templateRepository, never()).save(any(DocumentTemplate.class));
    }
}
