package com.becommerce.crm.application.activity.port.output;

import com.becommerce.crm.domain.activity.Activity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    /** Data da atividade mais recente de um contato (usado pelo Customer 360). */
    java.util.Optional<java.time.LocalDateTime> findLatestActivityAtByContactId(UUID contactId);

    /**
     * Data da atividade mais recente de cada oportunidade informada, em uma única
     * consulta em lote (evita N+1 no Customer 360 / análise contextual).
     */
    Map<UUID, java.time.LocalDateTime> findLatestActivityAtByOpportunityIds(Collection<UUID> opportunityIds);

    void delete(Activity activity);
}