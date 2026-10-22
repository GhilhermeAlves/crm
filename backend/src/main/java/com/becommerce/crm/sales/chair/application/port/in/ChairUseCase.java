package com.becommerce.crm.sales.chair.application.port.in;

import com.becommerce.crm.sales.chair.application.dto.ChairResponse;
import com.becommerce.crm.sales.chair.application.dto.CreateChairRequest;
import com.becommerce.crm.sales.chair.application.dto.UpdateChairRequest;

import java.util.List;
import java.util.UUID;

public interface ChairUseCase {

    ChairResponse create(UUID companyId, CreateChairRequest request);

    ChairResponse getById(UUID companyId, UUID chairId);

    ChairResponse update(UUID companyId, UUID chairId, UpdateChairRequest request);

    void delete(UUID companyId, UUID chairId);

    List<ChairResponse> listByCompany(UUID companyId);

    /** Indica se a cadeira existe e pertence à empresa informada. */
    boolean existsInCompany(UUID companyId, UUID chairId);
}
