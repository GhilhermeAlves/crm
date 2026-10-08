package com.becommerce.crm.communication.omnichannel.application.port.output;

import com.becommerce.crm.communication.omnichannel.domain.IgnoredContact;

import java.util.List;
import java.util.UUID;

/** Contatos ignorados por empresa (RLS FORCE via GUC). */
public interface OmnichannelIgnoredContactRepository {

    List<IgnoredContact> findByCompany(UUID companyId);

    boolean existsByCompanyAndPhone(UUID companyId, String phone);

    IgnoredContact save(IgnoredContact contact);

    void delete(UUID companyId, UUID id);
}
