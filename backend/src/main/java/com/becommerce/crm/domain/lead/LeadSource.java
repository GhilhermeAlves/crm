package com.becommerce.crm.domain.lead;

/**
 * Origem de captação do lead (V016). A origem é obrigatória (L-002) e
 * determina o score base de origem na fórmula de scoring (L-040).
 */
public enum LeadSource {
    WHATSAPP,
    FORM,
    API,
    IMPORT,
    MANUAL
}