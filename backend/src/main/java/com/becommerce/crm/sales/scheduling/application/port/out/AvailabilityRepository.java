package com.becommerce.crm.sales.scheduling.application.port.out;

import com.becommerce.crm.sales.scheduling.domain.AvailabilityOverride;
import com.becommerce.crm.sales.scheduling.domain.AvailabilityWindow;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AvailabilityRepository {

    List<AvailabilityWindow> findWindowsByUserId(UUID companyId, UUID userId);

    void replaceWindows(UUID companyId, UUID userId, List<AvailabilityWindow> windows);

    List<AvailabilityOverride> findOverridesByUserId(UUID companyId, UUID userId);

    List<AvailabilityOverride> findOverridesByUserIdAndDateRange(UUID companyId, UUID userId,
                                                                 LocalDate from, LocalDate to);

    void replaceOverrides(UUID companyId, UUID userId, List<AvailabilityOverride> overrides);

    Optional<String> findTimezone(UUID userId);

    void saveTimezone(UUID userId, UUID companyId, String timezone);
}
