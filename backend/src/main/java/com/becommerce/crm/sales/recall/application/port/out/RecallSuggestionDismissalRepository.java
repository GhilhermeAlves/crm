package com.becommerce.crm.sales.recall.application.port.out;

import java.util.UUID;

public interface RecallSuggestionDismissalRepository {

    boolean existsByCompanyAndContact(UUID companyId, UUID contactId);

    void save(UUID companyId, UUID contactId, UUID createdBy);
}
