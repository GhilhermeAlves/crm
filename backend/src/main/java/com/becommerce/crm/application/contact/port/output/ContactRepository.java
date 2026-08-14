package com.becommerce.crm.application.contact.port.output;

import com.becommerce.crm.domain.contact.Contact;

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
}