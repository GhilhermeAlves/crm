package com.becommerce.crm.masterdata.importing.application;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ProcedureRowMapperTest {

    private Map<String, String> row(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    @Test
    void mapsDoneProcedureWithToothAndFaces() {
        var p = ProcedureRowMapper.map(2, row("id", "9", "patient_id", "7", "procedure_type_id", "3",
                "status", "done", "done_at", "2025-08-01 10:00:00", "amount", "28000",
                "tooth", "16", "tooth_surfaces", "{occlusal,mesial,incisal}", "observations", " ok "));
        assertEquals("DONE", p.state());
        assertEquals(LocalDate.of(2025, 8, 1), p.performedAt());
        assertEquals(28000, p.priceCents());
        assertEquals("PERMANENT", p.dentition());
        assertEquals(List.of(new ProcedureRowMapper.Tooth(16, List.of("O", "M"))), p.teeth());
        assertEquals("ok", p.notes());
        assertEquals("9", p.sourceId());
        assertEquals("7", p.sourcePatientId());
        assertNull(p.error());
    }

    @Test
    void pendingHasNoPerformedDateAndArchBecomesNote() {
        var p = ProcedureRowMapper.map(3, row("status", "pending", "done_at", "2025-08-01", "tooth", "upper_arch"));
        assertEquals("PLANNED", p.state());
        assertNull(p.performedAt());
        assertTrue(p.teeth().isEmpty());
        assertEquals("Arcada superior", p.notes());
    }

    @Test
    void deciduousToothAndExisted() {
        var p = ProcedureRowMapper.map(4, row("status", "existed", "tooth", "55"));
        assertEquals("PRE_EXISTING", p.state());
        assertEquals("DECIDUOUS", p.dentition());
    }

    @Test
    void doneWithoutDateFallsBackToCreatedAt() {
        var p = ProcedureRowMapper.map(5, row("status", "done", "created_at", "2025-01-02 00:00:00"));
        assertEquals(LocalDate.of(2025, 1, 2), p.performedAt());
        assertEquals(1, p.warnings().size());
    }

    @Test
    void invalidToothOrStatusIsError() {
        assertNotNull(ProcedureRowMapper.map(6, row("status", "done", "done_at", "2025-01-01", "tooth", "19")).error());
        assertNotNull(ProcedureRowMapper.map(7, row("status", "weird")).error());
    }

    @Test
    void flagsDiscarded() {
        assertTrue(ProcedureRowMapper.map(8, row("status", "pending", "discarded_at", "2025-01-01")).discarded());
    }
}
