package com.becommerce.crm.masterdata.person;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class BankDataCryptoTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void criptografaEDescriptografa() {
        BankDataCrypto crypto = new BankDataCrypto(KEY);
        String stored = crypto.encrypt("12345-6");
        assertTrue(stored.startsWith("v1:"));
        assertNotEquals("12345-6", stored);
        assertEquals("12345-6", crypto.decrypt(stored));
        assertNotEquals(stored, crypto.encrypt("12345-6"), "IV aleatório: mesmo valor gera textos diferentes");
    }

    @Test
    void semChaveNaoGravaEmTextoAberto() {
        BankDataCrypto crypto = new BankDataCrypto("");
        assertFalse(crypto.available());
        PersonException e = assertThrows(PersonException.class, () -> crypto.encrypt("123"));
        assertEquals("BANK_KEY_MISSING", e.code());
        assertNull(crypto.encrypt(null), "valor vazio não exige chave");
    }

    @Test
    void chaveComTamanhoErradoFalhaNaSubida() {
        assertThrows(IllegalStateException.class,
                () -> new BankDataCrypto(Base64.getEncoder().encodeToString(new byte[16])));
    }

    @Test
    void mascaraMostraSoOFinal() {
        assertEquals("****3456", BankDataCrypto.mask("123456"));
        assertEquals("****", BankDataCrypto.mask("12"));
    }
}
