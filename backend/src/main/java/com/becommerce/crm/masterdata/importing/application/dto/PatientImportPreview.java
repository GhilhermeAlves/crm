package com.becommerce.crm.masterdata.importing.application.dto;
import java.util.List;
import java.util.UUID;
public record PatientImportPreview(int total, int ok, int duplicates, int errors, int ignored,
                                   List<String> unsupportedFiles, List<ImportRowResult> rows, UUID importId,
                                   List<String> notices) {}
