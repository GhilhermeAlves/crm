package com.becommerce.crm.masterdata.company.mail;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanyMailSettingsJpaRepository extends JpaRepository<CompanyMailSettingsJpaEntity, UUID> {
}
