package com.becommerce.crm.sales.holiday.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HolidayJpaRepository extends JpaRepository<HolidayJpaEntity, UUID> {

    List<HolidayJpaEntity> findByCompanyIdOrderByDateAsc(UUID companyId);

    List<HolidayJpaEntity> findByCompanyIdAndDateBetweenOrderByDateAsc(UUID companyId, LocalDate from, LocalDate to);

    Optional<HolidayJpaEntity> findByCompanyIdAndDate(UUID companyId, LocalDate date);
}
