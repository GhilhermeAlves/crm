package com.becommerce.crm.reports.export.domain;

/**
 * Relatórios que podem ser impressos (PDF) ou exportados (XLSX) pelo submenu de
 * Relatórios. Cada tipo define o rótulo pt-BR, a permissão exigida e o prefixo
 * do nome do arquivo.
 */
public enum ReportExportType {

    MONTHLY_DRE("Contábil mensal (DRE)", "finance:read", "contabil-mensal"),
    CASH_FLOW("Fluxo de caixa", "finance:read", "fluxo-de-caixa"),
    TRANSACTIONS("Extrato de lançamentos", "finance:read", "extrato-lancamentos"),
    AGENDA("Agenda / atendimentos", "appointment:read", "agenda");

    private final String label;
    private final String requiredPermission;
    private final String fileSlug;

    ReportExportType(String label, String requiredPermission, String fileSlug) {
        this.label = label;
        this.requiredPermission = requiredPermission;
        this.fileSlug = fileSlug;
    }

    public String label() {
        return label;
    }

    public String requiredPermission() {
        return requiredPermission;
    }

    public String fileSlug() {
        return fileSlug;
    }
}
