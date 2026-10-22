package com.becommerce.crm.clinical.documenttemplate.domain;

/**
 * Tipo de documento clínico. Persistido como {@code String} na coluna
 * {@code type} e validado pelo CHECK da V105.
 */
public enum DocumentType {
    PRESCRIPTION,
    CERTIFICATE,
    CONSENT,
    OTHER
}
