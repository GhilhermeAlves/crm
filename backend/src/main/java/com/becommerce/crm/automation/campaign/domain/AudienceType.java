package com.becommerce.crm.automation.campaign.domain;

/**
 * Tipo de público da campanha (Sprint 17 — segmentação simples).
 * Critérios avançados (comportamento, pipeline, scoring, IA) entram como
 * novos valores/strategies sem mudar o schema (FUTURE).
 */
public enum AudienceType {
    CONTACTS,
    LEADS
}
