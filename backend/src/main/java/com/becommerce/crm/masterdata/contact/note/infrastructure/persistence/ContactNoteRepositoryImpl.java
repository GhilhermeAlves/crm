package com.becommerce.crm.masterdata.contact.note.infrastructure.persistence;

import com.becommerce.crm.masterdata.contact.note.application.port.out.ContactNoteRepository;
import com.becommerce.crm.masterdata.contact.note.domain.ContactNote;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ContactNoteRepositoryImpl implements ContactNoteRepository {

    private final ContactNoteJpaRepository jpaRepository;

    public ContactNoteRepositoryImpl(ContactNoteJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ContactNote save(ContactNote note) {
        return toDomain(jpaRepository.save(toEntity(note)));
    }

    @Override
    public Optional<ContactNote> findById(UUID id) {
        return jpaRepository.findById(id).map(ContactNoteRepositoryImpl::toDomain);
    }

    @Override
    public List<ContactNote> findByCompanyIdAndContactIdOrderByCreatedAtDesc(UUID companyId, UUID contactId) {
        return jpaRepository.findByCompanyIdAndContactIdOrderByCreatedAtDesc(companyId, contactId).stream()
                .map(ContactNoteRepositoryImpl::toDomain).toList();
    }

    @Override
    public void delete(ContactNote note) {
        jpaRepository.deleteById(note.getId());
    }

    private static ContactNoteJpaEntity toEntity(ContactNote note) {
        ContactNoteJpaEntity e = new ContactNoteJpaEntity();
        e.setId(note.getId());
        e.setCompanyId(note.getCompanyId());
        e.setContactId(note.getContactId());
        e.setBody(note.getBody());
        e.setCreatedBy(note.getCreatedBy());
        e.setCreatedAt(note.getCreatedAt());
        e.setUpdatedAt(note.getUpdatedAt());
        return e;
    }

    private static ContactNote toDomain(ContactNoteJpaEntity e) {
        return ContactNote.reconstitute(e.getId(), e.getCompanyId(), e.getContactId(), e.getBody(),
                e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
