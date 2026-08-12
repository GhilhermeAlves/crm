package com.becommerce.crm.application.contact.port.input;

import com.becommerce.crm.application.contact.dto.ContactResponse;
import com.becommerce.crm.application.contact.dto.CreateContactRequest;

import java.util.UUID;

/** Casos de uso de contatos (Sprint 8.6). */
public interface ContactUseCase {

    /**
     * Cria um contato respeitando {@code max_contacts} da empresa. Lança
     * {@code QuotaExceededException} se o limite for atingido.
     */
    ContactResponse create(UUID companyId, CreateContactRequest request, UUID createdBy);

    ContactResponse getById(UUID companyId, UUID contactId);
}