package com.becommerce.crm.identity.invitation.domain;

import java.util.Locale;

/** Especialidade do profissional convidado; gravada no convite e herdada pela membership. */
public enum ProfessionalSpecialty {
    GENERIC,
    DENTAL,
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
