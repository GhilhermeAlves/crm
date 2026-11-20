package com.becommerce.crm.masterdata.importing.application;

import com.becommerce.crm.masterdata.importing.application.dto.ImportRowResult;
import com.becommerce.crm.masterdata.importing.application.dto.PatientImportPreview;
import com.becommerce.crm.masterdata.importing.application.dto.UndoResult;
import com.becommerce.crm.masterdata.importing.infrastructure.DataImportRepository;
import com.becommerce.crm.masterdata.importing.infrastructure.ProcedureImportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;

import static com.becommerce.crm.masterdata.importing.application.PatientImportService.summarize;
import static com.becommerce.crm.masterdata.importing.application.PatientImportService.withTenant;

/**
 * Importação de procedimentos (procedures.csv + procedure_types.csv). Liga cada
 * procedimento ao paciente pelo código de origem guardado na importação de
 * pacientes. Grava tudo numa transação; não gera lançamento financeiro.
 */
@Service
public class ProcedureImportService {

    static final String OK = "OK", DUP = "DUPLICADO", ERR = "ERRO", IGN = "IGNORADO", DONE = "IMPORTADO";
    private static final int MAX_NAME = 160;

    private final DataImportRepository imports;
    private final ProcedureImportRepository repo;
    private final TransactionTemplate tx;

    public ProcedureImportService(DataImportRepository imports, ProcedureImportRepository repo,
                                  PlatformTransactionManager txManager) {
        this.imports = imports;
        this.repo = repo;
        this.tx = new TransactionTemplate(txManager);
    }

    private record Type(String name, long basePriceCents) {}

    private record Analysis(List<ProcedureRowMapper.Mapped> procedures, List<ImportRowResult> rows,
                            Map<String, UUID> contacts, Map<String, Type> types,
                            Map<String, UUID> services, List<String> notices) {}

    public PatientImportPreview preview(UUID companyId, byte[] zip) {
        Analysis a = analyze(companyId, zip);
        return summarize(a.rows(), List.of(), null, a.notices());
    }

    public PatientImportPreview importProcedures(UUID companyId, byte[] zip, String fileName, UUID userId) {
        Analysis a = analyze(companyId, zip);
        return withTenant(companyId, () -> tx.execute(status -> {
            UUID importId = imports.createImport(companyId, userId, "PROCEDURES", fileName);
            Map<String, UUID> services = new HashMap<>(a.services());
            List<ImportRowResult> out = new ArrayList<>();
            int imported = 0, skipped = 0, failed = 0;
            for (int i = 0; i < a.rows().size(); i++) {
                ImportRowResult row = a.rows().get(i);
                if (!OK.equals(row.status())) {
                    if (ERR.equals(row.status())) failed++; else skipped++;
                    out.add(row);
                    continue;
                }
                ProcedureRowMapper.Mapped p = a.procedures().get(i);
                Type type = a.types().get(p.sourceTypeId());
                UUID serviceId = services.computeIfAbsent(ProcedureImportRepository.normalize(type.name()),
                        k -> repo.createService(companyId, importId, type.name(), type.basePriceCents()));
                repo.insertProcedure(companyId, importId, userId, a.contacts().get(p.sourcePatientId()),
                        serviceId, type.name(), p);
                imported++;
                out.add(new ImportRowResult(row.line(), row.name(), DONE, null, row.warnings()));
            }
            imports.finishImport(companyId, importId, out.size(), imported, skipped, failed);
            return summarize(out, List.of(), importId, a.notices());
        }));
    }

    public UndoResult undo(UUID companyId, UUID importId) {
        return withTenant(companyId, () -> {
            if (!imports.isOpen(companyId, importId)) {
                throw new ImportFileException("Importação não encontrada ou já desfeita.");
            }
            int removed = repo.deleteProcedures(companyId, importId);
            repo.deleteUnusedServices(companyId, importId);
            imports.markUndone(companyId, importId);
            return new UndoResult(removed, 0);
        });
    }

    private Analysis analyze(UUID companyId, byte[] zip) {
        Map<String, CsvTable> tables = CsvTable.readTables(zip, "procedures.csv", "procedure_types.csv");
        Map<String, Type> types = new HashMap<>();
        for (Map<String, String> t : tables.get("procedure_types.csv").rows()) {
            String name = Optional.ofNullable(t.get("name")).orElse("").trim().replaceAll("\\s+", " ");
            if (name.length() > MAX_NAME) name = name.substring(0, MAX_NAME);
            long base = 0;
            try {
                base = Math.max(0, Long.parseLong(Optional.ofNullable(t.get("base_value")).orElse("0").trim()));
            } catch (NumberFormatException ignored) {
                // sem valor base
            }
            types.put(Optional.ofNullable(t.get("id")).orElse("").trim(), new Type(name, base));
        }

        Map<String, UUID> contacts = withTenant(companyId, () -> imports.contactsBySourceId(companyId));
        Map<String, UUID> services = withTenant(companyId, () -> repo.servicesByName(companyId));
        Set<String> already = withTenant(companyId, () -> repo.importedSourceIds(companyId));

        List<ProcedureRowMapper.Mapped> procedures = new ArrayList<>();
        List<ImportRowResult> rows = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Set<String> toCreate = new TreeSet<>(), reused = new TreeSet<>(), broken = new TreeSet<>();
        int line = 1;
        for (Map<String, String> raw : tables.get("procedures.csv").rows()) {
            line++;
            ProcedureRowMapper.Mapped p = ProcedureRowMapper.map(line, raw);
            procedures.add(p);
            Type type = p.sourceTypeId() == null ? null : types.get(p.sourceTypeId());
            String label = (type == null || type.name().isBlank() ? "Procedimento" : type.name())
                    + (p.teeth().isEmpty() ? "" : " — dente " + p.teeth().get(0).number());
            String status = OK, msg = null;
            if (p.discarded()) {
                status = IGN; msg = "Excluído no sistema de origem";
            } else if (p.error() != null) {
                status = ERR; msg = p.error();
            } else if (type == null || type.name().isBlank()) {
                status = ERR; msg = "Tipo de procedimento não encontrado";
            } else if (p.sourcePatientId() == null || !contacts.containsKey(p.sourcePatientId())) {
                status = ERR; msg = "Paciente não importado (importe os pacientes antes)";
            } else if (p.sourceId() != null && (already.contains(p.sourceId()) || !seen.add(p.sourceId()))) {
                status = DUP; msg = "Procedimento já importado";
            }
            if (OK.equals(status)) {
                String key = ProcedureImportRepository.normalize(type.name());
                if (services.containsKey(key)) reused.add(type.name()); else toCreate.add(type.name());
                if (type.name().contains("�")) broken.add(type.name());
            }
            rows.add(new ImportRowResult(p.line(), label, status, msg, p.warnings()));
        }

        List<String> notices = new ArrayList<>();
        if (!toCreate.isEmpty()) {
            notices.add(toCreate.size() + " serviço(s) novo(s) no catálogo: " + String.join(", ", toCreate));
        }
        if (!reused.isEmpty()) {
            notices.add(reused.size() + " serviço(s) já existente(s) reaproveitado(s): " + String.join(", ", reused));
        }
        if (!broken.isEmpty()) {
            notices.add(broken.size() + " nome(s) com acentuação quebrada no arquivo de origem — renomeie no catálogo depois");
        }
        return new Analysis(procedures, rows, contacts, types, services, notices);
    }
}
