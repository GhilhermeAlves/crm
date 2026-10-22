package com.becommerce.crm.sales.holiday.domain;

import com.becommerce.crm.sales.holiday.domain.exception.HolidayValidationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class CompanyHoliday {

    private static final int NAME_MAX_LENGTH = 120;

    private final UUID id;
    private final UUID companyId;
    private LocalDate date;
    private String name;
    private boolean recurringYearly;
    private boolean blocksSchedule;
    private final Instant createdAt;
    private Instant updatedAt;

    private CompanyHoliday(UUID id, UUID companyId, LocalDate date, String name, boolean recurringYearly,
                           boolean blocksSchedule, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.date = date;
        this.name = name;
        this.recurringYearly = recurringYearly;
        this.blocksSchedule = blocksSchedule;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CompanyHoliday create(UUID companyId, LocalDate date, String name, Boolean recurringYearly,
                                        Boolean blocksSchedule) {
        if (companyId == null) {
            throw new HolidayValidationException("Empresa é obrigatória.");
        }
        if (date == null) {
            throw new HolidayValidationException("Data é obrigatória.");
        }
        validateName(name);
        Instant now = Instant.now();
        return new CompanyHoliday(UUID.randomUUID(), companyId, date, name.trim(),
                recurringYearly != null && recurringYearly, blocksSchedule == null || blocksSchedule, now, now);
    }

    public static CompanyHoliday reconstitute(UUID id, UUID companyId, LocalDate date, String name,
                                              boolean recurringYearly, boolean blocksSchedule, Instant createdAt,
                                              Instant updatedAt) {
        return new CompanyHoliday(id, companyId, date, name, recurringYearly, blocksSchedule, createdAt, updatedAt);
    }

    /**
     * Ocorrências dentro de [from, to] (inclusive). Feriados anuais se repetem a partir do ano
     * cadastrado; 29/02 cai em 28/02 nos anos não bissextos. Cada ocorrência mantém o id original.
     */
    public static List<CompanyHoliday> occurrencesBetween(Collection<CompanyHoliday> holidays,
                                                          LocalDate from, LocalDate to) {
        List<CompanyHoliday> result = new ArrayList<>();
        for (CompanyHoliday h : holidays) {
            if (!h.recurringYearly) {
                if (!h.date.isBefore(from) && !h.date.isAfter(to)) {
                    result.add(h);
                }
                continue;
            }
            for (int year = Math.max(from.getYear(), h.date.getYear()); year <= to.getYear(); year++) {
                LocalDate occurrence = h.date.withYear(year);
                if (!occurrence.isBefore(from) && !occurrence.isAfter(to)) {
                    result.add(new CompanyHoliday(h.id, h.companyId, occurrence, h.name, true,
                            h.blocksSchedule, h.createdAt, h.updatedAt));
                }
            }
        }
        result.sort(Comparator.comparing(CompanyHoliday::getDate));
        return result;
    }

    public void update(LocalDate date, String name, Boolean recurringYearly, Boolean blocksSchedule) {
        if (date != null) {
            this.date = date;
        }
        if (name != null) {
            validateName(name);
            this.name = name.trim();
        }
        if (recurringYearly != null) {
            this.recurringYearly = recurringYearly;
        }
        if (blocksSchedule != null) {
            this.blocksSchedule = blocksSchedule;
        }
        touch();
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new HolidayValidationException("O nome do feriado é obrigatório.");
        }
        if (name.trim().length() > NAME_MAX_LENGTH) {
            throw new HolidayValidationException("O nome do feriado deve ter no máximo " + NAME_MAX_LENGTH + " caracteres.");
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getName() {
        return name;
    }

    public boolean isRecurringYearly() {
        return recurringYearly;
    }

    public boolean isBlocksSchedule() {
        return blocksSchedule;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
