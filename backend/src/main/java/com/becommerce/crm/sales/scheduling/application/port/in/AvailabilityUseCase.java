package com.becommerce.crm.sales.scheduling.application.port.in;

import com.becommerce.crm.sales.scheduling.application.dto.AvailabilityResponse;
import com.becommerce.crm.sales.scheduling.application.dto.SetAvailabilityRequest;

import java.util.UUID;

public interface AvailabilityUseCase {

    AvailabilityResponse get(UUID companyId, UUID userId);

    AvailabilityResponse set(UUID companyId, UUID userId, SetAvailabilityRequest request);
}
