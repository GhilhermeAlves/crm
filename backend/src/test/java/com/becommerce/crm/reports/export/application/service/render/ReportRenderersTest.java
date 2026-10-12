package com.becommerce.crm.reports.export.application.service.render;

import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.reports.export.application.dto.AgendaDataset;
import com.becommerce.crm.reports.export.application.dto.AgendaDataset.ProfessionalSummary;
import com.becommerce.crm.reports.export.application.dto.MonthlyDreDataset;
import com.becommerce.crm.reports.export.application.dto.MonthlyDreDataset.DreGroup;
import com.becommerce.crm.reports.export.application.dto.MonthlyDreDataset.DreLine;
import com.becommerce.crm.reports.export.application.dto.TransactionsDataset;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.AppointmentRow;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.CompanyHeader;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.TransactionRow;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
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
}
