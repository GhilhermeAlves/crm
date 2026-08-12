package com.becommerce.crm.domain.company;

import java.util.UUID;

/**
 * Lançada ao tentar excluir a empresa em que o usuário está autenticado/logado.
 * Evita que usuários (incluindo SUPER_ADMIN) fiquem órfãos do próprio tenant.
 */
public class CompanyDeletionForbiddenException extends RuntimeException {

    public CompanyDeletionForbiddenException(UUID companyId) {
        super("Você não pode excluir a empresa em que está logado (" + companyId + ").");
    }
}