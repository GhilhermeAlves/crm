package com.becommerce.crm.masterdata.company.application.port.output;

import com.becommerce.crm.masterdata.company.domain.Company;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository {
    Company save(Company company);
    Optional<Company> findById(UUID id);
    /**
     * Id da empresa com o CNPJ/CPF, entre TODAS as empresas (V133: ignora o RLS
     * de companies, para a checagem de duplicidade enxergar as demais).
     */
    Optional<UUID> findIdByCnpj(String cnpj);
    /** Checagem de duplicidade entre todas as empresas (V133, ignora o RLS). */
    boolean existsByCnpj(String cnpj);
    /** Checagem de duplicidade entre todas as empresas (V133, ignora o RLS). */
    boolean existsByEmail(String email);
    /** Empresas visíveis pelo RLS de companies (V133) na sessão atual. */
    List<Company> findAll();
    /**
     * Ids de todas as empresas, para jobs e seeders sem usuário (V133). Os dados
     * de cada empresa devem ser lidos já com o tenant dela.
     */
    List<UUID> findAllIds();
    void deleteById(UUID id);
}
