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

    // ── Módulo Pacientes (V104): paciente = contato com is_patient = true ──

    /** Cria o contato já marcado como paciente (mesmas regras do {@link #create}). */
    ContactResponse createPatient(UUID companyId, CreateContactRequest request, UUID createdBy);

    /** Pacientes ativos filtrados por nome, CPF, celular ou prontuário. */
    List<ContactResponse> searchPatients(UUID companyId, String query);

    /** Paciente da empresa; 404 se o contato não existir ou não for paciente. */
    ContactResponse getPatient(UUID companyId, UUID contactId);

    ContactResponse updatePatient(UUID companyId, UUID contactId, UpdateContactRequest request);

    /** Marca o contato como paciente (idempotente). */
    ContactResponse makePatient(UUID companyId, UUID contactId);

    /**
     * Variante sem exceção para fluxos de outros módulos (agendamento): contato
     * inexistente/excluído retorna {@code false} sem marcar a transação
     * corrente para rollback.
     */
    boolean markAsPatientIfExists(UUID companyId, UUID contactId);
}