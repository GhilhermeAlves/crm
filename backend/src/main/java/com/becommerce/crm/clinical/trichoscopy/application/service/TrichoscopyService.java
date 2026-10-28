package com.becommerce.crm.clinical.trichoscopy.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.clinical.trichoscopy.application.dto.TrichoscopyImageResponse;
import com.becommerce.crm.clinical.trichoscopy.application.port.in.TrichoscopyUseCase;
import com.becommerce.crm.clinical.trichoscopy.application.port.out.TrichoscopyRepository;
import com.becommerce.crm.clinical.trichoscopy.domain.PatientTrichoscopyImage;
import com.becommerce.crm.clinical.trichoscopy.domain.exception.TrichoscopyImageNotFoundException;
import com.becommerce.crm.masterdata.company.application.port.output.CompanyModulesRepository;
import com.becommerce.crm.masterdata.company.domain.CompanySegment;
import com.becommerce.crm.masterdata.storage.application.dto.StorageResponse;
import com.becommerce.crm.masterdata.storage.application.port.input.StorageUseCase;
import com.becommerce.crm.masterdata.contact.note.application.service.PatientTimelineRecorder;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Imagens do tricoscópio por paciente. O módulo PATIENTS já é exigido pelo
 * ModuleGuardInterceptor (rota /patients); aqui só se exige o segmento TRICHOLOGY.
 */
@Service
public class TrichoscopyService implements TrichoscopyUseCase {

    static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final List<String> ALLOWED = List.of("image/jpeg", "image/png", "image/webp");
    private static final int MAX_NOTES = 2000;

    private final TrichoscopyRepository repository;
    private final StorageUseCase storageUseCase;
    private final CompanyModulesRepository companyModulesRepository;
    private final TenantAuditRecorder auditor;


    private PatientTimelineRecorder timeline;

    /** Opcional: registra os eventos nas anotações do paciente. */
    @Autowired(required = false)
    public void setPatientTimelineRecorder(PatientTimelineRecorder timeline) {
        this.timeline = timeline;
    }

    private void note(UUID companyId, UUID contactId, String body, UUID actorId) {
        if (timeline != null) {
            timeline.record(companyId, contactId, body, actorId);
        }
    }

    public TrichoscopyService(TrichoscopyRepository repository,
                              StorageUseCase storageUseCase,
                              CompanyModulesRepository companyModulesRepository,
                              TenantAuditRecorder auditor) {
        this.repository = repository;
        this.storageUseCase = storageUseCase;
        this.companyModulesRepository = companyModulesRepository;
        this.auditor = auditor;
    }

    @Override
    @Transactional
    public TrichoscopyImageResponse capture(UUID companyId, UUID contactId, String fileName, String contentType,
                                            byte[] data, String notes, UUID userId) {
        requireTrichology(companyId);
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Arquivo inválido.");
        }
        if (contentType == null || !ALLOWED.contains(contentType) || data.length > MAX_BYTES) {
            throw new IllegalArgumentException("A foto deve ser JPG, PNG ou WEBP de até 5 MB.");
        }
        String cleanNotes = notes == null || notes.isBlank() ? null : notes.trim();
        if (cleanNotes != null && cleanNotes.length() > MAX_NOTES) {
            throw new IllegalArgumentException("Observações com no máximo " + MAX_NOTES + " caracteres.");
        }

        try {
            TenantContext.setCompanyId(companyId);
            StorageResponse stored = storageUseCase.uploadForContact(companyId, contactId,
                    fileName == null || fileName.isBlank() ? "tricoscopia.jpg" : fileName,
                    contentType, data, userId);
            PatientTrichoscopyImage saved = repository.save(
                    PatientTrichoscopyImage.create(companyId, contactId, stored.id(), userId, cleanNotes));
            note(companyId, contactId, cleanNotes == null
                    ? "Imagem de tricoscopia capturada."
                    : "Imagem de tricoscopia capturada: " + cleanNotes, userId);
            auditor.record(companyId, AuditAction.CREATE, AuditModule.CONTACTS, "PatientTrichoscopyImage",
                    saved.getId().toString(), "TRICHOSCOPY_CAPTURED", userId,
                    Map.of("contactId", contactId.toString(), "storageObjectId", stored.id().toString()));
            return toResponse(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrichoscopyImageResponse> list(UUID companyId, UUID contactId) {
        requireTrichology(companyId);
        try {
            TenantContext.setCompanyId(companyId);
            return repository.findByCompanyIdAndContactId(companyId, contactId).stream()
                    .map(this::toResponse)
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID contactId, UUID imageId, UUID userId) {
        requireTrichology(companyId);
        try {
            TenantContext.setCompanyId(companyId);
            PatientTrichoscopyImage image = repository
                    .findByIdAndCompanyIdAndContactId(imageId, companyId, contactId)
                    .orElseThrow(() -> new TrichoscopyImageNotFoundException(imageId));
            repository.delete(image);
            storageUseCase.delete(companyId, image.getStorageObjectId());
            note(companyId, contactId, "Imagem de tricoscopia excluída.", userId);
            auditor.record(companyId, AuditAction.DELETE, AuditModule.CONTACTS, "PatientTrichoscopyImage",
                    imageId.toString(), "TRICHOSCOPY_DELETED", userId,
                    Map.of("contactId", contactId.toString()));
        } finally {
            TenantContext.clear();
        }
    }

    private void requireTrichology(UUID companyId) {
        boolean trichology = companyModulesRepository.findByCompanyId(companyId)
                .map(modules -> modules.segment() == CompanySegment.TRICHOLOGY)
                .orElse(false);
        if (!trichology) {
            throw new AccessDeniedException("Recurso exclusivo de clínicas de tricologia/medicina capilar.");
        }
    }

    private TrichoscopyImageResponse toResponse(PatientTrichoscopyImage image) {
        String downloadUrl = "/api/v1/companies/" + image.getCompanyId() + "/storage/"
                + image.getStorageObjectId() + "?inline=true";
        return new TrichoscopyImageResponse(
                image.getId(),
                image.getContactId(),
                image.getStorageObjectId(),
                image.getTakenBy(),
                image.getTakenAt(),
                image.getNotes(),
                downloadUrl);
    }
}
