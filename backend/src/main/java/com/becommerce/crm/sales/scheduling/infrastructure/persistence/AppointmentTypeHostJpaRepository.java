package com.becommerce.crm.sales.scheduling.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AppointmentTypeHostJpaRepository extends JpaRepository<AppointmentTypeHostJpaEntity, AppointmentTypeHostJpaEntity.PK> {

    List<AppointmentTypeHostJpaEntity> findByAppointmentTypeId(UUID appointmentTypeId);

    void deleteByAppointmentTypeId(UUID appointmentTypeId);
}
