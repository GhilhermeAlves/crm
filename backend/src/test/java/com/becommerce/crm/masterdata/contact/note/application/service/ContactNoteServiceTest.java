package com.becommerce.crm.masterdata.contact.note.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.Contact;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactNotFoundException;
import com.becommerce.crm.masterdata.contact.note.application.dto.ContactNoteRequest;
import com.becommerce.crm.masterdata.contact.note.application.dto.ContactNoteResponse;
import com.becommerce.crm.masterdata.contact.note.application.port.out.ContactNoteRepository;
import com.becommerce.crm.masterdata.contact.note.domain.ContactNote;
import com.becommerce.crm.masterdata.contact.note.domain.exception.ContactNoteNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactNoteServiceTest {

    @Mock ContactNoteRepository noteRepository;
    @Mock ContactRepository contactRepository;
    @Mock UserRepository userRepository;
    @Mock TenantAuditRecorder auditor;
    @InjectMocks ContactNoteService service;

    private final UUID companyId = UUID.randomUUID();
    private final UUID contactId = UUID.randomUUID();
    private final UUID authorId = UUID.randomUUID();

    private void ownedContact() {
        Contact contact = mock(Contact.class);
        when(contact.getCompanyId()).thenReturn(companyId);
        when(contactRepository.findById(contactId)).thenReturn(Optional.of(contact));
    }

    private ContactNote note(UUID id, UUID author, LocalDateTime createdAt) {
        return ContactNote.reconstitute(id, companyId, contactId, "Anotação", author, createdAt, createdAt);
    }

    @Test
    void listsNotesOrderedNewestFirstWithAuthorName() {
        ownedContact();
        LocalDateTime earlier = LocalDateTime.now().minusDays(1);
        UUID newerId = UUID.randomUUID();
        ContactNote newer = note(newerId, authorId, LocalDateTime.now());
        ContactNote older = note(UUID.randomUUID(), authorId, earlier);
        when(noteRepository.findByCompanyIdAndContactIdOrderByCreatedAtDesc(companyId, contactId))
                .thenReturn(List.of(newer, older));
        User author = mock(User.class);
        when(author.getName()).thenReturn("Dra. Ana");
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));

        List<ContactNoteResponse> result = service.list(companyId, contactId);

        assertEquals(2, result.size());
        assertEquals(newerId, result.get(0).id());
        assertEquals("Dra. Ana", result.get(0).createdByName());
    }

    @Test
    void listsNotesFailsWhenContactMissing() {
        when(contactRepository.findById(contactId)).thenReturn(Optional.empty());

        assertThrows(ContactNotFoundException.class, () -> service.list(companyId, contactId));
    }

    @Test
    void createsNoteForOwnedContact() {
        ownedContact();
        when(noteRepository.save(any(ContactNote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(authorId)).thenReturn(Optional.empty());

        ContactNoteResponse response = service.create(companyId, contactId,
                new ContactNoteRequest("Paciente relatou melhora."), authorId);

        assertEquals("Paciente relatou melhora.", response.body());
        assertEquals(contactId, response.contactId());
        verify(noteRepository).save(any(ContactNote.class));
    }

    @Test
    void updateByNonAuthorIsDenied() {
        ownedContact();
        UUID noteId = UUID.randomUUID();
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note(noteId, authorId, LocalDateTime.now())));

        assertThrows(CrmAccessDeniedException.class, () -> service.update(companyId, contactId, noteId,
                new ContactNoteRequest("Alterado"), UUID.randomUUID(), false));
        verify(noteRepository, never()).save(any(ContactNote.class));
    }

    @Test
    void updateByAuthorSucceeds() {
        ownedContact();
        UUID noteId = UUID.randomUUID();
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note(noteId, authorId, LocalDateTime.now())));
        when(noteRepository.save(any(ContactNote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(authorId)).thenReturn(Optional.empty());

        ContactNoteResponse response = service.update(companyId, contactId, noteId,
                new ContactNoteRequest("  Alterado  "), authorId, false);

        assertEquals("Alterado", response.body());
    }

    @Test
    void deleteByNonAuthorAdminSucceeds() {
        ownedContact();
        UUID noteId = UUID.randomUUID();
        ContactNote existing = note(noteId, authorId, LocalDateTime.now());
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(existing));

        service.delete(companyId, contactId, noteId, UUID.randomUUID(), true);

        verify(noteRepository).delete(existing);
    }

    @Test
    void deleteByNonAuthorNonAdminIsDenied() {
        ownedContact();
        UUID noteId = UUID.randomUUID();
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note(noteId, authorId, LocalDateTime.now())));

        assertThrows(CrmAccessDeniedException.class, () -> service.delete(companyId, contactId, noteId,
                UUID.randomUUID(), false));
        verify(noteRepository, never()).delete(any(ContactNote.class));
    }

    @Test
    void updateFailsWhenNoteBelongsToAnotherContact() {
        ownedContact();
        UUID noteId = UUID.randomUUID();
        ContactNote other = ContactNote.reconstitute(noteId, companyId, UUID.randomUUID(), "x", authorId,
                LocalDateTime.now(), LocalDateTime.now());
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(other));

        assertThrows(ContactNoteNotFoundException.class, () -> service.update(companyId, contactId, noteId,
                new ContactNoteRequest("Alterado"), authorId, false));
    }
}
