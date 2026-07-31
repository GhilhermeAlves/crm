package com.becommerce.auth.application.identity.port.output;

import com.becommerce.auth.domain.identity.User;

import java.util.Optional;

/**
 * Porta de saída para leitura de usuários CRM. Resolução sempre por identidade
 * derivada do JWT (sub → email), nunca por entrada arbitrária do cliente.
 */
public interface UserRepository {

    Optional<User> findByKeycloakSub(String keycloakSub);

    Optional<User> findByEmail(String email);
}
