package com.becommerce.crm.sales.scheduling.application.port.out;

import com.becommerce.crm.sales.scheduling.domain.ScheduleBlock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleBlockRepository {

    ScheduleBlock save(ScheduleBlock block);

    Optional<ScheduleBlock> findById(UUID id);

    List<ScheduleBlock> findByHostIdAndRange(UUID companyId, UUID hostId, Instant from, Instant to);

    List<ScheduleBlock> findByCompanyIdAndRange(UUID companyId, Instant from, Instant to, List<UUID> hostIds);

    void delete(ScheduleBlock block);
}
