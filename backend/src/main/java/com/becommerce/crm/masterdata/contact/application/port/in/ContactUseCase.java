package com.becommerce.crm.masterdata.contact.application.port.in;

import com.becommerce.crm.masterdata.contact.application.dto.response.ContactResponse;
import com.becommerce.crm.masterdata.contact.application.dto.request.CreateContactRequest;
import com.becommerce.crm.masterdata.contact.application.dto.request.UpdateContactRequest;

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