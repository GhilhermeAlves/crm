package com.becommerce.crm.sales.holiday.infrastructure.persistence;

import com.becommerce.crm.sales.holiday.application.port.out.HolidayRepository;
import com.becommerce.crm.sales.holiday.domain.CompanyHoliday;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class HolidayRepositoryImpl implements HolidayRepository {

    private final HolidayJpaRepository jpa;

    public HolidayRepositoryImpl(HolidayJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public CompanyHoliday save(CompanyHoliday holiday) {
        return toDomain(jpa.save(toEntity(holiday)));
    }

    @Override
    public Optional<CompanyHoliday> findById(UUID id) {
        return jpa.findById(id).map(HolidayRepositoryImpl::toDomain);
    }

    @Override
    public List<CompanyHoliday> findByCompanyId(UUID companyId) {
        return jpa.findByCompanyIdOrderByDateAsc(companyId).stream().map(HolidayRepositoryImpl::toDomain).toList();
    }

    @Override
    public List<CompanyHoliday> findByCompanyIdAndDateRange(UUID companyId, LocalDate from, LocalDate to) {
        // Feriados anuais ficam gravados no ano em que foram cadastrados: carrega os da
        // empresa (poucas linhas) e projeta as ocorrências no período pedido.
        return CompanyHoliday.occurrencesBetween(findByCompanyId(companyId), from, to);
    }

    @Override
    public Optional<CompanyHoliday> findByCompanyIdAndDate(UUID companyId, LocalDate date) {
        return jpa.findByCompanyIdAndDate(companyId, date).map(HolidayRepositoryImpl::toDomain);
    }

    @Override
    public void delete(CompanyHoliday holiday) {
        jpa.deleteById(holiday.getId());
    }

    private static HolidayJpaEntity toEntity(CompanyHoliday holiday) {
        HolidayJpaEntity entity = new HolidayJpaEntity();
        entity.setId(holiday.getId());
        entity.setCompanyId(holiday.getCompanyId());
        entity.setDate(holiday.getDate());
        entity.setName(holiday.getName());
        entity.setRecurringYearly(holiday.isRecurringYearly());
        entity.setBlocksSchedule(holiday.isBlocksSchedule());
        entity.setCreatedAt(holiday.getCreatedAt());
        entity.setUpdatedAt(holiday.getUpdatedAt());
        return entity;
    }

    private static CompanyHoliday toDomain(HolidayJpaEntity entity) {
        return CompanyHoliday.reconstitute(entity.getId(), entity.getCompanyId(), entity.getDate(), entity.getName(),
                entity.isRecurringYearly(), entity.isBlocksSchedule(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
