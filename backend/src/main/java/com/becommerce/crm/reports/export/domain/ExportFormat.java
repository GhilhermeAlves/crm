package com.becommerce.crm.reports.export.domain;

/** Formato do arquivo exportado: PDF para impressão, XLSX para o Excel. */
public enum ExportFormat {

    PDF("application/pdf", "pdf", "PDF"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx", "Excel");

    private final String contentType;
    private final String extension;
    private final String label;

    ExportFormat(String contentType, String extension, String label) {
        this.contentType = contentType;
        this.extension = extension;
        this.label = label;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }

    public String label() {
        return label;
    }
}
