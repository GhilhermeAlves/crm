package com.becommerce.crm.reports.export.domain;

import java.util.Collection;
import java.util.List;

/**
 * Catálogo único dos relatórios que podem ser impressos (PDF) ou exportados
 * (XLSX/CSV) pelo submenu de Relatórios. Cada tipo define rótulo, descrição,
 * domínio, permissão exigida, formatos aceitos, modo de período, janela máxima
 * em meses, segmento de URL do frontend e prefixo do nome do arquivo. O
 * frontend consome estes metadados pelo endpoint {@code /catalog}.
 */
public enum ReportExportType {

    MONTHLY_DRE("Contábil mensal (DRE)",
            "Receitas, deduções e despesas do mês agrupadas por conta, com o resultado do mês "
                    + "(regime de caixa). Ideal para enviar ao contador.",
            ReportDomain.FINANCE, "finance:read", Formats.ALL, ReportPeriodMode.MONTH, 1,
            "monthly-dre", "contabil-mensal"),
    CASH_FLOW("Fluxo de caixa",
            "Entradas e saídas pagas dia a dia, com saldo do dia e saldo acumulado do período.",
            ReportDomain.FINANCE, "finance:read", Formats.ALL, ReportPeriodMode.RANGE, Defaults.MAX_MONTHS,
            "cash-flow", "fluxo-de-caixa"),
    TRANSACTIONS("Extrato de lançamentos",
            "Todos os lançamentos com vencimento no período: paciente, categoria, forma de pagamento, "
                    + "valor e status.",
            ReportDomain.FINANCE, "finance:read", Formats.ALL, ReportPeriodMode.RANGE, Defaults.MAX_MONTHS,
            "transactions", "extrato-lancamentos"),
    AGENDA("Agenda / atendimentos",
            "Consultas do período com resumo por profissional (agendadas, realizadas, faltas e canceladas) "
                    + "e a lista detalhada.",
            ReportDomain.AGENDA, "appointment:read", Formats.ALL, ReportPeriodMode.RANGE, Defaults.MAX_MONTHS,
            "agenda", "agenda"),
    BUDGETS("Orçamentos",
            "Orçamentos criados no período com resumo por situação (em aberto, aprovados e perdidos) e a "
                    + "lista com paciente, itens, desconto, total e forma de pagamento.",
            ReportDomain.SALES, "contact:read", Formats.ALL, ReportPeriodMode.RANGE, Defaults.MAX_MONTHS,
            "budgets", "orcamentos"),
    LEADS("Leads",
            "Leads criados no período com resumo por status e a lista com nome, contato, origem, status, "
                    + "responsável e score.",
            ReportDomain.SALES, "lead:read", Formats.ALL, ReportPeriodMode.RANGE, Defaults.MAX_MONTHS,
            "leads", "leads"),
    CONTACTS("Contatos",
            "Contatos cadastrados no período com documento, e-mail, telefone, cidade/UF, nascimento, "
                    + "origem e tags.",
            ReportDomain.SALES, "contact:read", Formats.ALL, ReportPeriodMode.RANGE, Defaults.MAX_MONTHS,
            "contacts", "contatos"),
    ACTIVITIES("Atividades",
            "Atividades registradas no período com resumo por tipo e a lista com data, tipo, assunto, "
                    + "contato e quem registrou.",
            ReportDomain.SALES, "activity:read", Formats.ALL, ReportPeriodMode.RANGE, Defaults.MAX_MONTHS,
            "activities", "atividades");

    /** Janelas padrão; relatórios de alto volume podem usar {@link #HIGH_VOLUME_MAX_MONTHS}. */
    public static final class Defaults {
        public static final int MAX_MONTHS = 12;
        public static final int HIGH_VOLUME_MAX_MONTHS = 3;

        private Defaults() {
        }
    }

    private static final class Formats {
        // Todos os geradores atuais renderizam PDF, XLSX e CSV (o check do banco aceita CSV desde a V136).
        static final List<ExportFormat> ALL = List.of(ExportFormat.PDF, ExportFormat.XLSX, ExportFormat.CSV);

        private Formats() {
        }
    }

    private final String label;
    private final String description;
    private final ReportDomain domain;
    private final String requiredPermission;
    private final List<ExportFormat> formats;
    private final ReportPeriodMode periodMode;
    private final int maxMonths;
    private final String slug;
    private final String fileSlug;

    ReportExportType(String label, String description, ReportDomain domain, String requiredPermission,
                     List<ExportFormat> formats, ReportPeriodMode periodMode, int maxMonths, String slug,
                     String fileSlug) {
        this.label = label;
        this.description = description;
        this.domain = domain;
        this.requiredPermission = requiredPermission;
        this.formats = List.copyOf(formats);
        this.periodMode = periodMode;
        this.maxMonths = maxMonths;
        this.slug = slug;
        this.fileSlug = fileSlug;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public ReportDomain domain() {
        return domain;
    }

    public String requiredPermission() {
        return requiredPermission;
    }

    public List<ExportFormat> formats() {
        return formats;
    }

    public boolean supports(ExportFormat format) {
        return formats.contains(format);
    }

    public ReportPeriodMode periodMode() {
        return periodMode;
    }

    /** Janela máxima do período em meses (no modo MONTH é sempre 1 mês fechado). */
    public int maxMonths() {
        return maxMonths;
    }

    /** Segmento da URL no frontend: /reports/export/&lt;slug&gt;. */
    public String slug() {
        return slug;
    }

    public String fileSlug() {
        return fileSlug;
    }

    /** Pode usar o relatório: CRM_ADMIN/SUPER_ADMIN sempre; os demais com a permissão do tipo. */
    public boolean allowedFor(Collection<String> roles, Collection<String> permissions) {
        if (roles != null && (roles.contains("CRM_ADMIN") || roles.contains("SUPER_ADMIN"))) {
            return true;
        }
        return permissions != null && permissions.contains(requiredPermission);
    }
}
