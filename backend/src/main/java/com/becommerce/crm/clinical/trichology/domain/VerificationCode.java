package com.becommerce.crm.clinical.trichology.domain;

import java.security.SecureRandom;

/** Código de verificação do relatório: 12 caracteres sem ambiguidade (sem 0/O, 1/I/L). */
public final class VerificationCode {

    static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    public static final int LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private VerificationCode() {
    }

    public static String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /** Normaliza o que a pessoa digitou (minúsculas, espaços, hífens). */
    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.toUpperCase().replaceAll("[\\s-]", "");
    }

    public static boolean isWellFormed(String code) {
        if (code == null || code.length() != LENGTH) {
            return false;
        }
        for (char c : code.toCharArray()) {
            if (ALPHABET.indexOf(c) < 0) {
                return false;
            }
        }
        return true;
    }
}
