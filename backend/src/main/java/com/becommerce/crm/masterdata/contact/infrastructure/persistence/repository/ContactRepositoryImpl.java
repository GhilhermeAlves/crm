package com.becommerce.crm.masterdata.contact.infrastructure.persistence.repository;

import com.becommerce.crm.masterdata.contact.infrastructure.persistence.entity.ContactJpaEntity;

import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.Contact;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    @Override
    public List<Contact> findByCompanyIdActive(UUID companyId) {
        return jpaRepository.findByCompanyIdAndDeletedAtIsNullOrderByFirstNameAscLastNameAsc(companyId).stream()
                .map(ContactRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public Optional<Contact> findByCompanyIdAndPhone(UUID companyId, String phone) {
        return jpaRepository.findFirstByCompanyIdAndPhoneAndDeletedAtIsNull(companyId, phone)
                .map(ContactRepositoryImpl::toDomain);
    }

    private static ContactJpaEntity toEntity(Contact c) {
        ContactJpaEntity e = new ContactJpaEntity();
        e.setId(c.getId());
        e.setCompanyId(c.getCompanyId());
        e.setFirstName(c.getFirstName());
        e.setLastName(c.getLastName());
        e.setEmail(c.getEmail());
        e.setPhone(c.getPhone());
        e.setMobile(c.getMobile());
        e.setNotes(c.getNotes());
        e.setBirthDate(c.getBirthDate());
        e.setCpf(c.getCpf());
        e.setRg(c.getRg());
        e.setRgIssuer(c.getRgIssuer());
        e.setGender(c.getGender());
        e.setMaritalStatus(c.getMaritalStatus());
        e.setProfessionalStatus(c.getProfessionalStatus());
        e.setCreatedAt(c.getCreatedAt());
        e.setUpdatedAt(c.getUpdatedAt());
        e.setDeletedAt(c.getDeletedAt());
        return e;
    }

    private static Contact toDomain(ContactJpaEntity e) {
        return Contact.reconstitute(
                e.getId(), e.getCompanyId(), e.getFirstName(), e.getLastName(),
                e.getEmail(), e.getPhone(), e.getMobile(), e.getNotes(),
                e.getBirthDate(), e.getCpf(), e.getRg(), e.getRgIssuer(),
                e.getGender(), e.getMaritalStatus(), e.getProfessionalStatus(),
                e.getCreatedAt(), e.getUpdatedAt(), e.getDeletedAt());
    }
}