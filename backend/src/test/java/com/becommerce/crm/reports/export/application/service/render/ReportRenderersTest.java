package com.becommerce.crm.reports.export.application.service.render;

import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.reports.export.application.dto.AgendaDataset;
import com.becommerce.crm.reports.export.application.dto.AgendaDataset.ProfessionalSummary;
import com.becommerce.crm.reports.export.application.dto.BudgetsDataset;
import com.becommerce.crm.reports.export.application.dto.BudgetsDataset.StatusSummary;
import com.becommerce.crm.reports.export.application.dto.MonthlyDreDataset;
import com.becommerce.crm.reports.export.application.dto.MonthlyDreDataset.DreGroup;
import com.becommerce.crm.reports.export.application.dto.MonthlyDreDataset.DreLine;
import com.becommerce.crm.reports.export.application.dto.TransactionsDataset;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.AppointmentRow;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.BudgetRow;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.CompanyHeader;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.TransactionRow;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportRenderersTest {

    private static final CompanyHeader COMPANY = new CompanyHeader("Clínica Sorriso", "12.345.678/0001-90");
    private static final LocalDateTime ISSUED = LocalDateTime.of(2026, 7, 1, 10, 30, 15);

    private ReportDocument dre() {
        MonthlyDreDataset data = new MonthlyDreDataset(List.of(
                new DreGroup("Receitas", true, List.of(new DreLine("Procedimento", 2, 150_000),
                        new DreLine("Consulta", 0, 0))),
                new DreGroup("Despesas administrativas", false, List.of(new DreLine("Aluguel", 1, 50_000)))),
                100_000);
        return ReportDocumentFactory.monthlyDre(data, YearMonth.of(2026, 6), COMPANY, ISSUED);
    }

    @Test
    void pdfHasHeaderResultAndPageCount() throws Exception {
        byte[] pdf = PdfReportLayout.render(dre());

        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
        PdfReader reader = new PdfReader(pdf);
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        assertThat(text).contains("Resultado do mês").contains("Junho de 2026").contains("Clínica Sorriso")
                .contains("Página 1/1");
    }

    @Test
    void pdfLandscapeWithManyRowsBreaksPages() throws Exception {
        List<TransactionRow> rows = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            rows.add(new TransactionRow(LocalDate.of(2026, 6, 1), null, "Lançamento " + i, "Ana",
                    TransactionCategory.RENT, PaymentMethod.PIX, TransactionType.EXPENSE, 1_000,
                    TransactionStatus.PENDING));
        }
        ReportDocument doc = ReportDocumentFactory.transactions(new TransactionsDataset(rows, 0, 120_000),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), COMPANY, ISSUED);

        PdfReader reader = new PdfReader(PdfReportLayout.render(doc));
        int pages = reader.getNumberOfPages();
        assertThat(pages).isGreaterThan(1);
        assertThat(new PdfTextExtractor(reader).getTextFromPage(pages)).contains("Página " + pages + "/");
    }

    @Test
    void xlsxWritesMoneyAsNumber() throws Exception {
        byte[] xlsx = XlsxReportWriter.write(dre());

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).startsWith("Contábil mensal (DRE)");
            Row firstItem = sheet.getRow(6);
            assertThat(firstItem.getCell(0).getStringCellValue()).isEqualTo("Procedimento");
            assertThat(firstItem.getCell(2).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(firstItem.getCell(2).getNumericCellValue()).isEqualTo(1500.0);
            Row last = sheet.getRow(sheet.getLastRowNum());
            assertThat(last.getCell(0).getStringCellValue()).isEqualTo("Resultado do mês");
            assertThat(last.getCell(2).getNumericCellValue()).isEqualTo(1000.0);
        }
    }

    @Test
    void agendaXlsxHasOneSheetPerSection() throws Exception {
        AgendaDataset data = new AgendaDataset(
                List.of(new ProfessionalSummary("Dra. Bia", 1, 0, 2, 0, 1)),
                List.of(new AppointmentRow(Instant.parse("2026-06-10T13:00:00Z"), "Ana", "Consulta", "Dra. Bia",
                        "COMPLETED", "WHATSAPP")));
        ReportDocument doc = ReportDocumentFactory.agenda(data, LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30), COMPANY, ISSUED);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(XlsxReportWriter.write(doc)))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            Row appointment = workbook.getSheetAt(1).getRow(5);
            assertThat(appointment.getCell(0).getStringCellValue()).isEqualTo("10/06/2026 10:00");
            assertThat(appointment.getCell(4).getStringCellValue()).isEqualTo("Realizada");
            assertThat(appointment.getCell(5).getStringCellValue()).isEqualTo("WhatsApp");
        }
        assertThat(PdfReportLayout.render(doc)).isNotEmpty();
    }

    // ===== orçamentos, CSV e logo =====

    private ReportDocument budgets() {
        List<BudgetRow> rows = List.of(
                new BudgetRow(LocalDate.of(2026, 11, 13), "Andreia Souza", "Plano de tratamento", "DRAFT", 2,
                        635_000, 0, null, null, null),
                new BudgetRow(LocalDate.of(2026, 11, 14), "João; Silva", "Clareamento", "APPROVED", 1,
                        100_000, 10_000, PaymentMethod.CREDIT_CARD, 3, LocalDate.of(2026, 11, 14)));
        BudgetsDataset data = new BudgetsDataset(List.of(new StatusSummary("DRAFT", 1, 635_000),
                new StatusSummary("APPROVED", 1, 90_000), new StatusSummary("LOST", 0, 0)), rows);
        return ReportDocumentFactory.budgets(data, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                COMPANY, ISSUED);
    }

    private static String pngDataUrl() throws Exception {
        BufferedImage image = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    @Test
    void budgetsPdfHasLogoSummaryAndDetail() throws Exception {
        byte[] withLogo = PdfReportLayout.render(budgets(), pngDataUrl());
        byte[] withoutLogo = PdfReportLayout.render(budgets());

        String text = new PdfTextExtractor(new PdfReader(withLogo)).getTextFromPage(1);
        assertThat(text).contains("ORÇAMENTOS").contains("Resumo por situação").contains("Em aberto")
                .contains("Andreia Souza").contains("Cartão de crédito");
        // A logo entra como imagem: o arquivo cresce e o texto continua o mesmo.
        assertThat(withLogo.length).isGreaterThan(withoutLogo.length);
    }

    @Test
    void invalidOrExternalLogoIsIgnored() {
        assertThat(PdfReportLayout.logo("https://exemplo.com/logo.png")).isNull();
        assertThat(PdfReportLayout.logo("data:image/svg+xml;base64,PHN2Zz4=")).isNull();
        assertThat(PdfReportLayout.logo("data:image/png;base64,naoeimagem")).isNull();
        assertThat(PdfReportLayout.render(budgets(), "data:image/png;base64,naoeimagem")).isNotEmpty();
    }

    @Test
    void budgetsXlsxHasSummaryAndDetailSheets() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(XlsxReportWriter.write(budgets())))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
        }
    }

    @Test
    void csvHasBomSemicolonsCommaDecimalsAndEscaping() {
        byte[] csv = CsvReportWriter.write(budgets());

        assertThat(csv[0]).isEqualTo((byte) 0xEF);
        assertThat(csv[1]).isEqualTo((byte) 0xBB);
        assertThat(csv[2]).isEqualTo((byte) 0xBF);
        String text = new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);
        assertThat(text).startsWith("Orçamentos\r\nPeríodo;01/11/2026 a 30/11/2026\r\n");
        assertThat(text).contains("Situação;Orçamentos;Valor total\r\n");
        assertThat(text).contains("Em aberto;1;6350,00\r\n");
        assertThat(text).contains("13/11/2026;Andreia Souza;Plano de tratamento;Em aberto;2;6350,00;0,00;6350,00;—;\r\n");
        assertThat(text).contains("\"João; Silva\";Clareamento;Aprovado;1;1000,00;-100,00;900,00;Cartão de crédito em 3x;14/11/2026");
        assertThat(text).contains("Total;;;;;;;7250,00;;");
    }

    @Test
    void csvEscapesQuotesAndLineBreaks() {
        assertThat(CsvReportWriter.escape("diz \"oi\"")).isEqualTo("\"diz \"\"oi\"\"\"");
        assertThat(CsvReportWriter.escape("linha1\nlinha2")).isEqualTo("\"linha1\nlinha2\"");
        assertThat(CsvReportWriter.escape("simples")).isEqualTo("simples");
    }

    // ===== formula injection =====

    private ReportDocument budgetsWithFormula() {
        List<BudgetRow> rows = List.of(new BudgetRow(LocalDate.of(2026, 11, 14), "=SUM(A1)", "Clareamento",
                "APPROVED", 1, 100_000, 10_000, PaymentMethod.CREDIT_CARD, 3, LocalDate.of(2026, 11, 14)));
        BudgetsDataset data = new BudgetsDataset(List.of(new StatusSummary("APPROVED", 1, 90_000)), rows);
        return ReportDocumentFactory.budgets(data, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                COMPANY, ISSUED);
    }

    @Test
    void sanitizerPrefixesOnlyDangerousText() {
        assertThat(SpreadsheetSanitizer.sanitize("=SUM(A1)")).isEqualTo("'=SUM(A1)");
        assertThat(SpreadsheetSanitizer.sanitize("+1")).isEqualTo("'+1");
        assertThat(SpreadsheetSanitizer.sanitize("-2")).isEqualTo("'-2");
        assertThat(SpreadsheetSanitizer.sanitize("@cmd")).isEqualTo("'@cmd");
        assertThat(SpreadsheetSanitizer.sanitize("\tx")).isEqualTo("'\tx");
        assertThat(SpreadsheetSanitizer.sanitize("\rx")).isEqualTo("'\rx");
        assertThat(SpreadsheetSanitizer.sanitize("Andreia Souza")).isEqualTo("Andreia Souza");
        assertThat(SpreadsheetSanitizer.sanitize("")).isEmpty();
        assertThat(SpreadsheetSanitizer.sanitize(null)).isNull();
    }

    @Test
    void csvSanitizesFormulaTextButNotNegativeMoney() {
        byte[] csv = CsvReportWriter.write(budgetsWithFormula());
        String text = new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);

        assertThat(text).contains(";'=SUM(A1);Clareamento;");
        assertThat(text).doesNotContain(";=SUM(A1)");
        // Desconto negativo de coluna MONEY continua número, sem apóstrofo.
        assertThat(text).contains(";-100,00;").doesNotContain("'-100,00");
        assertThat(CsvReportWriter.text(new ReportDocument.Money(-1_234))).isEqualTo("-12,34");
        assertThat(CsvReportWriter.text(-5L)).isEqualTo("-5");
        assertThat(CsvReportWriter.text("Texto normal")).isEqualTo("Texto normal");
    }

    @Test
    void xlsxSanitizesFormulaTextButKeepsNegativeMoneyNumeric() throws Exception {
        byte[] xlsx = XlsxReportWriter.write(budgetsWithFormula());

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            boolean foundFormula = false;
            boolean foundNegative = false;
            for (Sheet sheet : workbook) {
                for (Row row : sheet) {
                    for (org.apache.poi.ss.usermodel.Cell cell : row) {
                        assertThat(cell.getCellType()).isNotEqualTo(CellType.FORMULA);
                        if (cell.getCellType() == CellType.STRING) {
                            assertThat(cell.getStringCellValue()).isNotEqualTo("=SUM(A1)");
                            foundFormula |= cell.getStringCellValue().equals("'=SUM(A1)");
                        } else if (cell.getCellType() == CellType.NUMERIC) {
                            foundNegative |= cell.getNumericCellValue() == -100.0;
                        }
                    }
                }
            }
            assertThat(foundFormula).isTrue();
            assertThat(foundNegative).isTrue();
        }
    }

    @Test
    void emptyNoticeAppearsInPdfCsvAndXlsx() throws Exception {
        ReportDocument doc = ReportDocumentFactory.transactions(new TransactionsDataset(List.of(), 0, 0),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), COMPANY, ISSUED)
                .withNotice(ReportDocument.EMPTY_NOTICE);

        String pdf = new PdfTextExtractor(new PdfReader(PdfReportLayout.render(doc))).getTextFromPage(1);
        assertThat(pdf).contains("Nenhum registro encontrado para o período.");

        String csv = new String(CsvReportWriter.write(doc), StandardCharsets.UTF_8);
        assertThat(csv).contains("\r\nNenhum registro encontrado para o período.\r\n");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(XlsxReportWriter.write(doc)))) {
            assertThat(workbook.getSheetAt(0).getRow(3).getCell(0).getStringCellValue())
                    .isEqualTo("Nenhum registro encontrado para o período.");
        }
    }

    @Test
    void documentWithoutNoticeHasNoEmptyMessage() {
        String csv = new String(CsvReportWriter.write(dre()), StandardCharsets.UTF_8);
        assertThat(csv).doesNotContain("Nenhum registro");
    }

    /** Volume próximo do limite: só confere a contagem de linhas geradas. */
    @Test
    void largeVolumeXlsxAndCsvKeepAllRows() throws Exception {
        int total = 20_000;
        List<ReportDocument.Row> rows = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            rows.add(ReportDocument.Row.item("Paciente " + i, LocalDate.of(2026, 6, 1),
                    new ReportDocument.Money(i * 100L)));
        }
        ReportDocument doc = new ReportDocument("Volume", "06/2026", COMPANY.name(), COMPANY.cnpj(), ISSUED,
                false, List.of(new ReportDocument.ReportTable(null, List.of(
                        ReportDocument.Column.text("Nome", 3), ReportDocument.Column.date("Data"),
                        ReportDocument.Column.money("Valor")), rows)));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(XlsxReportWriter.write(doc)))) {
            Sheet sheet = workbook.getSheetAt(0);
            // Cabeçalho na linha 4 (base 0) + uma linha por item.
            assertThat(sheet.getLastRowNum()).isEqualTo(4 + total);
            assertThat(sheet.getRow(5 + total - 1).getCell(2).getNumericCellValue()).isEqualTo(total - 1.0);
        }

        String csv = new String(CsvReportWriter.write(doc), StandardCharsets.UTF_8);
        assertThat(csv.lines().filter(l -> l.startsWith("Paciente ")).count()).isEqualTo(total);
    }
}
