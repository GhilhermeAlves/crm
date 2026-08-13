package com.becommerce.crm.domain.lead.exception;

import java.util.UUID;

/**
 * Violação do índice único {@code (contact_id, company_id)} (V016): já existe
 * um lead para o contato nesta empresa.
 */
public class DuplicateLeadException extends RuntimeException {

    public DuplicateLeadException(UUID contactId, UUID companyId) {
        super("Já existe um lead para este contato nesta empresa (contact=" + contactId + ", company=" + companyId + ").");
    }
}