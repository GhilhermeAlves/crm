package com.becommerce.crm.finance.accounting.application.service;

import com.becommerce.crm.finance.accounting.application.dto.AccountingReportDataset;
import com.becommerce.crm.finance.accounting.application.dto.MonthlyAccountingData;
import com.becommerce.crm.finance.accounting.application.dto.MonthlyAccountingData.MonthlyCategory;
import com.becommerce.crm.finance.accounting.application.port.output.AccountingReportGateway;
import com.becommerce.crm.finance.accounting.domain.AccountingReportType;
import com.becommerce.crm.finance.accounting.domain.ReportFormat;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Renderiza o relatório contábil (Etapa 3) em PDF (OpenPDF) ou CSV (UTF-8 com
 * BOM para o Excel). Valores em centavos são formatados pt-BR
 * ({@link FinanceLabels}). Saldo/Resultado negativo sai com sinal de menos.
 */
@Component
public class AccountingReportRenderer {

    public byte[] render(AccountingReportType type, ReportFormat format, YearMonth month, String companyName,
                         AccountingReportDataset dataset, int generatedCount) {
        return switch (format) {
            case CSV -> csv(type, dataset);
            case PDF -> pdf(type, month, companyName, dataset, generatedCount);
        };
    }

    // =========================================================================
    // CSV
    // =========================================================================

    private static byte[] csv(AccountingReportType type, AccountingReportDataset dataset) {
        String body = switch (type) {
            case MONTHLY_ACCOUNTING -> monthlyCsv(dataset.monthly());
            case STATEMENT -> statementCsv(dataset.statementRows());
            case TRANSACTIONS -> transactionsCsv(dataset.transactionRows());
        };
        return ("\uFEFF" + body).getBytes(StandardCharsets.UTF_8);
    }

    private static String monthlyCsv(MonthlyAccountingData monthly) {
        List<String> lines = new ArrayList<>();
        lines.add("Tipo;Categoria;Valor");
        for (MonthlyCategory category : monthly.incomeCategories()) {
            lines.add("Receita;" + csv(FinanceLabels.categoryLabel(category.category())) + ";"
                    + FinanceLabels.formatCents(category.totalCents()));
        }
        for (MonthlyCategory category : monthly.expenseCategories()) {
            lines.add("Despesa;" + csv(FinanceLabels.categoryLabel(category.category())) + ";"
                    + FinanceLabels.formatCents(category.totalCents()));
        }
        lines.add("Total de receitas;" + FinanceLabels.formatCents(monthly.incomeTotalCents()));
        lines.add("Total de despesas;" + FinanceLabels.formatCents(monthly.expenseTotalCents()));
        lines.add("Taxas;" + FinanceLabels.formatCents(monthly.feeTotalCents()));
        lines.add("Resultado;" + FinanceLabels.formatCents(monthly.resultCents()));
        return String.join("\r\n", lines);
    }

    private static String statementCsv(List<AccountingReportGateway.StatementRow> rows) {
        List<String> lines = new ArrayList<>();
        lines.add("Data;Descrição;Categoria;Entrada;Saída;Saldo");
        long balance = 0L;
        for (AccountingReportGateway.StatementRow row : rows) {
            balance += (row.type() == TransactionType.INCOME ? 1 : -1) * row.amountCents();
            boolean income = row.type() == TransactionType.INCOME;
            String entrada = income ? FinanceLabels.formatCents(row.amountCents()) : "";
            String saida = income ? "" : FinanceLabels.formatCents(row.amountCents());
            lines.add(FinanceLabels.dateLabel(row.paidAt()) + ";"
                    + csv(row.description()) + ";"
                    + csv(FinanceLabels.categoryLabel(row.category())) + ";"
                    + entrada + ";" + saida + ";"
                    + FinanceLabels.formatCents(balance));
        }
        return String.join("\r\n", lines);
    }

    private static String transactionsCsv(List<AccountingReportGateway.TransactionDetailRow> rows) {
        List<String> lines = new ArrayList<>();
        lines.add("Vencimento;Pagamento;Descrição;Categoria;Status;Valor;Taxa;Líquido;"
                + "Forma de pagamento;Parcela;Paciente;Profissional");
        for (AccountingReportGateway.TransactionDetailRow row : rows) {
            String pagamento = row.paidAt() == null ? "" : FinanceLabels.dateLabel(row.paidAt());
            String parcela = row.installmentTotal() == null
                    ? ""
                    : row.installmentNumber() + "/" + row.installmentTotal();
            lines.add(FinanceLabels.dateLabel(row.dueDate()) + ";"
                    + pagamento + ";"
                    + csv(row.description()) + ";"
                    + csv(FinanceLabels.categoryLabel(row.category())) + ";"
                    + FinanceLabels.statusLabel(row.status(), false) + ";"
                    + FinanceLabels.formatCents(row.amountCents()) + ";"
                    + FinanceLabels.formatCents(row.feeCents()) + ";"
                    + FinanceLabels.formatCents(row.amountCents() - row.feeCents()) + ";"
                    + csv(FinanceLabels.paymentLabel(row.paymentMethod())) + ";"
                    + parcela + ";"
                    + csv(label(row.contactName())) + ";"
                    + csv(label(row.professionalName())));
        }
        return String.join("\r\n", lines);
    }

    private static String label(String value) {
        return value == null ? "" : value;
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(";") || value.contains("\"") || value.contains("\r") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // =========================================================================
    // PDF (OpenPDF)
    // =========================================================================

    private static byte[] pdf(AccountingReportType type, YearMonth month, String companyName,
                              AccountingReportDataset dataset, int generatedCount) {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);

            document.add(new Paragraph(title(type), titleFont));
            document.add(new Paragraph(trim(companyName), subFont));
            document.add(new Paragraph("Mês referência: " + FinanceLabels.monthLabel(month), subFont));
            document.add(new Paragraph("Gerado em: " + FinanceLabels.dateLabel(LocalDate.now())
                    + " — Solicitação nº " + generatedCount, subFont));
            document.add(Chunk.NEWLINE);

            PdfPTable table = switch (type) {
                case MONTHLY_ACCOUNTING -> monthlyTable(dataset, cellFont, headFont);
                case STATEMENT -> statementTable(dataset, cellFont, headFont);
                case TRANSACTIONS -> transactionsTable(dataset, cellFont, headFont);
            };
            document.add(table);
        } catch (DocumentException ex) {
            throw new IllegalStateException("Falha ao gerar o PDF do relatório contábil.", ex);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private static String trim(String value) {
        return value == null ? "" : value;
    }

    private static String title(AccountingReportType type) {
        return switch (type) {
            case MONTHLY_ACCOUNTING -> "Relatório Contábil Mensal";
            case STATEMENT -> "Extrato do Mês";
            case TRANSACTIONS -> "Lançamentos do Mês";
        };
    }

    private static PdfPTable monthlyTable(AccountingReportDataset dataset, Font cellFont, Font headFont) {
        MonthlyAccountingData monthly = dataset.monthly();
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        addHeader(table, new String[]{"Tipo", "Categoria", "Valor"}, headFont);
        for (MonthlyCategory category : monthly.incomeCategories()) {
            addTextRow(table, new String[]{"Receita", FinanceLabels.categoryLabel(category.category()),
                    FinanceLabels.formatReais(category.totalCents())}, cellFont);
        }
        for (MonthlyCategory category : monthly.expenseCategories()) {
            addTextRow(table, new String[]{"Despesa", FinanceLabels.categoryLabel(category.category()),
                    FinanceLabels.formatReais(category.totalCents())}, cellFont);
        }
        addTextRow(table, new String[]{"Total de receitas", "",
                FinanceLabels.formatReais(monthly.incomeTotalCents())}, cellFont);
        addTextRow(table, new String[]{"Total de despesas", "",
                FinanceLabels.formatReais(monthly.expenseTotalCents())}, cellFont);
        addTextRow(table, new String[]{"Taxas", "",
                FinanceLabels.formatReais(monthly.feeTotalCents())}, cellFont);
        addTextRow(table, new String[]{"Resultado", "",
                FinanceLabels.formatReais(monthly.resultCents())}, cellFont);
        return table;
    }

    private static PdfPTable statementTable(AccountingReportDataset dataset, Font cellFont, Font headFont) {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        addHeader(table, new String[]{"Data", "Descrição", "Categoria", "Entrada", "Saída", "Saldo"}, headFont);
        long balance = 0L;
        for (AccountingReportGateway.StatementRow row : dataset.statementRows()) {
            balance += (row.type() == TransactionType.INCOME ? 1 : -1) * row.amountCents();
            boolean income = row.type() == TransactionType.INCOME;
            String entrada = income ? FinanceLabels.formatReais(row.amountCents()) : "";
            String saida = income ? "" : FinanceLabels.formatReais(row.amountCents());
            addTextRow(table, new String[]{FinanceLabels.dateLabel(row.paidAt()), row.description(),
                    FinanceLabels.categoryLabel(row.category()), entrada, saida,
                    FinanceLabels.formatReais(balance)}, cellFont);
        }
        return table;
    }

    private static PdfPTable transactionsTable(AccountingReportDataset dataset, Font cellFont, Font headFont) {
        PdfPTable table = new PdfPTable(12);
        table.setWidthPercentage(100);
        addHeader(table, new String[]{"Vencimento", "Pagamento", "Descrição", "Categoria", "Status", "Valor",
                "Taxa", "Líquido", "Forma de pagamento", "Parcela", "Paciente", "Profissional"}, headFont);
        for (AccountingReportGateway.TransactionDetailRow row : dataset.transactionRows()) {
            String pagamento = row.paidAt() == null ? "" : FinanceLabels.dateLabel(row.paidAt());
            String parcela = row.installmentTotal() == null
                    ? ""
                    : row.installmentNumber() + "/" + row.installmentTotal();
            addTextRow(table, new String[]{FinanceLabels.dateLabel(row.dueDate()), pagamento,
                    row.description(), FinanceLabels.categoryLabel(row.category()),
                    FinanceLabels.statusLabel(row.status(), false), FinanceLabels.formatReais(row.amountCents()),
                    FinanceLabels.formatReais(row.feeCents()),
                    FinanceLabels.formatReais(row.amountCents() - row.feeCents()),
                    FinanceLabels.paymentLabel(row.paymentMethod()), parcela, trim(row.contactName()),
                    trim(row.professionalName())}, cellFont);
        }
        return table;
    }

    private static void addHeader(PdfPTable table, String[] texts, Font font) {
        for (String text : texts) {
            PdfPCell cell = new PdfPCell(new Phrase(text, font));
            cell.setGrayFill(0.9f);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(cell);
        }
    }

    private static void addTextRow(PdfPTable table, String[] texts, Font font) {
        for (int i = 0; i < texts.length; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(texts[i], font));
            boolean numeric = i == texts.length - 1 && !texts[i].isEmpty() && texts[i].charAt(0) != 'R';
            cell.setHorizontalAlignment(numeric ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
            table.addCell(cell);
        }
    }
}