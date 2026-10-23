package com.becommerce.crm.masterdata.contact.registration.application.port;

import com.becommerce.crm.masterdata.contact.registration.domain.RegistrationLink;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RegistrationLinkRepository {

    RegistrationLink save(RegistrationLink link);

    /** Busca pelo hash do token, com lock da linha (submit concorrente). */
    Optional<RegistrationLink> findByTokenHashForUpdate(String tokenHash);

    Optional<RegistrationLink> findByTokenHash(String tokenHash);

    /** Marca como usados os links ainda não usados do contato. */
    int invalidateUnused(UUID companyId, UUID contactId, LocalDateTime now);

    /** Atualiza somente {@code used_at} do link. */
    void markUsed(UUID linkId, LocalDateTime now);
}
