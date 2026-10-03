package com.becommerce.crm.sales.scheduling.application.port.in;

import com.becommerce.crm.sales.scheduling.application.dto.BlockResponse;
import com.becommerce.crm.sales.scheduling.application.dto.CreateBlockRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BlockUseCase {

    BlockResponse create(UUID companyId, CreateBlockRequest request, UUID createdBy);

    BlockResponse getById(UUID companyId, UUID blockId);

    void delete(UUID companyId, UUID blockId);

    List<BlockResponse> list(UUID companyId, Instant from, Instant to, List<UUID> hostIds);
}
