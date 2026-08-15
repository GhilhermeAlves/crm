package com.becommerce.crm.application.omnichannel.port.output;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolve a empresa proprietária de um canal a partir da referência externa
 * (number/phone_number_id) sem depender de sessão de usuário — usado pelo
 * webhook. Implementado sobre a função SECURITY DEFINER
 * {@code app.resolve_channel_company} (bypassa RLS apenas nesta consulta de
 * mapeamento; a persistência segue sob RLS FORCE).
 */
public interface OmnichannelCompanyResolver {

    /** Retorna o companyId do canal, ou vazio se não encontrado. */
    Optional<UUID> resolveCompanyByChannelReference(String channelExternalId);
}