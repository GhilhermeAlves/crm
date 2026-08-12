package com.becommerce.crm.infrastructure.contact.persistence;

import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.domain.contact.Contact;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ContactRepositoryImpl implements ContactRepository {

    private final ContactJpaRepository jpaRepository;

    public ContactRepositoryImpl(ContactJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Contact save(Contact contact) {
        return toDomain(jpaRepository.save(toEntity(contact)));
    }

    @Override
    public Optional<Contact> findById(UUID id) {
        return jpaRepository.findById(id).map(ContactRepositoryImpl::toDomain);
    }

    @Override
    public long countActiveByCompanyId(UUID companyId) {
        return jpaRepository.countByCompanyIdAndDeletedAtIsNull(companyId);
    }

    private static ContactJpaEntity toEntity(Contact c) {
        ContactJpaEntity e = new ContactJpaEntity();
        e.setId(c.getId());
        e.setCompanyId(c.getCompanyId());
        e.setFirstName(c.getFirstName());
        e.setLastName(c.getLastName());
        e.setEmail(c.getEmail());
        e.setPhone(c.getPhone());
        e.setNotes(c.getNotes());
        e.setCreatedAt(c.getCreatedAt());
        e.setUpdatedAt(c.getUpdatedAt());
        e.setDeletedAt(c.getDeletedAt());
        return e;
    }

    private static Contact toDomain(ContactJpaEntity e) {
        return Contact.reconstitute(
                e.getId(), e.getCompanyId(), e.getFirstName(), e.getLastName(),
                e.getEmail(), e.getPhone(), e.getNotes(),
                e.getCreatedAt(), e.getUpdatedAt(), e.getDeletedAt());
    }
}