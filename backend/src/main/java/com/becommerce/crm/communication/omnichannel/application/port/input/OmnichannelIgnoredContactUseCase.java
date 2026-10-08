package com.becommerce.crm.communication.omnichannel.application.port.input;

import com.becommerce.crm.communication.omnichannel.application.dto.IgnoredContactRequest;
import com.becommerce.crm.communication.omnichannel.domain.IgnoredContact;

import java.util.List;
import java.util.UUID;

public interface OmnichannelIgnoredContactUseCase {

    List<IgnoredContact> list(UUID companyId);

    IgnoredContact add(UUID companyId, IgnoredContactRequest request);

    void remove(UUID companyId, UUID id);
}
