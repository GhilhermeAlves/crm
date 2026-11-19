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
