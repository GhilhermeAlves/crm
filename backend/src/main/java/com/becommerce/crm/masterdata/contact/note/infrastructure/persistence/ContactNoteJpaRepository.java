package com.becommerce.crm.masterdata.contact.note.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContactNoteJpaRepository extends JpaRepository<ContactNoteJpaEntity, UUID> {

    List<ContactNoteJpaEntity> findByCompanyIdAndContactIdOrderByCreatedAtDesc(UUID companyId, UUID contactId);
}
