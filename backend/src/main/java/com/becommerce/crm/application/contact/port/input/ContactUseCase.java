package com.becommerce.crm.application.contact.port.input;

import com.becommerce.crm.application.contact.dto.ContactResponse;
import com.becommerce.crm.application.contact.dto.CreateContactRequest;
import com.becommerce.crm.application.contact.dto.UpdateContactRequest;

import java.util.List;
import java.util.UUID;

/** Casos de uso de contatos (Sprint 8.6). */
public interface ContactUseCase {

    /**
     * Cria um contato respeitando {@code max_contacts} da empresa. Lança
     * {@code QuotaExceededException} se o limite for atingido.
     */
    ContactResponse create(UUID companyId, CreateContactRequest request, UUID createdBy);

    ContactResponse getById(UUID companyId, UUID contactId);

    /** Lista os contatos ativos da empresa (diretório de clientes). */
    List<ContactResponse> listByCompany(UUID companyId);

    /** Busca contatos ativos por nome/e-mail/telefone, limitada (para a IA). */
    List<ContactResponse> search(UUID companyId, String query, int limit);

    ContactResponse update(UUID companyId, UUID contactId, UpdateContactRequest request);

    void delete(UUID companyId, UUID contactId);
}