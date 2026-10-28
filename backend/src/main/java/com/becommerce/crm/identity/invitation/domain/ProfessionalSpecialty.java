package com.becommerce.crm.identity.invitation.domain;

import java.util.Locale;

/**
 * Especialidade do profissional convidado; gravada no convite e herdada pela
 * membership. Mesmos valores da especialidade da empresa ({@code CompanySegment}).
 */
public enum ProfessionalSpecialty {
    GENERIC,
    DENTAL,
    OCCUPATIONAL_SAFETY,
    TRICHOLOGY;

    /** Valor opcional: vazio vira {@code null}; valor desconhecido é rejeitado. */
    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Especialidade inválida para convite: " + value);
        }
    }
}
