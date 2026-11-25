package com.becommerce.crm.masterdata.person;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Criptografia AES-GCM dos dados bancários (agência, conta, dígito e chave PIX), na aplicação.
 * Chave: {@code app.people.data-key} (variável PEOPLE_DATA_KEY), 32 bytes em Base64. Sem chave,
 * os dados bancários ficam indisponíveis (nunca são gravados em texto aberto).
 * Formato gravado: "v1:" + Base64(iv(12) || ciphertext+tag).
 */
@Component
public class BankDataCrypto {

    private static final String PREFIX = "v1:";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final SecretKeySpec key;

    public BankDataCrypto(@Value("${app.people.data-key:}") String base64Key) {
        SecretKeySpec k = null;
        if (base64Key != null && !base64Key.isBlank()) {
            byte[] raw = Base64.getDecoder().decode(base64Key.trim());
            if (raw.length != 32) {
                throw new IllegalStateException("app.people.data-key precisa ter 32 bytes em Base64");
            }
            k = new SecretKeySpec(raw, "AES");
        }
        this.key = k;
    }

    public boolean available() {
        return key != null;
    }

    public void ensureAvailable() {
        requireKey();
    }

    private void requireKey() {
        if (key == null) {
            throw new PersonException(HttpStatus.SERVICE_UNAVAILABLE, "BANK_KEY_MISSING",
                    "Dados bancários indisponíveis: a chave de criptografia (PEOPLE_DATA_KEY) não está configurada no servidor.");
        }
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) return null;
        requireKey();
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] ct = c.doFinal(plain.trim().getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array());
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criptografar dado bancário", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null || stored.isBlank()) return null;
        if (!stored.startsWith(PREFIX)) return stored;
        requireKey();
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, all, 0, 12));
            return new String(c.doFinal(all, 12, all.length - 12), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao descriptografar dado bancário", e);
        }
    }

    /** Mostra só os 4 últimos caracteres. */
    public static String mask(String plain) {
        if (plain == null) return null;
        return plain.length() <= 4 ? "****" : "****" + plain.substring(plain.length() - 4);
    }
}
