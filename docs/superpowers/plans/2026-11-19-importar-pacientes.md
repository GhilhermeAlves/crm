# Importação de pacientes — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Importar pela interface do CRM os pacientes de um zip de CSVs exportado de outro sistema, com prévia, rastreio e desfazer.

**Architecture:** Módulo novo `com.becommerce.crm.masterdata.importing`. Um leitor de zip/CSV puro, um mapeador puro (linha → `CreateContactRequest`), um service que analisa/importa usando `ContactService` (sem disparar automações) e registra cada paciente criado numa tabela de vínculo; o desfazer apaga (soft delete via `ContactService.delete`) só quem não tem agenda nem procedimentos. Frontend: página em Configurações → "Importar dados".

**Tech Stack:** Spring Boot / Java 21, JDBC (`NamedParameterJdbcTemplate`), Flyway, Postgres RLS; Next.js + React Query + shadcn, Vitest.

**Spec:** `docs/superpowers/specs/2026-11-19-importar-pacientes-design.md`

## Global Constraints

- Backend: **não rodar `mvn test`** — passar o comando ao usuário (`cd backend && mvn test -Dtest='PatientImport*,CsvTable*,ContactServiceTest'`).
- Frontend: rodar testes alterados e `npx prettier --check --end-of-line auto <arquivos>` em todo arquivo alterado.
- Migration nova: `V142__data_import.sql`; RLS `tenant_isolation_policy` + `GRANT ... TO crm_app`, igual à V141.
- Toda guarda deixa o CRM_ADMIN passar (permissões `company:update` + `contact:create`).
- Dados reais da clínica **nunca** entram no repositório; testes usam CSV fictício.
- Zip até 5 MB (limite global de multipart já existente; nginx sem client_max_body_size usa 1 MB — o zip real tem bem menos); `patients.csv` lido com limite de 10 MB descompactado.
- Commits locais na branch `feature/importar-pacientes`; sem push/PR sem pedido.
- Textos de UI em pt-BR.

## File Structure

Backend (`backend/src/main/java/com/becommerce/crm/masterdata/importing/`):
- `application/CsvTable.java` — parser CSV RFC 4180 + leitura do zip.
- `application/PatientRowMapper.java` — linha → `MappedPatient`.
- `application/dto/*.java` — records de resposta.
- `application/PatientImportService.java` — preview / import / list / undo.
- `infrastructure/DataImportRepository.java` — JDBC.
- `web/PatientImportController.java`.
- Modify: `masterdata/contact/application/service/ContactService.java` (criação sem automação).
- `resources/db/migration/V142__data_import.sql`.

Frontend (`frontend-refine/src/`):
- `features/masterdata/importing/{types.ts,import.service.ts,useImports.ts,components/PatientImportPanel.tsx}`
- `app/(dashboard)/settings/import/page.tsx`
- Modify: `lib/constants.ts`, `components/layout/navigation.ts`
- Test: `__tests__/features/masterdata/importing/PatientImportPanel.test.tsx`

---

### Task 1: Migration

**Files:** Create `backend/src/main/resources/db/migration/V142__data_import.sql`

- [ ] **Step 1:** Confirmar que não existe `V142__*` nem tabela `data_import*`: `ls backend/src/main/resources/db/migration | grep -E "V142|data_import"` → vazio.
- [ ] **Step 2:** Criar:

```sql
-- Importação de base de outro sistema (spec 2026-11-19). RLS por empresa.

CREATE TABLE data_import (
    id          UUID PRIMARY KEY,
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id     UUID,
    kind        VARCHAR(30)  NOT NULL,
    source_file VARCHAR(255),
    total       INT NOT NULL DEFAULT 0,
    imported    INT NOT NULL DEFAULT 0,
    skipped     INT NOT NULL DEFAULT 0,
    failed      INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    undone_at   TIMESTAMP
);
CREATE INDEX idx_data_import_company ON data_import (company_id, created_at DESC);

CREATE TABLE data_import_contacts (
    import_id  UUID NOT NULL REFERENCES data_import(id) ON DELETE CASCADE,
    contact_id UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    PRIMARY KEY (import_id, contact_id)
);

ALTER TABLE data_import ENABLE ROW LEVEL SECURITY;
ALTER TABLE data_import FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON data_import
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON data_import TO crm_app;

ALTER TABLE data_import_contacts ENABLE ROW LEVEL SECURITY;
ALTER TABLE data_import_contacts FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON data_import_contacts
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON data_import_contacts TO crm_app;
```

- [ ] **Step 3:** Commit `feat(importing): migration V142 data_import`.

---

### Task 2: Leitor de zip/CSV

**Files:** Create `.../importing/application/CsvTable.java`; Test `backend/src/test/java/com/becommerce/crm/masterdata/importing/application/CsvTableTest.java`

**Interfaces — Produces:**
- `record CsvTable(List<String> header, List<Map<String,String>> rows)`
- `static CsvTable parse(String text)`
- `static ZipContents readZip(byte[] zip)` com `record ZipContents(CsvTable patients, List<String> otherFiles)`; lança `ImportFileException` (RuntimeException, mesma pasta) com mensagem pt-BR.

- [ ] **Step 1: Teste**

```java
package com.becommerce.crm.masterdata.importing.application;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class CsvTableTest {

    @Test
    void parsesQuotedFieldsCommasAndNewlines() {
        CsvTable t = CsvTable.parse("id,name,obs\n1,\"Silva, Ana\",\"linha1\nlinha2\"\n2,Beto,\"diz \"\"oi\"\"\"\n");
        assertEquals(2, t.rows().size());
        assertEquals("Silva, Ana", t.rows().get(0).get("name"));
        assertEquals("linha1\nlinha2", t.rows().get(0).get("obs"));
        assertEquals("diz \"oi\"", t.rows().get(1).get("obs"));
    }

    @Test
    void stripsBomAndCrLf() {
        CsvTable t = CsvTable.parse("﻿id,name\r\n1,Ana\r\n");
        assertEquals("Ana", t.rows().get(0).get("name"));
    }

    @Test
    void readsPatientsFromNestedFolderAndListsOthers() throws Exception {
        byte[] zip = zip("base/patients.csv", "id,name\n1,Ana\n", "base/budgets.csv", "id\n");
        CsvTable.ZipContents c = CsvTable.readZip(zip);
        assertEquals("Ana", c.patients().rows().get(0).get("name"));
        assertEquals(java.util.List.of("budgets.csv"), c.otherFiles());
    }

    @Test
    void failsWithoutPatientsCsv() throws Exception {
        byte[] zip = zip("base/budgets.csv", "id\n");
        ImportFileException e = assertThrows(ImportFileException.class, () -> CsvTable.readZip(zip));
        assertTrue(e.getMessage().contains("patients.csv"));
    }

    @Test
    void failsOnInvalidZip() {
        assertThrows(ImportFileException.class, () -> CsvTable.readZip("nada".getBytes()));
    }

    private static byte[] zip(String... nameAndContent) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            for (int i = 0; i < nameAndContent.length; i += 2) {
                z.putNextEntry(new ZipEntry(nameAndContent[i]));
                z.write(nameAndContent[i + 1].getBytes(StandardCharsets.UTF_8));
                z.closeEntry();
            }
        }
        return out.toByteArray();
    }
}
```

- [ ] **Step 2:** Falha por classes inexistentes (usuário roda `mvn test -Dtest=CsvTableTest`).
- [ ] **Step 3: Implementação**

```java
package com.becommerce.crm.masterdata.importing.application;

public class ImportFileException extends RuntimeException {
    public ImportFileException(String message) { super(message); }
}
```

```java
package com.becommerce.crm.masterdata.importing.application;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** CSV RFC 4180 (vírgula, aspas duplas, quebras de linha dentro de aspas) e leitura do zip exportado. */
public record CsvTable(List<String> header, List<Map<String, String>> rows) {

    public static final long MAX_ENTRY_BYTES = 10L * 1024 * 1024;

    public record ZipContents(CsvTable patients, List<String> otherFiles) {}

    public static ZipContents readZip(byte[] zip) {
        CsvTable patients = null;
        List<String> others = new ArrayList<>();
        boolean anyEntry = false;
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                anyEntry = true;
                if (e.isDirectory()) continue;
                String name = e.getName().replace('\\', '/');
                String base = name.substring(name.lastIndexOf('/') + 1);
                if (!base.toLowerCase(Locale.ROOT).endsWith(".csv")) continue;
                if (base.equalsIgnoreCase("patients.csv")) {
                    patients = parse(new String(readLimited(in), StandardCharsets.UTF_8));
                } else {
                    others.add(base);
                }
            }
        } catch (IOException ex) {
            throw new ImportFileException("Arquivo .zip inválido ou corrompido.");
        }
        if (!anyEntry) throw new ImportFileException("Arquivo .zip inválido ou vazio.");
        if (patients == null) throw new ImportFileException("O zip não contém o arquivo patients.csv.");
        Collections.sort(others);
        return new ZipContents(patients, others);
    }

    private static byte[] readLimited(ZipInputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(buf)) > 0) {
            total += n;
            if (total > MAX_ENTRY_BYTES) throw new ImportFileException("patients.csv excede 10 MB.");
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    public static CsvTable parse(String text) {
        if (text.startsWith("﻿")) text = text.substring(1);
        List<List<String>> records = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') { field.append('"'); i++; }
                    else quoted = false;
                } else field.append(c);
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                current.add(field.toString()); field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                current.add(field.toString()); field.setLength(0);
                records.add(current); current = new ArrayList<>();
            } else field.append(c);
        }
        if (field.length() > 0 || !current.isEmpty()) { current.add(field.toString()); records.add(current); }
        records.removeIf(r -> r.size() == 1 && r.get(0).isBlank());
        if (records.isEmpty()) return new CsvTable(List.of(), List.of());
        List<String> header = records.get(0).stream().map(String::trim).toList();
        List<Map<String, String>> rows = new ArrayList<>();
        for (List<String> r : records.subList(1, records.size())) {
            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 0; i < header.size(); i++) row.put(header.get(i), i < r.size() ? r.get(i) : "");
            rows.add(row);
        }
        return new CsvTable(header, rows);
    }
}
```

- [ ] **Step 4:** Usuário roda `mvn test -Dtest=CsvTableTest` → PASS.
- [ ] **Step 5:** Commit `feat(importing): leitor de zip/CSV`.

---

### Task 3: Mapeador de paciente

**Files:** Create `.../importing/application/PatientRowMapper.java`, `.../importing/application/MappedPatient.java`; Test `.../importing/application/PatientRowMapperTest.java`

**Interfaces — Produces:**
- `record MappedPatient(int line, String name, CreateContactRequest request, boolean discarded, List<String> warnings)` — `line` é a linha do CSV (cabeçalho = 1).
- `static MappedPatient PatientRowMapper.map(int line, Map<String,String> row)`

- [ ] **Step 1: Teste**

```java
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
                "name", "  Maria  da Silva Souza ", "email", "MARIA@EX.COM ", "phone", "(34) 99999-0000",
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
```

- [ ] **Step 2:** Falha (usuário: `mvn test -Dtest=PatientRowMapperTest`).
- [ ] **Step 3: Implementação**

```java
package com.becommerce.crm.masterdata.importing.application;

import com.becommerce.crm.masterdata.contact.application.dto.request.CreateContactRequest;
import java.util.List;

public record MappedPatient(int line, String name, CreateContactRequest request,
                            boolean discarded, List<String> warnings) {}
```

```java
package com.becommerce.crm.masterdata.importing.application;

import com.becommerce.crm.masterdata.contact.application.dto.request.CreateContactRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/** Converte uma linha de patients.csv (exportação de outro sistema) em CreateContactRequest. */
public final class PatientRowMapper {

    private static final Map<String, String> GENDER = Map.of(
            "male", "MASCULINO", "m", "MASCULINO", "masculino", "MASCULINO",
            "female", "FEMININO", "f", "FEMININO", "feminino", "FEMININO",
            "other", "OUTRO", "outro", "OUTRO");
    private static final Map<String, String> MARITAL = Map.ofEntries(
            Map.entry("single", "SOLTEIRO"), Map.entry("solteiro", "SOLTEIRO"),
            Map.entry("married", "CASADO"), Map.entry("casado", "CASADO"),
            Map.entry("divorced", "DIVORCIADO"), Map.entry("divorciado", "DIVORCIADO"),
            Map.entry("widowed", "VIUVO"), Map.entry("viuvo", "VIUVO"),
            Map.entry("stable_union", "UNIAO_ESTAVEL"), Map.entry("uniao_estavel", "UNIAO_ESTAVEL"));

    private PatientRowMapper() {}

    public static MappedPatient map(int line, Map<String, String> row) {
        List<String> warnings = new ArrayList<>();
        Fields f = new Fields(row, warnings);

        String fullName = f.raw("name") == null ? "" : f.raw("name").replaceAll("\\s+", " ");
        int sp = fullName.indexOf(' ');
        String first = sp < 0 ? fullName : fullName.substring(0, sp);
        String last = sp < 0 ? null : fullName.substring(sp + 1);

        String email = f.raw("email") == null ? null : f.raw("email").toLowerCase(Locale.ROOT);
        String gender = f.lookup("gender", GENDER);
        String marital = f.lookup("marital_status", MARITAL);
        String state = f.raw("state") == null ? null : f.raw("state").toUpperCase(Locale.ROOT);
        if (state != null && !state.matches("[A-Z]{2}")) {
            warnings.add("UF \"" + state + "\" ignorada");
            state = null;
        }
        LocalDate birth = null;
        if (f.raw("birthdate") != null) {
            try {
                birth = LocalDate.parse(f.raw("birthdate").substring(0, Math.min(10, f.raw("birthdate").length())));
            } catch (DateTimeParseException e) {
                warnings.add("Data de nascimento \"" + f.raw("birthdate") + "\" ignorada");
            }
        }
        String occupation = f.raw("occupation");
        String guardianName = f.cut("contact_name", 160);

        CreateContactRequest req = new CreateContactRequest(
                f.cut(first.isEmpty() ? null : first, "name", 100), f.cut(last, "name", 100),
                f.cut(email, "email", 255),
                f.cut(f.digits("local_phone"), "local_phone", 20),
                f.cut(f.digits("phone"), "phone", 20),
                occupation == null ? null : f.cut("Profissão: " + occupation, "occupation", 500),
                birth,
                f.cut(f.digits("cpf"), "cpf", 14),
                f.cut("identity_document", 20), f.cut("identity_document_emissor", 20),
                gender, marital, null,
                f.cut(f.digits("zipcode"), "zipcode", 10),
                f.cut("address1", 160), f.cut("address_number", 20), f.cut("address2", 80),
                f.cut("district", 80), f.cut("city", 80), state,
                f.cut("health_plan", 80), f.cut("health_plan_number", 40),
                f.cut("medical_record_id", 40), f.cut("source", 80),
                guardianName == null ? null : Boolean.TRUE, guardianName,
                f.cut(f.digits("contact_cpf"), "contact_cpf", 14),
                f.cut(f.digits("contact_phone"), "contact_phone", 20));

        return new MappedPatient(line, fullName, req, f.raw("discarded_at") != null, List.copyOf(warnings));
    }

    private record Fields(Map<String, String> row, List<String> warnings) {
        String raw(String key) {
            String v = row.get(key);
            if (v == null) return null;
            v = v.trim();
            return v.isEmpty() ? null : v;
        }
        String digits(String key) {
            String v = raw(key);
            if (v == null) return null;
            v = v.replaceAll("\\D", "");
            return v.isEmpty() ? null : v;
        }
        String cut(String key, int max) { return cut(raw(key), key, max); }
        String cut(String value, String key, int max) {
            if (value == null || value.length() <= max) return value;
            warnings.add("Campo " + key + " cortado em " + max + " caracteres");
            return value.substring(0, max);
        }
        String lookup(String key, Map<String, String> table) {
            String v = raw(key);
            if (v == null) return null;
            String mapped = table.get(v.toLowerCase(Locale.ROOT));
            if (mapped == null) warnings.add("Valor \"" + v + "\" de " + key + " ignorado");
            return mapped;
        }
    }
}
```

- [ ] **Step 4:** Usuário roda `mvn test -Dtest=PatientRowMapperTest` → PASS.
- [ ] **Step 5:** Commit `feat(importing): mapeamento de patients.csv`.

---

### Task 4: Criação de paciente sem automações

**Files:** Modify `backend/src/main/java/com/becommerce/crm/masterdata/contact/application/service/ContactService.java` (método `create` privado, ~linhas 56–103); Test: `backend/src/test/java/.../masterdata/contact/application/service/ContactServiceTest.java` (existente — adicionar caso; se não existir, criar seguindo o padrão de mocks do `TransactionImportServiceTest`).

**Interfaces — Produces:** `public ContactResponse createImportedPatient(UUID companyId, CreateContactRequest request, UUID createdBy)` — igual a `createPatient`, mas **não** publica `WorkflowTriggerEvent.contactCreated`.

- [ ] **Step 1: Teste** (no `ContactServiceTest`, reaproveitando o setup existente de mocks `eventPublisher`/`contactRepository`):

```java
@Test
void createImportedPatientDoesNotTriggerWorkflows() {
    // mesmo arranjo usado pelo teste de createPatient existente
    service.createImportedPatient(companyId, validRequest(), userId);
    verify(contactRepository).save(any());
    verify(eventPublisher, never()).publish(any());
}
```

- [ ] **Step 2:** Falha (método não existe).
- [ ] **Step 3:** Em `ContactService`: trocar a assinatura privada para `create(UUID companyId, CreateContactRequest request, UUID createdBy, boolean patient, boolean publishEvents)`; os dois chamadores atuais passam `true`; envolver o `eventPublisher.publish(...)` em `if (publishEvents) { ... }`; adicionar:

```java
@Transactional
public ContactResponse createImportedPatient(UUID companyId, CreateContactRequest request, UUID createdBy) {
    return create(companyId, request, createdBy, true, false);
}
```

- [ ] **Step 4:** Usuário roda `mvn test -Dtest=ContactServiceTest` → PASS.
- [ ] **Step 5:** Commit `feat(contacts): criação de paciente importado sem automações`.

---

### Task 5: Repositório + service de importação

**Files:**
- Create `.../importing/infrastructure/DataImportRepository.java`
- Create `.../importing/application/dto/{ImportRowResult.java,PatientImportPreview.java,DataImportSummary.java,UndoResult.java}`
- Create `.../importing/application/PatientImportService.java`
- Test `.../importing/application/PatientImportServiceTest.java`

**Interfaces — Consumes:** `CsvTable.readZip`, `PatientRowMapper.map`, `ContactService.createImportedPatient`, `ContactService.delete`.
**Produces:**
- `record ImportRowResult(int line, String name, String status /* OK|DUPLICADO|ERRO|IGNORADO|IMPORTADO */, String message, List<String> warnings)`
- `record PatientImportPreview(int total, int ok, int duplicates, int errors, int ignored, List<String> unsupportedFiles, List<ImportRowResult> rows, UUID importId)` (`importId` null na prévia)
- `record DataImportSummary(UUID id, String sourceFile, int total, int imported, int skipped, int failed, Instant createdAt, Instant undoneAt)`
- `record UndoResult(int removed, int kept)`
- Service: `preview(UUID companyId, byte[] zip)`, `importPatients(UUID companyId, byte[] zip, String fileName, UUID userId)`, `list(UUID companyId)`, `undo(UUID companyId, UUID importId)`.

- [ ] **Step 1: Teste** (Mockito; `Validator` real de `Validation.buildDefaultValidatorFactory()`):

```java
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
```

Observação: o `ContactResponse` é record — se `mock()` falhar por ser final, construir via o construtor canônico preenchendo `id` e `null` no resto. Conferir `ContactResponse.java` antes.

- [ ] **Step 2:** Falha (classes inexistentes).
- [ ] **Step 3: DTOs**

```java
package com.becommerce.crm.masterdata.importing.application.dto;
import java.util.List;
public record ImportRowResult(int line, String name, String status, String message, List<String> warnings) {}
```
```java
package com.becommerce.crm.masterdata.importing.application.dto;
import java.util.List;
import java.util.UUID;
public record PatientImportPreview(int total, int ok, int duplicates, int errors, int ignored,
                                   List<String> unsupportedFiles, List<ImportRowResult> rows, UUID importId) {}
```
```java
package com.becommerce.crm.masterdata.importing.application.dto;
import java.time.Instant;
import java.util.UUID;
public record DataImportSummary(UUID id, String sourceFile, int total, int imported, int skipped,
                                int failed, Instant createdAt, Instant undoneAt) {}
```
```java
package com.becommerce.crm.masterdata.importing.application.dto;
public record UndoResult(int removed, int kept) {}
```

- [ ] **Step 4: Repositório**

```java
package com.becommerce.crm.masterdata.importing.infrastructure;

import com.becommerce.crm.masterdata.importing.application.dto.DataImportSummary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.util.*;

/** Acesso JDBC às tabelas de importação. Chamadores definem TenantContext (RLS). */
@Repository
public class DataImportRepository {

    public record ExistingKeys(Set<String> cpfs, Set<String> recordNumbers, Set<String> emails) {}

    private final NamedParameterJdbcTemplate jdbc;

    public DataImportRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public ExistingKeys existingKeys(UUID companyId) {
        Set<String> cpfs = new HashSet<>(), records = new HashSet<>(), emails = new HashSet<>();
        jdbc.query("""
                SELECT cpf, record_number, lower(email) AS email FROM contacts
                WHERE company_id = :c AND deleted_at IS NULL""", Map.of("c", companyId), rs -> {
            add(cpfs, rs.getString("cpf") == null ? null : rs.getString("cpf").replaceAll("\\D", ""));
            add(records, rs.getString("record_number"));
            add(emails, rs.getString("email"));
        });
        return new ExistingKeys(cpfs, records, emails);
    }

    private static void add(Set<String> set, String v) {
        if (v != null && !v.isBlank()) set.add(v.trim());
    }

    public UUID createImport(UUID companyId, UUID userId, String kind, String sourceFile) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO data_import (id, company_id, user_id, kind, source_file)
                VALUES (:id, :c, :u, :k, :f)""",
                new HashMap<>(Map.of("id", id, "c", companyId, "k", kind)) {{
                    put("u", userId); put("f", sourceFile);
                }});
        return id;
    }

    public void linkContact(UUID companyId, UUID importId, UUID contactId) {
        jdbc.update("INSERT INTO data_import_contacts (import_id, contact_id, company_id) VALUES (:i, :ct, :c)",
                Map.of("i", importId, "ct", contactId, "c", companyId));
    }

    public void finishImport(UUID companyId, UUID importId, int total, int imported, int skipped, int failed) {
        jdbc.update("""
                UPDATE data_import SET total = :t, imported = :im, skipped = :s, failed = :f
                WHERE id = :id AND company_id = :c""",
                Map.of("t", total, "im", imported, "s", skipped, "f", failed, "id", importId, "c", companyId));
    }

    public List<DataImportSummary> list(UUID companyId) {
        return jdbc.query("""
                SELECT id, source_file, total, imported, skipped, failed, created_at, undone_at
                FROM data_import WHERE company_id = :c ORDER BY created_at DESC LIMIT 50""",
                Map.of("c", companyId), (rs, i) -> new DataImportSummary(
                        rs.getObject("id", UUID.class), rs.getString("source_file"),
                        rs.getInt("total"), rs.getInt("imported"), rs.getInt("skipped"), rs.getInt("failed"),
                        rs.getTimestamp("created_at").toInstant(),
                        Optional.ofNullable(rs.getTimestamp("undone_at")).map(Timestamp::toInstant).orElse(null)));
    }

    public boolean isOpen(UUID companyId, UUID importId) {
        Integer n = jdbc.queryForObject("""
                SELECT count(*) FROM data_import WHERE id = :id AND company_id = :c AND undone_at IS NULL""",
                Map.of("id", importId, "c", companyId), Integer.class);
        return n != null && n > 0;
    }

    public List<UUID> linkedContacts(UUID companyId, UUID importId) {
        return jdbc.queryForList("""
                SELECT l.contact_id FROM data_import_contacts l
                JOIN contacts c ON c.id = l.contact_id AND c.deleted_at IS NULL
                WHERE l.import_id = :i AND l.company_id = :c""",
                Map.of("i", importId, "c", companyId), UUID.class);
    }

    public boolean hasClinicalHistory(UUID companyId, UUID contactId) {
        Boolean b = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM appointments WHERE contact_id = :ct AND company_id = :c)
                    OR EXISTS (SELECT 1 FROM patient_procedures WHERE contact_id = :ct AND company_id = :c)""",
                Map.of("ct", contactId, "c", companyId), Boolean.class);
        return Boolean.TRUE.equals(b);
    }

    public void markUndone(UUID companyId, UUID importId) {
        jdbc.update("UPDATE data_import SET undone_at = now() WHERE id = :id AND company_id = :c",
                Map.of("id", importId, "c", companyId));
    }
}
```

Antes de escrever: confirmar que `appointments` e `patient_procedures` têm `company_id` (`grep -n "company_id" backend/src/main/resources/db/migration/V079__scheduling.sql V106__patient_procedures.sql`). Se `appointments` não tiver, remover o filtro `company_id` daquela subquery (a RLS já isola).

- [ ] **Step 5: Service**

```java
package com.becommerce.crm.masterdata.importing.application;

import com.becommerce.crm.masterdata.contact.application.dto.request.CreateContactRequest;
import com.becommerce.crm.masterdata.contact.application.service.ContactService;
import com.becommerce.crm.masterdata.importing.application.dto.*;
import com.becommerce.crm.masterdata.importing.infrastructure.DataImportRepository;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Importação de pacientes de outro sistema. Sem @Transactional de propósito:
 * cada paciente é criado na transação do ContactService, então uma falha não
 * desfaz os demais.
 */
@Service
public class PatientImportService {

    static final String OK = "OK", DUP = "DUPLICADO", ERR = "ERRO", IGN = "IGNORADO", DONE = "IMPORTADO";

    private final ContactService contacts;
    private final DataImportRepository repo;
    private final Validator validator;

    public PatientImportService(ContactService contacts, DataImportRepository repo, Validator validator) {
        this.contacts = contacts;
        this.repo = repo;
        this.validator = validator;
    }

    private record Analysis(List<MappedPatient> patients, List<ImportRowResult> rows, List<String> others) {}

    public PatientImportPreview preview(UUID companyId, byte[] zip) {
        Analysis a = analyze(companyId, zip);
        return summarize(a.rows(), a.others(), null);
    }

    public PatientImportPreview importPatients(UUID companyId, byte[] zip, String fileName, UUID userId) {
        Analysis a = analyze(companyId, zip);
        UUID importId = withTenant(companyId, () -> repo.createImport(companyId, userId, "PATIENTS", fileName));
        List<ImportRowResult> out = new ArrayList<>();
        int imported = 0, skipped = 0, failed = 0;
        for (int i = 0; i < a.rows().size(); i++) {
            ImportRowResult row = a.rows().get(i);
            if (!OK.equals(row.status())) {
                if (ERR.equals(row.status())) failed++; else skipped++;
                out.add(row);
                continue;
            }
            try {
                UUID contactId = contacts.createImportedPatient(companyId, a.patients().get(i).request(), userId).id();
                withTenant(companyId, () -> { repo.linkContact(companyId, importId, contactId); return null; });
                imported++;
                out.add(new ImportRowResult(row.line(), row.name(), DONE, null, row.warnings()));
            } catch (RuntimeException e) {
                failed++;
                out.add(new ImportRowResult(row.line(), row.name(), ERR, e.getMessage(), row.warnings()));
            }
        }
        final int fi = imported, fs = skipped, ff = failed;
        withTenant(companyId, () -> { repo.finishImport(companyId, importId, out.size(), fi, fs, ff); return null; });
        return summarize(out, a.others(), importId);
    }

    public List<DataImportSummary> list(UUID companyId) {
        return withTenant(companyId, () -> repo.list(companyId));
    }

    public UndoResult undo(UUID companyId, UUID importId) {
        List<UUID> linked = withTenant(companyId, () -> {
            if (!repo.isOpen(companyId, importId)) {
                throw new ImportFileException("Importação não encontrada ou já desfeita.");
            }
            return repo.linkedContacts(companyId, importId);
        });
        int removed = 0, kept = 0;
        for (UUID id : linked) {
            boolean busy = withTenant(companyId, () -> repo.hasClinicalHistory(companyId, id));
            if (busy) { kept++; continue; }
            contacts.delete(companyId, id);
            removed++;
        }
        withTenant(companyId, () -> { repo.markUndone(companyId, importId); return null; });
        return new UndoResult(removed, kept);
    }

    private Analysis analyze(UUID companyId, byte[] zip) {
        CsvTable.ZipContents contents = CsvTable.readZip(zip);
        DataImportRepository.ExistingKeys existing = withTenant(companyId, () -> repo.existingKeys(companyId));
        Set<String> seenCpf = new HashSet<>(), seenRecord = new HashSet<>(), seenEmail = new HashSet<>();
        List<MappedPatient> patients = new ArrayList<>();
        List<ImportRowResult> rows = new ArrayList<>();
        int line = 1;
        for (Map<String, String> raw : contents.patients().rows()) {
            line++;
            MappedPatient p = PatientRowMapper.map(line, raw);
            patients.add(p);
            CreateContactRequest r = p.request();
            String status = OK, msg = null;
            if (p.discarded()) {
                status = IGN; msg = "Excluído no sistema de origem";
            } else {
                Set<ConstraintViolation<CreateContactRequest>> v = validator.validate(r);
                if (!v.isEmpty()) {
                    status = ERR;
                    msg = v.stream().map(ConstraintViolation::getMessage).sorted().collect(Collectors.joining("; "));
                } else if (r.cpf() != null && (existing.cpfs().contains(r.cpf()) || !seenCpf.add(r.cpf()))) {
                    status = DUP; msg = "CPF já cadastrado";
                } else if (r.recordNumber() != null
                        && (existing.recordNumbers().contains(r.recordNumber()) || !seenRecord.add(r.recordNumber()))) {
                    status = DUP; msg = "Prontuário já cadastrado";
                } else if (r.email() != null && (existing.emails().contains(r.email()) || !seenEmail.add(r.email()))) {
                    status = DUP; msg = "E-mail já cadastrado";
                }
            }
            rows.add(new ImportRowResult(p.line(), p.name(), status, msg, p.warnings()));
        }
        return new Analysis(patients, rows, contents.otherFiles());
    }

    private static PatientImportPreview summarize(List<ImportRowResult> rows, List<String> others, UUID importId) {
        Map<String, Long> c = rows.stream().collect(Collectors.groupingBy(ImportRowResult::status, Collectors.counting()));
        int ok = (int) (c.getOrDefault(OK, 0L) + c.getOrDefault(DONE, 0L));
        return new PatientImportPreview(rows.size(), ok, c.getOrDefault(DUP, 0L).intValue(),
                c.getOrDefault(ERR, 0L).intValue(), c.getOrDefault(IGN, 0L).intValue(), others, rows, importId);
    }

    private static <T> T withTenant(UUID companyId, java.util.function.Supplier<T> action) {
        boolean had = TenantContext.hasCompanyId();
        UUID previous = had ? TenantContext.getCompanyId() : null;
        TenantContext.setCompanyId(companyId);
        try {
            return action.get();
        } finally {
            if (had) TenantContext.setCompanyId(previous); else TenantContext.clear();
        }
    }
}
```

Nota: linha vazia (sem nome) cai em ERRO pelo `@NotBlank` de `firstName` — o teste espera isso.

- [ ] **Step 6:** Usuário roda `mvn test -Dtest=PatientImportServiceTest` → PASS.
- [ ] **Step 7:** Commit `feat(importing): service de prévia, importação e desfazer`.

---

### Task 6: Controller + tratamento de erro

**Files:** Create `.../importing/web/PatientImportController.java`; Modify `shared/web/handler/GlobalExceptionHandler.java` (adicionar handler 400 para `ImportFileException`, copiando o formato do `handleContactValidationException` da linha ~955).

- [ ] **Step 1:** Controller:

```java
package com.becommerce.crm.masterdata.importing.web;

import com.becommerce.crm.masterdata.importing.application.ImportFileException;
import com.becommerce.crm.masterdata.importing.application.PatientImportService;
import com.becommerce.crm.masterdata.importing.application.dto.*;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/imports")
public class PatientImportController {

    private static final String OWN_COMPANY = "Você só pode importar dados na sua própria empresa.";
    private static final long MAX_ZIP_BYTES = 5L * 1024 * 1024;
    private static final String CAN_IMPORT = "hasRole('CRM_ADMIN') or hasRole('SUPER_ADMIN') or (hasAuthority('company:update') and hasAuthority('contact:create'))";

    private final PatientImportService service;

    public PatientImportController(PatientImportService service) { this.service = service; }

    @PostMapping("/patients/preview")
    @PreAuthorize(CAN_IMPORT)
    public PatientImportPreview preview(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                        @RequestParam("file") MultipartFile file) throws IOException {
        return service.preview(companyId, read(file));
    }

    @PostMapping("/patients")
    @PreAuthorize(CAN_IMPORT)
    public PatientImportPreview importPatients(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                               @RequestParam("file") MultipartFile file,
                                               @AuthenticationPrincipal CurrentUser principal) throws IOException {
        return service.importPatients(companyId, read(file), file.getOriginalFilename(), principal.userId());
    }

    @GetMapping
    @PreAuthorize(CAN_IMPORT)
    public List<DataImportSummary> list(@CurrentCompanyId(OWN_COMPANY) UUID companyId) {
        return service.list(companyId);
    }

    @DeleteMapping("/{importId}")
    @PreAuthorize(CAN_IMPORT)
    public UndoResult undo(@CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID importId) {
        return service.undo(companyId, importId);
    }

    private static byte[] read(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new ImportFileException("Selecione um arquivo .zip.");
        if (file.getSize() > MAX_ZIP_BYTES) throw new ImportFileException("O arquivo deve ter no máximo 5 MB.");
        return file.getBytes();
    }
}
```

- [ ] **Step 2:** Confirmar que CRM_ADMIN recebe `company:update`/`contact:create` (`grep -rn "CRM_ADMIN" backend/src/main/java/com/becommerce/crm/shared/security | head`); se for tratado por `hasRole`, usar `hasRole('CRM_ADMIN') or (...)` no `CAN_IMPORT`.
- [ ] **Step 3:** Conferir o limite global de upload (`grep -rn "max-file-size" backend/src/main/resources`); se < 20MB, subir para `20MB` em `spring.servlet.multipart.max-file-size`/`max-request-size`.
- [ ] **Step 4:** Usuário roda `mvn -q compile` e `mvn test -Dtest='CsvTable*,PatientRowMapper*,PatientImport*,ContactServiceTest'` → PASS.
- [ ] **Step 5:** Commit `feat(importing): endpoints de importação de pacientes`.

---

### Task 7: Frontend

**Files:**
- Create `frontend-refine/src/features/masterdata/importing/types.ts`
- Create `frontend-refine/src/features/masterdata/importing/import.service.ts`
- Create `frontend-refine/src/features/masterdata/importing/useImports.ts`
- Create `frontend-refine/src/features/masterdata/importing/components/PatientImportPanel.tsx`
- Create `frontend-refine/src/app/(dashboard)/settings/import/page.tsx`
- Modify `frontend-refine/src/lib/constants.ts` (adicionar `SETTINGS_IMPORT: "/settings/import",` após `SETTINGS_EMAIL`)
- Modify `frontend-refine/src/components/layout/navigation.ts` (item após "Plano e cobrança": `{ label: "Importar dados", href: ROUTES.SETTINGS_IMPORT, icon: Upload, permission: "company:update" }`, importando `Upload` de `lucide-react`)
- Test `frontend-refine/src/__tests__/features/masterdata/importing/PatientImportPanel.test.tsx`

- [ ] **Step 1: types + service + hooks**

```ts
// types.ts
export type ImportRowStatus = "OK" | "DUPLICADO" | "ERRO" | "IGNORADO" | "IMPORTADO";

export interface ImportRowResult {
  line: number;
  name: string;
  status: ImportRowStatus;
  message: string | null;
  warnings: string[];
}

export interface PatientImportPreview {
  total: number;
  ok: number;
  duplicates: number;
  errors: number;
  ignored: number;
  unsupportedFiles: string[];
  rows: ImportRowResult[];
  importId: string | null;
}

export interface DataImportSummary {
  id: string;
  sourceFile: string | null;
  total: number;
  imported: number;
  skipped: number;
  failed: number;
  createdAt: string;
  undoneAt: string | null;
}

export interface UndoResult {
  removed: number;
  kept: number;
}
```

```ts
// import.service.ts
import api from "@/lib/api";
import type { DataImportSummary, PatientImportPreview, UndoResult } from "./types";

const base = (companyId: string) => `/api/v1/companies/${companyId}/imports`;

function zipForm(file: File): FormData {
  const form = new FormData();
  form.append("file", file);
  return form;
}

// `Content-Type: null` deixa o browser montar o multipart com boundary.
const multipart = { headers: { "Content-Type": null } };

export const importService = {
  async preview(companyId: string, file: File): Promise<PatientImportPreview> {
    const r = await api.post<PatientImportPreview>(
      `${base(companyId)}/patients/preview`,
      zipForm(file),
      multipart,
    );
    return r.data;
  },
  async importPatients(companyId: string, file: File): Promise<PatientImportPreview> {
    const r = await api.post<PatientImportPreview>(
      `${base(companyId)}/patients`,
      zipForm(file),
      multipart,
    );
    return r.data;
  },
  async list(companyId: string): Promise<DataImportSummary[]> {
    const r = await api.get<DataImportSummary[]>(base(companyId));
    return r.data;
  },
  async undo(companyId: string, importId: string): Promise<UndoResult> {
    const r = await api.delete<UndoResult>(`${base(companyId)}/${importId}`);
    return r.data;
  },
};
```

Antes: conferir se `base()` do `finance.service.ts` já inclui `/api/v1` via `baseURL` do axios; replicar exatamente o mesmo prefixo usado lá.

```ts
// useImports.ts
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { importService } from "./import.service";

const PATIENT_KEYS = ["patients", "contacts", "patient", "contacts-search"];

export function useImports(companyId: string | undefined) {
  return useQuery({
    queryKey: ["data-imports", companyId],
    queryFn: () => importService.list(companyId!),
    enabled: !!companyId,
  });
}

export function usePreviewImport(companyId: string | undefined) {
  return useMutation({ mutationFn: (file: File) => importService.preview(companyId!, file) });
}

function useInvalidatePatients() {
  const qc = useQueryClient();
  return () => {
    qc.invalidateQueries({ queryKey: ["data-imports"] });
    PATIENT_KEYS.forEach((k) => qc.invalidateQueries({ queryKey: [k] }));
  };
}

export function useImportPatients(companyId: string | undefined) {
  const invalidate = useInvalidatePatients();
  return useMutation({
    mutationFn: (file: File) => importService.importPatients(companyId!, file),
    onSuccess: invalidate,
  });
}

export function useUndoImport(companyId: string | undefined) {
  const invalidate = useInvalidatePatients();
  return useMutation({
    mutationFn: (importId: string) => importService.undo(companyId!, importId),
    onSuccess: invalidate,
  });
}
```

- [ ] **Step 2: Teste do painel (falha primeiro)**

```tsx
import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { PatientImportPanel } from "@/features/masterdata/importing/components/PatientImportPanel";
import { importService } from "@/features/masterdata/importing/import.service";

vi.mock("@/features/masterdata/importing/import.service", () => ({
  importService: { preview: vi.fn(), importPatients: vi.fn(), list: vi.fn(), undo: vi.fn() },
}));

const preview = {
  total: 3, ok: 1, duplicates: 1, errors: 1, ignored: 0,
  unsupportedFiles: ["budgets.csv"], importId: null,
  rows: [
    { line: 2, name: "Ana", status: "OK", message: null, warnings: [] },
    { line: 3, name: "Beto", status: "DUPLICADO", message: "CPF já cadastrado", warnings: [] },
    { line: 4, name: "", status: "ERRO", message: "Primeiro nome é obrigatório.", warnings: [] },
  ],
};

function renderPanel() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <PatientImportPanel companyId="c1" />
    </QueryClientProvider>,
  );
}

describe("PatientImportPanel", () => {
  beforeEach(() => {
    vi.mocked(importService.list).mockResolvedValue([]);
    vi.mocked(importService.preview).mockResolvedValue(preview as never);
    vi.mocked(importService.importPatients).mockResolvedValue({
      ...preview, importId: "i1",
      rows: [{ ...preview.rows[0], status: "IMPORTADO" }, preview.rows[1], preview.rows[2]],
    } as never);
  });

  it("mostra a prévia e importa só os válidos", async () => {
    renderPanel();
    const file = new File(["x"], "base.zip", { type: "application/zip" });
    fireEvent.change(screen.getByLabelText(/arquivo \.zip/i), { target: { files: [file] } });

    expect(await screen.findByText("CPF já cadastrado")).toBeInTheDocument();
    expect(screen.getByText(/budgets\.csv/)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /importar 1 paciente/i }));
    await waitFor(() => expect(importService.importPatients).toHaveBeenCalledWith("c1", file));
    expect(await screen.findByText(/1 paciente importado/i)).toBeInTheDocument();
  });
});
```

Run: `cd frontend-refine && npx vitest run src/__tests__/features/masterdata/importing` → FAIL (componente inexistente).

- [ ] **Step 3: Painel**

```tsx
"use client";

import { useState } from "react";
import { Undo2, Upload } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { toast } from "sonner";
import type { ImportRowStatus, PatientImportPreview } from "../types";
import { useImportPatients, useImports, usePreviewImport, useUndoImport } from "../useImports";

const STATUS_LABEL: Record<ImportRowStatus, string> = {
  OK: "Pronto",
  IMPORTADO: "Importado",
  DUPLICADO: "Duplicado",
  ERRO: "Erro",
  IGNORADO: "Ignorado",
};

function errorMessage(err: unknown): string {
  const data = (err as { response?: { data?: { message?: string } } })?.response?.data;
  return data?.message ?? "Não foi possível processar o arquivo.";
}

export function PatientImportPanel({ companyId }: { companyId: string }) {
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<PatientImportPreview | null>(null);
  const previewMut = usePreviewImport(companyId);
  const importMut = useImportPatients(companyId);
  const undoMut = useUndoImport(companyId);
  const { data: history = [] } = useImports(companyId);

  const shown = result ?? previewMut.data ?? null;
  const done = result !== null;

  function onFile(f: File | null) {
    setFile(f);
    setResult(null);
    if (f) previewMut.mutate(f, { onError: (e) => toast.error(errorMessage(e)) });
  }

  function onImport() {
    if (!file) return;
    importMut.mutate(file, {
      onSuccess: (r) => setResult(r),
      onError: (e) => toast.error(errorMessage(e)),
    });
  }

  function onUndo(id: string) {
    undoMut.mutate(id, {
      onSuccess: (r) =>
        toast.success(
          `${r.removed} paciente(s) removido(s)` +
            (r.kept ? `; ${r.kept} mantido(s) por já terem agenda ou procedimentos` : ""),
        ),
      onError: (e) => toast.error(errorMessage(e)),
    });
  }

  const imported = shown?.rows.filter((r) => r.status === "IMPORTADO").length ?? 0;

  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <Label htmlFor="import-zip">Arquivo .zip exportado do outro sistema</Label>
        <Input
          id="import-zip"
          type="file"
          accept=".zip,application/zip"
          onChange={(e) => onFile(e.target.files?.[0] ?? null)}
        />
        <p className="text-sm text-muted-foreground">
          Nesta versão só os pacientes (patients.csv) são importados. Duplicados (CPF, prontuário
          ou e-mail) são pulados e nada é sobrescrito.
        </p>
      </div>

      {previewMut.isPending && <p className="text-sm">Analisando arquivo…</p>}

      {shown && (
        <div className="space-y-4">
          <div className="flex flex-wrap gap-2 text-sm">
            <Badge variant="secondary">Total: {shown.total}</Badge>
            <Badge>{done ? `${imported} paciente(s) importado(s)` : `Prontos: ${shown.ok}`}</Badge>
            <Badge variant="outline">Duplicados: {shown.duplicates}</Badge>
            <Badge variant="destructive">Erros: {shown.errors}</Badge>
            {shown.ignored > 0 && <Badge variant="outline">Ignorados: {shown.ignored}</Badge>}
          </div>
          {shown.unsupportedFiles.length > 0 && (
            <p className="text-sm text-muted-foreground">
              Ainda não suportado: {shown.unsupportedFiles.join(", ")}
            </p>
          )}
          {!done && (
            <Button onClick={onImport} disabled={shown.ok === 0 || importMut.isPending}>
              <Upload className="mr-2 h-4 w-4" />
              {importMut.isPending
                ? "Importando…"
                : `Importar ${shown.ok} paciente${shown.ok === 1 ? "" : "s"}`}
            </Button>
          )}
          <div className="max-h-[420px] overflow-auto rounded-md border">
            <table className="w-full text-sm">
              <thead className="sticky top-0 bg-muted">
                <tr className="text-left">
                  <th className="p-2">Linha</th>
                  <th className="p-2">Nome</th>
                  <th className="p-2">Situação</th>
                  <th className="p-2">Detalhe</th>
                </tr>
              </thead>
              <tbody>
                {shown.rows.map((r) => (
                  <tr key={r.line} className="border-t">
                    <td className="p-2">{r.line}</td>
                    <td className="p-2">{r.name || "—"}</td>
                    <td className="p-2">{STATUS_LABEL[r.status]}</td>
                    <td className="p-2">{[r.message, ...r.warnings].filter(Boolean).join(" · ")}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {history.length > 0 && (
        <div className="space-y-2">
          <h3 className="font-medium">Importações anteriores</h3>
          <ul className="divide-y rounded-md border text-sm">
            {history.map((h) => (
              <li key={h.id} className="flex items-center justify-between gap-2 p-2">
                <span>
                  {new Date(h.createdAt).toLocaleString("pt-BR")} — {h.sourceFile ?? "arquivo"}:{" "}
                  {h.imported} importado(s), {h.skipped} pulado(s), {h.failed} erro(s)
                  {h.undoneAt && " · desfeita"}
                </span>
                {!h.undoneAt && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => onUndo(h.id)}
                    disabled={undoMut.isPending}
                  >
                    <Undo2 className="mr-1 h-4 w-4" /> Desfazer
                  </Button>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
```

Antes: confirmar que `@/components/ui/badge` e `sonner` existem (`ls frontend-refine/src/components/ui/badge.tsx; grep -rl "from \"sonner\"" frontend-refine/src | head -1`); se o projeto usar outro toast, trocar o import.

- [ ] **Step 4: Página**

```tsx
"use client";

import { PatientImportPanel } from "@/features/masterdata/importing/components/PatientImportPanel";
import { useCompanyId } from "@/features/identity/auth/hooks/useCompanyId";

export default function ImportSettingsPage() {
  const companyId = useCompanyId();
  return (
    <div className="space-y-4 p-6">
      <div>
        <h1 className="text-2xl font-semibold">Importar dados</h1>
        <p className="text-sm text-muted-foreground">
          Traga os pacientes de outro sistema a partir do arquivo .zip de exportação.
        </p>
      </div>
      {companyId && <PatientImportPanel companyId={companyId} />}
    </div>
  );
}
```

Conferir a assinatura de `useCompanyId` (retorna string ou objeto) em `features/identity/auth/hooks/useCompanyId.ts` e ajustar.

- [ ] **Step 5:** Rota + menu (`constants.ts`, `navigation.ts`) conforme "Files".
- [ ] **Step 6:** `npx vitest run src/__tests__/features/masterdata/importing` → PASS; `npx tsc --noEmit`; `npx prettier --check --end-of-line auto` em todos os arquivos do Task 7 (corrigir com `--write` se preciso).
- [ ] **Step 7:** Commit `feat(importing): tela de importação de pacientes`.

---

### Task 8: Fechamento

- [ ] Usuário roda a suíte completa do backend: `cd backend && mvn test`.
- [ ] `cd frontend-refine && npx vitest run` + `npx tsc --noEmit`.
- [ ] Atualizar memória com o estado da branch.
- [ ] PR/deploy só quando o usuário pedir; depois do deploy, testar na empresa de teste em produção com o zip da Dra. Raquel e usar "Desfazer".
