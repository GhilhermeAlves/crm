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
    public List<Contact> findPatientsByCompanyIdActive(UUID companyId) {
        return jpaRepository.findByCompanyIdAndPatientTrueAndDeletedAtIsNullOrderByFirstNameAscLastNameAsc(companyId)
                .stream()
                .map(ContactRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public Optional<Contact> findByCompanyIdAndPhone(UUID companyId, String phone) {
        return jpaRepository.findFirstByCompanyIdAndPhoneAndDeletedAtIsNull(companyId, phone)
                .map(ContactRepositoryImpl::toDomain);
    }

    @Override
    public boolean existsByRecordNumber(UUID companyId, String recordNumber, UUID excludeContactId) {
        if (excludeContactId == null) {
            return jpaRepository.existsByCompanyIdAndRecordNumber(companyId, recordNumber);
        }
        return jpaRepository.existsByCompanyIdAndRecordNumberAndIdNot(companyId, recordNumber, excludeContactId);
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
        e.setZipCode(c.getZipCode());
        e.setStreet(c.getStreet());
        e.setAddressNumber(c.getAddressNumber());
        e.setAddressComplement(c.getAddressComplement());
        e.setNeighborhood(c.getNeighborhood());
        e.setCity(c.getCity());
        e.setState(c.getState());
        e.setHealthPlan(c.getHealthPlan());
        e.setHealthPlanNumber(c.getHealthPlanNumber());
        e.setRecordNumber(c.getRecordNumber());
        e.setReferralSource(c.getReferralSource());
        e.setHasGuardian(c.isHasGuardian());
        e.setGuardianName(c.getGuardianName());
        e.setGuardianCpf(c.getGuardianCpf());
        e.setGuardianPhone(c.getGuardianPhone());
        e.setCreatedAt(c.getCreatedAt());
        e.setUpdatedAt(c.getUpdatedAt());
        e.setDeletedAt(c.getDeletedAt());
        e.setPatient(c.isPatient());
        e.setPatientSince(c.getPatientSince());
        return e;
    }

    private static Contact toDomain(ContactJpaEntity e) {
        Contact contact = Contact.reconstitute(
                e.getId(), e.getCompanyId(), e.getFirstName(), e.getLastName(),
                e.getEmail(), e.getPhone(), e.getMobile(), e.getNotes(),
                e.getBirthDate(), e.getCpf(), e.getRg(), e.getRgIssuer(),
                e.getGender(), e.getMaritalStatus(), e.getProfessionalStatus(),
                e.getZipCode(), e.getStreet(), e.getAddressNumber(), e.getAddressComplement(),
                e.getNeighborhood(), e.getCity(), e.getState(), e.getHealthPlan(),
                e.getHealthPlanNumber(), e.getRecordNumber(), e.getReferralSource(),
                e.isHasGuardian(), e.getGuardianName(), e.getGuardianCpf(), e.getGuardianPhone(),
                e.getCreatedAt(), e.getUpdatedAt(), e.getDeletedAt());
        contact.restorePatient(e.isPatient(), e.getPatientSince());
        return contact;
    }
}