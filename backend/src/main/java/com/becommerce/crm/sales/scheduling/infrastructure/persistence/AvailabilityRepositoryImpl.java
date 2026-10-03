package com.becommerce.crm.sales.scheduling.infrastructure.persistence;

import com.becommerce.crm.sales.scheduling.application.port.out.AvailabilityRepository;
import com.becommerce.crm.sales.scheduling.domain.AvailabilityOverride;
import com.becommerce.crm.sales.scheduling.domain.AvailabilityWindow;
import com.becommerce.crm.sales.scheduling.domain.TimeRange;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class AvailabilityRepositoryImpl implements AvailabilityRepository {

    private final AvailabilityRuleJpaRepository ruleJpa;
    private final AvailabilityOverrideJpaRepository overrideJpa;
    private final UserSchedulingSettingsJpaRepository settingsJpa;

    public AvailabilityRepositoryImpl(AvailabilityRuleJpaRepository ruleJpa,
                                      AvailabilityOverrideJpaRepository overrideJpa,
                                      UserSchedulingSettingsJpaRepository settingsJpa) {
        this.ruleJpa = ruleJpa;
        this.overrideJpa = overrideJpa;
        this.settingsJpa = settingsJpa;
    }

    @Override
    public List<AvailabilityWindow> findWindowsByUserId(UUID companyId, UUID userId) {
        return ruleJpa.findByCompanyIdAndUserId(companyId, userId).stream()
                .map(e -> new AvailabilityWindow(e.getId(), e.getCompanyId(), e.getUserId(),
                        isoDayOfWeek(e.getWeekday()), e.getStartTime(), e.getEndTime()))
                .toList();
    }

    @Override
    @Transactional
    public void replaceWindows(UUID companyId, UUID userId, List<AvailabilityWindow> windows) {
        ruleJpa.deleteByCompanyIdAndUserId(companyId, userId);
        for (AvailabilityWindow w : windows) {
            AvailabilityRuleJpaEntity e = new AvailabilityRuleJpaEntity();
            e.setId(w.id());
            e.setCompanyId(companyId);
            e.setUserId(userId);
            e.setWeekday(isoWeekdayNumber(w.weekday()));
            e.setStartTime(w.start());
            e.setEndTime(w.end());
            ruleJpa.save(e);
        }
    }

    @Override
    public List<AvailabilityOverride> findOverridesByUserId(UUID companyId, UUID userId) {
        return groupOverrides(overrideJpa.findByCompanyIdAndUserId(companyId, userId));
    }

    @Override
    public List<AvailabilityOverride> findOverridesByUserIdAndDateRange(UUID companyId, UUID userId,
                                                                        LocalDate from, LocalDate to) {
        return groupOverrides(overrideJpa.findByUserIdAndDateRange(companyId, userId, from, to));
    }

    @Override
    @Transactional
    public void replaceOverrides(UUID companyId, UUID userId, List<AvailabilityOverride> overrides) {
        overrideJpa.deleteByCompanyIdAndUserId(companyId, userId);
        for (AvailabilityOverride o : overrides) {
            if (o.isUnavailable()) {
                AvailabilityOverrideJpaEntity e = new AvailabilityOverrideJpaEntity();
                e.setId(o.id());
                e.setCompanyId(companyId);
                e.setUserId(userId);
                e.setDate(o.date());
                overrideJpa.save(e);
            } else {
                for (TimeRange w : o.windows()) {
                    AvailabilityOverrideJpaEntity e = new AvailabilityOverrideJpaEntity();
                    e.setId(UUID.randomUUID());
                    e.setCompanyId(companyId);
                    e.setUserId(userId);
                    e.setDate(o.date());
                    e.setStartTime(w.start());
                    e.setEndTime(w.end());
                    overrideJpa.save(e);
                }
            }
        }
    }

    @Override
    public Optional<String> findTimezone(UUID userId) {
        return settingsJpa.findById(userId).map(UserSchedulingSettingsJpaEntity::getTimezone);
    }

    @Override
    @Transactional
    public void saveTimezone(UUID userId, UUID companyId, String timezone) {
        UserSchedulingSettingsJpaEntity e = settingsJpa.findById(userId).orElseGet(() -> {
            UserSchedulingSettingsJpaEntity n = new UserSchedulingSettingsJpaEntity();
            n.setUserId(userId);
            n.setCompanyId(companyId);
            return n;
        });
        e.setTimezone(timezone);
        e.setUpdatedAt(Instant.now());
        settingsJpa.save(e);
    }

    private static DayOfWeek isoDayOfWeek(short dbValue) {
        return DayOfWeek.of(dbValue);
    }

    private static short isoWeekdayNumber(DayOfWeek dow) {
        return (short) dow.getValue();
    }

    private List<AvailabilityOverride> groupOverrides(List<AvailabilityOverrideJpaEntity> entities) {
        Map<LocalDate, List<AvailabilityOverrideJpaEntity>> grouped = entities.stream()
                .collect(Collectors.groupingBy(AvailabilityOverrideJpaEntity::getDate));
        return grouped.entrySet().stream().map(entry -> {
            LocalDate date = entry.getKey();
            List<AvailabilityOverrideJpaEntity> rows = entry.getValue();
            UUID firstId = rows.getFirst().getId();
            UUID companyId = rows.getFirst().getCompanyId();
            UUID userId = rows.getFirst().getUserId();
            List<TimeRange> windows = rows.stream()
                    .filter(r -> r.getStartTime() != null && r.getEndTime() != null)
                    .map(r -> new TimeRange(r.getStartTime(), r.getEndTime()))
                    .toList();
            return new AvailabilityOverride(firstId, companyId, userId, date, windows);
        }).toList();
    }
}
