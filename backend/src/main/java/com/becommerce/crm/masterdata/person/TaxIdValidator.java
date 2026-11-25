package com.becommerce.crm.masterdata.person;

import java.util.Locale;

/**
 * CPF e CNPJ (inclusive o CNPJ alfanumérico da IN RFB 2.229/2024: as 12 primeiras posições
 * aceitam A–Z, valor do caractere = ASCII − 48; os dígitos verificadores seguem numéricos).
 * Mesma regra das funções app.is_valid_cpf/app.is_valid_cnpj (V147).
 */
public final class TaxIdValidator {

    private TaxIdValidator() {}

    /** Remove máscara e espaços; letras em maiúsculas. Nulo/vazio → null. */
    public static String normalize(String value) {
        if (value == null) return null;
        String s = value.replaceAll("[^0-9A-Za-z]", "").toUpperCase(Locale.ROOT);
        return s.isEmpty() ? null : s;
    }

    public static boolean isValidCpf(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1) return false;
        int sum = 0;
        for (int i = 0; i < 9; i++) sum += (cpf.charAt(i) - '0') * (10 - i);
        int d1 = (sum * 10) % 11;
        if (d1 == 10) d1 = 0;
        if (d1 != cpf.charAt(9) - '0') return false;
        sum = 0;
        for (int i = 0; i < 10; i++) sum += (cpf.charAt(i) - '0') * (11 - i);
        int d2 = (sum * 10) % 11;
        if (d2 == 10) d2 = 0;
        return d2 == cpf.charAt(10) - '0';
    }

    public static boolean isValidCnpj(String cnpj) {
        if (cnpj == null || !cnpj.matches("[0-9A-Z]{12}\\d{2}") || cnpj.chars().distinct().count() == 1) return false;
        int[] w1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] w2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 12; i++) sum += (cnpj.charAt(i) - 48) * w1[i];
        int r = sum % 11;
        int d1 = r < 2 ? 0 : 11 - r;
        if (d1 != cnpj.charAt(12) - '0') return false;
        sum = 0;
        for (int i = 0; i < 13; i++) sum += (cnpj.charAt(i) - 48) * w2[i];
        r = sum % 11;
        int d2 = r < 2 ? 0 : 11 - r;
        return d2 == cnpj.charAt(13) - '0';
    }

    /** Valida conforme o tipo (F = CPF, J = CNPJ). */
    public static boolean isValid(String personType, String normalized) {
        return "J".equals(personType) ? isValidCnpj(normalized) : isValidCpf(normalized);
    }

    /** Máscara de exibição para quem não tem person:sensitive. */
    public static String mask(String normalized) {
        if (normalized == null) return null;
        if (normalized.length() == 11) {
            return "***." + normalized.substring(3, 6) + "." + normalized.substring(6, 9) + "-**";
        }
        if (normalized.length() == 14) {
            return "**." + normalized.substring(2, 5) + ".***/" + normalized.substring(8, 12) + "-**";
        }
        return "***";
    }

    /** Formata para exibição (000.000.000-00 / 00.000.000/0000-00). */
    public static String format(String normalized) {
        if (normalized == null) return null;
        if (normalized.length() == 11) {
            return normalized.substring(0, 3) + "." + normalized.substring(3, 6) + "." + normalized.substring(6, 9)
                    + "-" + normalized.substring(9);
        }
        if (normalized.length() == 14) {
            return normalized.substring(0, 2) + "." + normalized.substring(2, 5) + "." + normalized.substring(5, 8)
                    + "/" + normalized.substring(8, 12) + "-" + normalized.substring(12);
        }
        return normalized;
    }
}
