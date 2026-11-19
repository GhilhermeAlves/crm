package com.becommerce.crm.masterdata.importing.application.dto;
import java.time.Instant;
import java.util.UUID;
public record DataImportSummary(UUID id, String sourceFile, int total, int imported, int skipped,
                                int failed, Instant createdAt, Instant undoneAt) {}
