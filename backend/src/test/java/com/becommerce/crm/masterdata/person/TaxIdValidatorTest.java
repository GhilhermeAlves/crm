package com.becommerce.crm.masterdata.person;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaxIdValidatorTest {

    @Test
    void cpfValidoEInvalido() {
        assertTrue(TaxIdValidator.isValidCpf("52998224725"));
        assertFalse(TaxIdValidator.isValidCpf("52998224724"));
        assertFalse(TaxIdValidator.isValidCpf("11111111111"));
        assertFalse(TaxIdValidator.isValidCpf("5299822472"));
    }

    @Test
    void cnpjNumericoEAlfanumerico() {
        assertTrue(TaxIdValidator.isValidCnpj("11222333000181"));
        assertFalse(TaxIdValidator.isValidCnpj("11222333000180"));
        // Exemplo da Receita (IN RFB 2.229/2024): 12.ABC.345/01DE-35
        assertTrue(TaxIdValidator.isValidCnpj("12ABC34501DE35"));
        assertFalse(TaxIdValidator.isValidCnpj("12ABC34501DE36"));
        assertFalse(TaxIdValidator.isValidCnpj("AAAAAAAAAAAAAA"));
    }

    @Test
    void normalizaEValidaPeloTipo() {
        assertEquals("12ABC34501DE35", TaxIdValidator.normalize("12.abc.345/01de-35"));
        assertNull(TaxIdValidator.normalize(" .-/ "));
        assertTrue(TaxIdValidator.isValid("F", "52998224725"));
        assertFalse(TaxIdValidator.isValid("J", "52998224725"));
    }

    @Test
    void mascaraEFormatacao() {
        assertEquals("***.982.247-**", TaxIdValidator.mask("52998224725"));
        assertEquals("529.982.247-25", TaxIdValidator.format("52998224725"));
        assertEquals("12.ABC.345/01DE-35", TaxIdValidator.format("12ABC34501DE35"));
        assertNull(TaxIdValidator.mask(null));
    }
}
