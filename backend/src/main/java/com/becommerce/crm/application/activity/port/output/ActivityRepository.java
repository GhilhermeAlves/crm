package com.becommerce.crm.application.activity.port.output;

import com.becommerce.crm.domain.activity.Activity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityRepository {

    Activity save(Activity activity);

    Optional<Activity> findById(UUID id);

    List<Activity> findByCompanyId(UUID companyId);

    List<Activity> findByContactId(UUID contactId);

    List<Activity> findByOpportunityId(UUID opportunityId);

    List<Activity> findRecentByCompanyId(UUID companyId, int limit);

    /** Data da atividade mais recente de uma oportunidade (usado pela inteligência operacional). */
    java.util.Optional<java.time.LocalDateTime> findLatestActivityAtByOpportunityId(UUID opportunityId);

    void delete(Activity activity);
}