package com.becommerce.crm.masterdata.contact.note.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactNotFoundException;
import com.becommerce.crm.masterdata.contact.note.application.dto.ContactNoteRequest;
import com.becommerce.crm.masterdata.contact.note.application.dto.ContactNoteResponse;
import com.becommerce.crm.masterdata.contact.note.application.port.in.ContactNoteUseCase;
import com.becommerce.crm.masterdata.contact.note.application.port.out.ContactNoteRepository;
import com.becommerce.crm.masterdata.contact.note.domain.ContactNote;
import com.becommerce.crm.masterdata.contact.note.domain.exception.ContactNoteNotFoundException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Anotações do paciente (ficha do paciente, Fase 1). Cada operação isola a
 * empresa ativa no {@link TenantContext} (finally {@code clear()}) e valida que o
 * contato pertence à MESMA empresa (defense-in-depth além do RLS). Edição e
 * exclusão são restritas ao autor ou a administradores. Auditoria via
 * {@link TenantAuditRecorder} ({@code AuditModule.CONTACTS}).
 */
@Service
public class ContactNoteService implements ContactNoteUseCase {

    private static final String RESOURCE = "ContactNote";

    private final ContactNoteRepository noteRepository;
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final TenantAuditRecorder auditor;

    public ContactNoteService(ContactNoteRepository noteRepository,
                              ContactRepository contactRepository,
                              UserRepository userRepository,
                              TenantAuditRecorder auditor) {
        this.noteRepository = noteRepository;
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
        this.auditor = auditor;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactNoteResponse> list(UUID companyId, UUID contactId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            Map<UUID, String> authorNames = new HashMap<>();
            return noteRepository.findByCompanyIdAndContactIdOrderByCreatedAtDesc(companyId, contactId)
                    .stream()
                    .map(note -> toResponse(note, authorName(note.getCreatedBy(), authorNames)))
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public ContactNoteResponse create(UUID companyId, UUID contactId, ContactNoteRequest request, UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            ContactNote note = ContactNote.create(companyId, contactId, request.body(), request.tooth(), createdBy);
            noteRepository.save(note);

            auditor.record(companyId, AuditAction.CREATE, AuditModule.CONTACTS, RESOURCE,
                    note.getId().toString(), "Anotação criada no paciente.", createdBy, null);
            return toResponse(note, authorName(createdBy, new HashMap<>()));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public ContactNoteResponse update(UUID companyId, UUID contactId, UUID noteId, ContactNoteRequest request,
                                      UUID actorId, boolean admin) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            ContactNote note = requireOwned(companyId, contactId, noteId);
            assertCanModify(note, actorId, admin);
            note.edit(request.body(), request.tooth());
            noteRepository.save(note);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.CONTACTS, RESOURCE,
                    note.getId().toString(), "Anotação atualizada no paciente.", actorId, null);
            return toResponse(note, authorName(note.getCreatedBy(), new HashMap<>()));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID contactId, UUID noteId, UUID actorId, boolean admin) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            ContactNote note = requireOwned(companyId, contactId, noteId);
            assertCanModify(note, actorId, admin);
            noteRepository.delete(note);

            auditor.record(companyId, AuditAction.DELETE, AuditModule.CONTACTS, RESOURCE,
                    noteId.toString(), "Anotação excluída do paciente.", actorId, null);
        } finally {
            TenantContext.clear();
        }
    }

    private void assertCanModify(ContactNote note, UUID actorId, boolean admin) {
        if (!admin && !note.isAuthoredBy(actorId)) {
            throw new CrmAccessDeniedException(
                    "Você só pode editar ou excluir as anotações criadas por você.");
        }
    }

    private void requireOwnedContact(UUID companyId, UUID contactId) {
        var contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ContactNotFoundException(contactId));
        if (!contact.getCompanyId().equals(companyId)) {
            throw new ContactNotFoundException(contactId);
        }
    }

    private ContactNote requireOwned(UUID companyId, UUID contactId, UUID noteId) {
        ContactNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ContactNoteNotFoundException(noteId));
        if (!note.getCompanyId().equals(companyId) || !note.getContactId().equals(contactId)) {
            throw new ContactNoteNotFoundException(noteId);
        }
        return note;
    }

    private String authorName(UUID createdBy, Map<UUID, String> cache) {
        if (createdBy == null) {
            return null;
        }
        if (cache.containsKey(createdBy)) {
            return cache.get(createdBy);
        }
        String name = userRepository.findById(createdBy)
                .map(com.becommerce.crm.identity.domain.User::getName).orElse(null);
        cache.put(createdBy, name);
        return name;
    }

    private static ContactNoteResponse toResponse(ContactNote note, String createdByName) {
        return new ContactNoteResponse(note.getId(), note.getContactId(), note.getBody(), note.getTooth(),
                note.getCreatedBy(), createdByName, note.getCreatedAt(), note.getUpdatedAt());
    }
}
