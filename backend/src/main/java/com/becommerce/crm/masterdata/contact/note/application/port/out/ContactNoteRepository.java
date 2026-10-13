package com.becommerce.crm.masterdata.contact.note.application.port.out;

import com.becommerce.crm.masterdata.contact.note.domain.ContactNote;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContactNoteRepository {

    ContactNote save(ContactNote note);

    Optional<ContactNote> findById(UUID id);

    List<ContactNote> findByCompanyIdAndContactIdOrderByCreatedAtDesc(UUID companyId, UUID contactId);

    void delete(ContactNote note);
}
