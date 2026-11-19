package com.becommerce.crm.masterdata.importing.application;

import com.becommerce.crm.masterdata.contact.application.dto.response.ContactResponse;
import com.becommerce.crm.masterdata.contact.application.service.ContactService;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactConflictException;
import com.becommerce.crm.masterdata.importing.application.dto.PatientImportPreview;
import com.becommerce.crm.masterdata.importing.infrastructure.DataImportRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PatientImportServiceTest {

    UUID company = UUID.randomUUID(), user = UUID.randomUUID();
    ContactService contacts = mock(ContactService.class);
    DataImportRepository repo = mock(DataImportRepository.class);
    PatientImportService service;

    @BeforeEach
    void setUp() {
        service = new PatientImportService(contacts, repo,
                Validation.buildDefaultValidatorFactory().getValidator());
        when(repo.existingKeys(company)).thenReturn(
                new DataImportRepository.ExistingKeys(Set.of("11111111111"), Set.of("R1"), Set.of("ja@ex.com")));
    }

    static byte[] zip(String csv) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            z.putNextEntry(new ZipEntry("b/patients.csv"));
            z.write(csv.getBytes(StandardCharsets.UTF_8));
            z.putNextEntry(new ZipEntry("b/budgets.csv"));
            z.write("id\n".getBytes());
        }
        return out.toByteArray();
    }

    static final String CSV = """
            name,cpf,medical_record_id,email,discarded_at
            Ana Lima,222.222.222-22,,,
            Beto Dup,111.111.111-11,,,
            Caio Pront,,R1,,
            Dani Mail,,,JA@ex.com,
            ,,,,
            Eva Email,,,nao-e-email,
            Fabi Velha,,,,2024-01-01
            Gil Repete,222.222.222-22,,,
            """;

    @Test
    void previewClassifiesRowsWithoutWriting() throws Exception {
        PatientImportPreview p = service.preview(company, zip(CSV));
        assertEquals(8, p.total());
        assertEquals(1, p.ok());
        assertEquals(4, p.duplicates());   // Beto(cpf), Caio(prontuário), Dani(e-mail), Gil(repetido no arquivo)
        assertEquals(2, p.errors());       // sem nome, e-mail inválido
        assertEquals(1, p.ignored());      // descartado
        assertEquals(List.of("budgets.csv"), p.unsupportedFiles());
        assertEquals(3, p.rows().get(1).line() );
        verifyNoInteractions(contacts);
        verify(repo, never()).createImport(any(), any(), any(), any());
    }

    @Test
    void importCreatesOkRowsAndLinksThem() throws Exception {
        UUID importId = UUID.randomUUID(), contactId = UUID.randomUUID();
        when(repo.createImport(eq(company), eq(user), eq("PATIENTS"), eq("base.zip"))).thenReturn(importId);
        ContactResponse resp = mock(ContactResponse.class);
        when(resp.id()).thenReturn(contactId);
        when(contacts.createImportedPatient(eq(company), any(), eq(user))).thenReturn(resp);

        PatientImportPreview r = service.importPatients(company, zip(CSV), "base.zip", user);

        assertEquals(importId, r.importId());
        verify(contacts, times(1)).createImportedPatient(eq(company), any(), eq(user));
        verify(repo).linkContact(company, importId, contactId);
        verify(repo).finishImport(company, importId, 8, 1, 5, 2);
        assertEquals("IMPORTADO", r.rows().get(0).status());
    }

    @Test
    void importTurnsServiceRejectionIntoRowError() throws Exception {
        when(repo.createImport(any(), any(), any(), any())).thenReturn(UUID.randomUUID());
        when(contacts.createImportedPatient(any(), any(), any()))
                .thenThrow(new ContactConflictException("Já existe um contato com este e-mail."));
        PatientImportPreview r = service.importPatients(company, zip(CSV), "base.zip", user);
        assertEquals("ERRO", r.rows().get(0).status());
        assertEquals("Já existe um contato com este e-mail.", r.rows().get(0).message());
    }

    @Test
    void undoRemovesOnlyContactsWithoutHistory() {
        UUID importId = UUID.randomUUID(), free = UUID.randomUUID(), busy = UUID.randomUUID();
        when(repo.isOpen(company, importId)).thenReturn(true);
        when(repo.linkedContacts(company, importId)).thenReturn(List.of(free, busy));
        when(repo.hasClinicalHistory(company, free)).thenReturn(false);
        when(repo.hasClinicalHistory(company, busy)).thenReturn(true);

        var res = service.undo(company, importId);

        assertEquals(1, res.removed());
        assertEquals(1, res.kept());
        verify(contacts).delete(company, free);
        verify(contacts, never()).delete(company, busy);
        verify(repo).markUndone(company, importId);
    }

    @Test
    void undoRejectsUnknownOrAlreadyUndone() {
        UUID importId = UUID.randomUUID();
        when(repo.isOpen(company, importId)).thenReturn(false);
        assertThrows(ImportFileException.class, () -> service.undo(company, importId));
    }
}
