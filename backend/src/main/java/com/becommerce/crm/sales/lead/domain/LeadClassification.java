package com.becommerce.crm.sales.lead.domain;

/**
 * Classificação qualitativa de um lead (V016), derivada do score (L-042):
 * HOT / WARM / COLD / DISQUALIFIED.
 */
public enum LeadClassification {
    HOT,
    WARM,
    COLD,
    DISQUALIFIED
}