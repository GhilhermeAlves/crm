package com.becommerce.auth.domain.identity;

import java.util.UUID;

/**
 * Projeção de leitura do usuário CRM (tabela {@code users}), usada pela
 * resolução de identidade. Não é um aggregate — é um read model imutável.
 */
public record User(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String name,
        String keycloakSub,
        UUID companyId,
        boolean active) {
}
