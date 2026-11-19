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
