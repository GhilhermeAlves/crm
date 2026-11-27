package com.becommerce.crm.reports.export.application.service.render;

import com.becommerce.crm.reports.export.application.service.render.ReportDocument.Column;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.ReportTable;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.Row;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.RowKind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Mascaramento LGPD das exportações. Aplicado ao {@link ReportDocument} antes
 * de qualquer renderer, então PDF, XLSX e CSV saem iguais. Só as colunas
 * classificadas ({@link Column#sensitive()}) são tocadas; nome de paciente ou
 * contato não é dado sensível aqui e nunca é mascarado.
 *
 * <p>Quem tem {@link #SENSITIVE_PERMISSION} (a mesma do cadastro de pessoas)
 * ou é CRM_ADMIN/SUPER_ADMIN recebe o arquivo sem máscara. A decisão é gravada
 * no pedido ({@code report_exports.unmasked}), pois o gerador roda sem usuário.
 */
public final class ReportFieldMasker {

    /** Permissão já existente (seed em RoleSeedService) para ver dados pessoais. */
    public static final String SENSITIVE_PERMISSION = "person:sensitive";

    /** Substituto dos campos omitidos. */
    public static final String OMITTED = "—";

    /** Classificação dos campos pessoais sensíveis. */
    public enum SensitiveField {
        /** CPF: "***.456.789-**". CNPJ (pessoa jurídica) não é mascarado. */
        CPF,
        /** E-mail: "j***@dominio.com". */
        EMAIL,
        /** Telefone/WhatsApp: "(11) *****-1234". */
        PHONE,
        ADDRESS,
        BIRTH_DATE,
        /** Dados bancários e chave PIX. */
        BANK_DATA,
        /** Dados clínicos (anamnese, diagnóstico, prontuário). */
        CLINICAL
    }

    private static final ReportFieldMasker MASKED = new ReportFieldMasker(true);
    private static final ReportFieldMasker UNMASKED = new ReportFieldMasker(false);

    private final boolean masking;

    private ReportFieldMasker(boolean masking) {
        this.masking = masking;
    }

    /** Masker conforme o flag gravado na exportação. */
    public static ReportFieldMasker forExport(boolean unmasked) {
        return unmasked ? UNMASKED : MASKED;
    }

    /** CRM_ADMIN/SUPER_ADMIN sempre passam (regra do projeto); os demais precisam da permissão. */
    public static boolean canSeeUnmasked(Collection<String> roles, Collection<String> permissions) {
        if (roles != null && (roles.contains("CRM_ADMIN") || roles.contains("SUPER_ADMIN"))) {
            return true;
        }
        return permissions != null && (permissions.contains(SENSITIVE_PERMISSION) || permissions.contains("*"));
    }

    public boolean masking() {
        return masking;
    }

    /** Valor de uma célula de campo sensível, mascarado se for o caso. */
    public Object mask(SensitiveField field, Object value) {
        if (!masking || field == null || value == null) {
            return value;
        }
        String text = value.toString();
        if (text.isBlank()) {
            return value;
        }
        return switch (field) {
            case CPF -> maskCpf(text);
            case EMAIL -> maskEmail(text);
            case PHONE -> maskPhone(text);
            default -> OMITTED;
        };
    }

    /** Cópia do documento com as colunas sensíveis mascaradas (ou o próprio, sem máscara). */
    public ReportDocument apply(ReportDocument document) {
        if (!masking || document.sections().stream()
                .noneMatch(t -> t.columns().stream().anyMatch(c -> c.sensitive() != null))) {
            return document;
        }
        List<ReportTable> sections = document.sections().stream().map(this::apply).toList();
        return new ReportDocument(document.title(), document.period(), document.companyName(),
                document.companyCnpj(), document.issuedAt(), document.landscape(), sections, document.notice());
    }

    private ReportTable apply(ReportTable table) {
        List<Column> columns = table.columns();
        if (columns.stream().allMatch(c -> c.sensitive() == null)) {
            return table;
        }
        List<Row> rows = new ArrayList<>(table.rows().size());
        for (Row row : table.rows()) {
            // Linha de grupo é só um rótulo; totais não carregam dado pessoal.
            if (row.kind() == RowKind.GROUP) {
                rows.add(row);
                continue;
            }
            List<Object> cells = new ArrayList<>(row.cells());
            for (int i = 0; i < cells.size() && i < columns.size(); i++) {
                Column column = columns.get(i);
                if (column.sensitive() != null) {
                    Object masked = mask(column.sensitive(), cells.get(i));
                    // Coluna de data omitida vira texto "—"; os renderers aceitam String em qualquer coluna.
                    cells.set(i, masked);
                }
            }
            rows.add(new Row(row.kind(), cells));
        }
        return new ReportTable(table.heading(), columns, rows);
    }

    /** "123.456.789-01" → "***.456.789-**"; CNPJ (14 dígitos) é dado de empresa e sai sem máscara. */
    static String maskCpf(String raw) {
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() == 11) {
            return "***." + digits.substring(3, 6) + "." + digits.substring(6, 9) + "-**";
        }
        if (digits.length() == 14) {
            return raw;
        }
        return OMITTED;
    }

    /** "joao@dominio.com" → "j***@dominio.com". */
    static String maskEmail(String raw) {
        String text = raw.trim();
        int at = text.indexOf('@');
        if (at <= 0 || at == text.length() - 1) {
            return OMITTED;
        }
        return text.charAt(0) + "***" + text.substring(at);
    }

    /** "(11) 98765-1234" → "(11) *****-1234"; sem DDD → "*****-1234". */
    static String maskPhone(String raw) {
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("55") && digits.length() >= 12) {
            digits = digits.substring(2);
        }
        if (digits.length() < 8) {
            return OMITTED;
        }
        String last = digits.substring(digits.length() - 4);
        if (digits.length() >= 10) {
            return "(" + digits.substring(0, 2) + ") *****-" + last;
        }
        return "*****-" + last;
    }
}
