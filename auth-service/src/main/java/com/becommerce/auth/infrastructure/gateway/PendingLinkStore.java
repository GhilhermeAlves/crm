package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.PendingLink;

import java.util.Optional;

/**
 * Storage do vínculo pendente (Sprint 7.2, Caso B). Curto e efêmero: TTL de
 * poucos minutos, uso único no sucesso e sem dados críticos além dos tokens do
 * servidor. Em memória (padrão) é suficiente para o fluxo; para múltiplas
 * réplicas a mesma decisão de {@link GatewaySessionStore} (Redis) se aplica —
 * ver pendência em REPORT 7.2.
 */
public interface PendingLinkStore {

    void put(PendingLink pendingLink);

    /**
     * Retorna o vínculo pendente se existir e ainda não expirou. O consumo
     * (remoção) é explícito via {@link #remove(String)} — o fluxo de senha
     * incorreta NÃO remove (permite nova tentativa).
     */
    Optional<PendingLink> get(String token);

    void remove(String token);

    void purgeExpired();
}
