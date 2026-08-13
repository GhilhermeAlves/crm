package com.becommerce.crm.domain.lead;

/**
 * Estados de um lead (V016). LINKS: fluxo de funil da empresa — o lead nasce
 * {@code NEW}, pode ser {@code CONTACTED}, qualificado ({@code QUALIFIED}/
 * {@code UNQUALIFIED}), convertido em oportunidade no pipeline
 * ({@code CONVERTED}) ou perder-se ({@code LOST}).
 */
public enum LeadStatus {
    NEW,
    CONTACTED,
    QUALIFIED,
    UNQUALIFIED,
    CONVERTED,
    LOST
}