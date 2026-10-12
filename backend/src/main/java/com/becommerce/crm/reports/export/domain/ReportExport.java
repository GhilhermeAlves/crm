package com.becommerce.crm.reports.export.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Pedido de exportação de relatório. Nasce PENDING e termina READY (arquivo no
 * storage) ou FAILED (mensagem de erro); estados terminais não mudam mais.
 */
public class ReportExport {

    private static final int MAX_ERROR = 500;

    private final UUID id;
    private final UUID companyId;
    private final UUID requestedBy;
    private final ReportExportType type;
    private final ExportFormat format;
    private final LocalDate periodFrom;
    private final LocalDate periodTo;
    private final Instant createdAt;
    private ReportExportStatus status;
    private String fileName;
    private UUID storageObjectId;
    private String errorMessage;
    private Instant finishedAt;

    public ReportExport(UUID id, UUID companyId, UUID requestedBy, ReportExportType type, ExportFormat format,
                        LocalDate periodFrom, LocalDate periodTo, ReportExportStatus status, String fileName,
                        UUID storageObjectId, String errorMessage, Instant createdAt, Instant finishedAt) {
        this.id = id;
        this.companyId = companyId;
        this.requestedBy = requestedBy;
        this.type = type;
        this.format = format;
        this.periodFrom = periodFrom;
        this.periodTo = periodTo;
        this.status = status;
        this.fileName = fileName;
        this.storageObjectId = storageObjectId;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.finishedAt = finishedAt;
    }

    public static ReportExport pending(UUID companyId, UUID requestedBy, ReportExportType type,
                                       ExportFormat format, LocalDate periodFrom, LocalDate periodTo) {
        return new ReportExport(UUID.randomUUID(), companyId, requestedBy, type, format, periodFrom, periodTo,
                ReportExportStatus.PENDING, null, null, null, Instant.now(), null);
    }

    public void markReady(UUID storageObjectId, String fileName) {
        requirePending();
        this.status = ReportExportStatus.READY;
        this.storageObjectId = storageObjectId;
        this.fileName = fileName;
        this.finishedAt = Instant.now();
    }

    public void markFailed(String message) {
        requirePending();
        String text = message == null || message.isBlank() ? "Erro ao gerar o relatório." : message;
        this.status = ReportExportStatus.FAILED;
        this.errorMessage = text.length() > MAX_ERROR ? text.substring(0, MAX_ERROR) : text;
        this.finishedAt = Instant.now();
    }

    private void requirePending() {
        if (status != ReportExportStatus.PENDING) {
            throw new IllegalStateException("Exportação " + id + " já finalizada (" + status + ").");
        }
    }

    public UUID id() {
        return id;
    }

    public UUID companyId() {
        return companyId;
    }

    public UUID requestedBy() {
        return requestedBy;
    }

    public ReportExportType type() {
        return type;
    }

    public ExportFormat format() {
        return format;
    }

    public LocalDate periodFrom() {
        return periodFrom;
    }

    public LocalDate periodTo() {
        return periodTo;
    }

    public ReportExportStatus status() {
        return status;
    }

    public String fileName() {
        return fileName;
    }

    public UUID storageObjectId() {
        return storageObjectId;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant finishedAt() {
        return finishedAt;
    }
}
