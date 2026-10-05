package com.becommerce.crm.sales.scheduling.application.port.in;

import com.becommerce.crm.sales.scheduling.application.dto.BirthdayMessageSettingsDto;

import java.util.Optional;
import java.util.UUID;

public interface BirthdayMessageSettingsUseCase {
    Optional<BirthdayMessageSettingsDto> get(UUID companyId);
    BirthdayMessageSettingsDto save(UUID companyId, BirthdayMessageSettingsDto request);
}
