package com.becommerce.crm.sales.scheduling.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BirthdayMessageSettingsJpaRepository extends JpaRepository<BirthdayMessageSettingsJpaEntity, UUID> {
}
