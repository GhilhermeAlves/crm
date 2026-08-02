package com.becommerce.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Mapeamento de leitura da tabela {@code companies} do banco CRM compartilhado.
 * Apenas o status é necessário para o gate de acesso ao CRM.
 */
@Entity
@Table(name = "companies")
public class CompanyJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String status;

    public CompanyJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
