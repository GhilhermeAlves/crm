package com.becommerce.crm.sales.chair.application.port.out;

import com.becommerce.crm.sales.chair.domain.Chair;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChairRepository {

    Chair save(Chair chair);

    Optional<Chair> findById(UUID id);

    List<Chair> findByCompanyId(UUID companyId);

    Optional<Chair> findByCompanyIdAndName(UUID companyId, String name);

    void delete(Chair chair);
}
