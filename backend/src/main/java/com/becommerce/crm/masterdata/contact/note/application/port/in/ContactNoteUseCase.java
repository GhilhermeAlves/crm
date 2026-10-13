package com.becommerce.crm.masterdata.contact.note.application.port.in;

import com.becommerce.crm.masterdata.contact.note.application.dto.ContactNoteRequest;
import com.becommerce.crm.masterdata.contact.note.application.dto.ContactNoteResponse;

import java.util.UUID;

import java.util.List;

/**
 * Caso de uso das anotações do paciente (ficha do paciente, Fase 1).
 * {@code admin} indica se o ator possui papel de administrador (ADMIN/OWNER/
 * SUPER_ADMIN); edição/exclusão é permitida ao autor ou a administradores.
 */
public interface ContactNoteUseCase {

    List<ContactNoteResponse> list(UUID companyId, UUID contactId);

    ContactNoteResponse create(UUID companyId, UUID contactId, ContactNoteRequest request, UUID createdBy);

    ContactNoteResponse update(UUID companyId, UUID contactId, UUID noteId, ContactNoteRequest request,
                               UUID actorId, boolean admin);

    void delete(UUID companyId, UUID contactId, UUID noteId, UUID actorId, boolean admin);
}
