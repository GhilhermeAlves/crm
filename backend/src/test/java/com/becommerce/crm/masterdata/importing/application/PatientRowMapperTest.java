package com.becommerce.crm.masterdata.importing.application;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PatientRowMapperTest {

    private Map<String, String> row(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    @Test
    void mapsMainFields() {
        MappedPatient p = PatientRowMapper.map(2, row(
                "id", "123", "name", "  Maria  da Silva Souza ", "email", "MARIA@EX.COM ", "phone", "(34) 99999-0000",
                "birthdate", "1986-06-25", "cpf", "123.456.789-09", "gender", "female",
                "marital_status", "married", "state", "mg", "zipcode", "38400-000",
                "address1", "Rua A", "address_number", "10", "district", "Centro", "city", "Uberlândia",
                "occupation", "Professora", "medical_record_id", " 77 ",
                "contact_name", "João", "contact_phone", "(34) 98888-0000", "discarded_at", ""));
        var r = p.request();
        assertEquals("Maria", r.firstName());
        assertEquals("da Silva Souza", r.lastName());
        assertEquals("maria@ex.com", r.email());
        assertEquals("34999990000", r.mobile());
        assertEquals(LocalDate.of(1986, 6, 25), r.birthDate());
        assertEquals("12345678909", r.cpf());
        assertEquals("FEMININO", r.gender());
        assertEquals("CASADO", r.maritalStatus());
        assertEquals("MG", r.state());
        assertEquals("38400000", r.zipCode());
        assertEquals("Profissão: Professora", r.notes());
        assertEquals("77", r.recordNumber());
        assertEquals(Boolean.TRUE, r.hasGuardian());
        assertEquals("34988880000", r.guardianPhone());
        assertFalse(p.discarded());
        assertEquals("123", p.sourceId());
    }

    @Test
    void blankBecomesNullAndUnknownValuesWarn() {
        MappedPatient p = PatientRowMapper.map(3, row("name", "Ana", "gender", "x", "birthdate", "25/13/2020",
                "state", "Minas"));
        assertNull(p.request().email());
        assertNull(p.request().gender());
        assertNull(p.request().birthDate());
        assertNull(p.request().state());
        assertEquals(3, p.warnings().size());
        assertNull(p.request().hasGuardian());
    }

    @Test
    void truncatesLongValuesWithWarning() {
        MappedPatient p = PatientRowMapper.map(4, row("name", "Ana", "city", "x".repeat(100)));
        assertEquals(80, p.request().city().length());
        assertTrue(p.warnings().get(0).contains("city"));
    }

    @Test
    void flagsDiscarded() {
        assertTrue(PatientRowMapper.map(5, row("name", "Ana", "discarded_at", "2025-01-01")).discarded());
    }
}
