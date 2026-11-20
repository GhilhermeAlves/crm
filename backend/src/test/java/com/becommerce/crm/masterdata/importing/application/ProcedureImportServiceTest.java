package com.becommerce.crm.masterdata.importing.application;

import com.becommerce.crm.masterdata.importing.application.dto.PatientImportPreview;
import com.becommerce.crm.masterdata.importing.infrastructure.DataImportRepository;
import com.becommerce.crm.masterdata.importing.infrastructure.ProcedureImportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProcedureImportServiceTest {

    UUID company = UUID.randomUUID(), user = UUID.randomUUID();
    UUID ana = UUID.randomUUID(), existingService = UUID.randomUUID();
    DataImportRepository imports = mock(DataImportRepository.class);
    ProcedureImportRepository repo = mock(ProcedureImportRepository.class);
    ProcedureImportService service;

    static final String TYPES = """
            id,name,base_value
            1,Limpeza ,25000
            2,Faceta em resina,48000
            3,Faceta  em resina,48000
            """;
    static final String PROCS = """
            id,patient_id,procedure_type_id,status,tooth,tooth_surfaces,amount,done_at,discarded_at
            10,P1,1,done,,{},25000,2025-08-01,
            11,P1,2,pending,11,{vestibular},48000,,
            12,P1,3,pending,12,{},48000,,
            13,P9,1,pending,,{},25000,,
            14,P1,1,pending,,{},25000,,2025-01-01
            15,P1,1,pending,,{},25000,,
            16,P1,99,pending,,{},0,,
            """;

    @BeforeEach
    void setUp() {
        service = new ProcedureImportService(imports, repo, mock(PlatformTransactionManager.class));
        when(imports.contactsBySourceId(company)).thenReturn(Map.of("P1", ana));
        when(repo.servicesByName(company)).thenReturn(Map.of("limpeza", existingService));
        when(repo.importedSourceIds(company)).thenReturn(Set.of("15"));
    }

    static byte[] zip() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            z.putNextEntry(new ZipEntry("b/procedures.csv"));
            z.write(PROCS.getBytes(StandardCharsets.UTF_8));
            z.putNextEntry(new ZipEntry("b/procedure_types.csv"));
            z.write(TYPES.getBytes(StandardCharsets.UTF_8));
        }
        return out.toByteArray();
    }

    @Test
    void previewClassifiesWithoutWriting() throws Exception {
        PatientImportPreview p = service.preview(company, zip());
        assertEquals(7, p.total());
        assertEquals(3, p.ok());          // 10, 11, 12
        assertEquals(1, p.duplicates());  // 15 já importado
        assertEquals(2, p.errors());      // 13 paciente não importado, 16 tipo inexistente
        assertEquals(1, p.ignored());     // 14 descartado
        assertTrue(p.notices().get(0).startsWith("1 serviço(s) novo(s)"));
        verify(repo, never()).insertProcedure(any(), any(), any(), any(), any(), any(), any());
        verify(imports, never()).createImport(any(), any(), any(), any());
    }

    @Test
    void importReusesAndCreatesServicesOncePerName() throws Exception {
        UUID importId = UUID.randomUUID(), facet = UUID.randomUUID();
        when(imports.createImport(company, user, "PROCEDURES", "base.zip")).thenReturn(importId);
        when(repo.createService(company, importId, "Faceta em resina", 48000)).thenReturn(facet);

        PatientImportPreview r = service.importProcedures(company, zip(), "base.zip", user);

        verify(repo, times(1)).createService(any(), any(), any(), anyLong());
        verify(repo).insertProcedure(eq(company), eq(importId), eq(user), eq(ana), eq(existingService),
                eq("Limpeza"), any());
        verify(repo, times(2)).insertProcedure(eq(company), eq(importId), eq(user), eq(ana), eq(facet),
                eq("Faceta em resina"), any());
        verify(imports).finishImport(company, importId, 7, 3, 2, 2);
        assertEquals(importId, r.importId());
    }

    @Test
    void undoDeletesProceduresAndUnusedServices() {
        UUID importId = UUID.randomUUID();
        when(imports.isOpen(company, importId)).thenReturn(true);
        when(repo.deleteProcedures(company, importId)).thenReturn(3);
        assertEquals(3, service.undo(company, importId).removed());
        verify(repo).deleteUnusedServices(company, importId);
        verify(imports).markUndone(company, importId);
    }
}
