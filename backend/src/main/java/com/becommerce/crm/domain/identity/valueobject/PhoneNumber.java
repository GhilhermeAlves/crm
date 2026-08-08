package com.becommerce.crm.domain.identity.valueobject;

/**
 * Número de telefone normalizado no formato E.164 (ex.: +5534999999999).
 * Valida e normaliza telefones brasileiros.
 */
public class PhoneNumber {

    private final String e164;

    public PhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Telefone não pode ser nulo ou vazio");
        }
        this.e164 = normalize(phone);
        validateE164(this.e164);
    }

    private String normalize(String phone) {
        // Remove tudo que não for dígito
        String digits = phone.replaceAll("\\D", "");
        
        // Se já começa com +, assume E.164
        if (phone.trim().startsWith("+")) {
            return "+" + digits;
        }
        
        // Brasil: DDI 55
        if (digits.length() == 10) {
            // Fixo sem DDD - assume DDD 11 (padrão)
            return "+5511" + digits;
        }
        if (digits.length() == 11) {
            // Celular com 9 dígitos + DDD
            return "+55" + digits;
        }
        if (digits.length() == 12 && digits.startsWith("55")) {
            // Já tem DDI Brasil
            return "+" + digits;
        }
        if (digits.length() == 13 && digits.startsWith("55")) {
            // DDI Brasil + DDD + 9 dígitos
            return "+" + digits;
        }
        
        // Fallback: assume já está em E.164 ou adiciona +
        if (!digits.startsWith("55")) {
            return "+55" + digits;
        }
        return "+" + digits;
    }

    private void validateE164(String e164) {
        if (!e164.matches("^\\+55\\d{10,11}$")) {
            throw new IllegalArgumentException("Telefone inválido: deve ser um número brasileiro válido no formato E.164 (+55DDDXXXXXXXXX)");
        }
    }

    public String getE164() {
        return e164;
    }

    public String getNationalFormat() {
        // Retorna formato legível: (DD) 9XXXX-XXXX
        String digits = e164.substring(3); // remove +55
        if (digits.length() == 11) {
            return String.format("(%s) %s-%s", digits.substring(0, 2), digits.substring(2, 7), digits.substring(7));
        } else if (digits.length() == 10) {
            return String.format("(%s) %s-%s", digits.substring(0, 2), digits.substring(2, 6), digits.substring(6));
        }
        return e164;
    }

    @Override
    public String toString() {
        return getNationalFormat();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PhoneNumber that = (PhoneNumber) obj;
        return e164.equals(that.e164);
    }

    @Override
    public int hashCode() {
        return e164.hashCode();
    }
}