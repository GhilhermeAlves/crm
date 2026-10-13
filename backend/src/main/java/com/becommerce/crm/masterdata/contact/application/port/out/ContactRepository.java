package com.becommerce.crm.masterdata.contact.application.port.out;

import com.becommerce.crm.masterdata.contact.domain.Contact;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para contatos por empresa (tabela {@code contacts}, V015).
 * O isolamento por tenant é garantido pelo RLS FORCE (V021).
 */
public interface ContactRepository {

    Contact save(Contact contact);

    Optional<Contact> findById(UUID id);

    /** Total de contatos ativos (não excluídos logicamente) da empresa. */
    long countActiveByCompanyId(UUID companyId);

    /** Contatos ativos (não excluídos logicamente) da empresa, ordenados por nome. */
    List<Contact> findByCompanyIdActive(UUID companyId);

    /** Contato ativo da empresa que corresponda ao telefone (matching de mensagens). */
    Optional<Contact> findByCompanyIdAndPhone(UUID companyId, String phone);

    /**
     * Verifica se já existe contato da empresa com o nº de prontuário informado.
     * {@code excludeContactId} (opcional) ignora o próprio contato na edição.
     */
    boolean existsByRecordNumber(UUID companyId, String recordNumber, UUID excludeContactId);
}