package com.becommerce.crm.reports.export.application.service.render;

import com.becommerce.crm.reports.export.application.dto.ContactsDataset;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.CompanyHeader;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.ContactRow;
import com.becommerce.crm.reports.export.application.service.ReportDatasetBuilder;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.ReportTable;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.Row;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.RowKind;
import com.becommerce.crm.reports.export.domain.ReportExportType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Relatório de contatos: colunas, total no rodapé e máscara LGPD (CPF mascarado, CNPJ não). */
class ContactsReportTest {

    private static final LocalDate FROM = LocalDate.of(2026, 6, 1);
    private static final LocalDate TO = LocalDate.of(2026, 6, 30);
    private static final LocalDate BIRTH = LocalDate.of(1990, 3, 15);

    private static List<ContactRow> rows() {
        return List.of(
                new ContactRow(FROM, "Ana Souza", "123.456.789-01", "ana@dominio.com", "(11) 98765-4321",
                        "Campinas", "SP", BIRTH, "Indicação", "VIP, Retorno"),
                new ContactRow(FROM.plusDays(1), "Empresa X", "12345678000199", null, null, null, "RJ", null,
                        null, null));
    }

    private static ReportDocument document() {
        ReportDataGateway gateway = mock(ReportDataGateway.class);
        when(gateway.contacts(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(rows());
        ContactsDataset data = new ReportDatasetBuilder(gateway).contacts(UUID.randomUUID(), FROM, TO);
        return ReportDocumentFactory.contacts(data, FROM, TO, new CompanyHeader("Clínica", "00.000.000/0001-00"),
                LocalDateTime.of(2026, 7, 1, 9, 0));
    }

    @Test
    void documentHasDetailAndGrandTotal() {
        ReportDocument doc = document();

        assertThat(doc.title()).isEqualTo(ReportExportType.CONTACTS.label());
        ReportTable detail = doc.sections().get(0);
        assertThat(detail.rows()).hasSize(3);
        assertThat(detail.rows().get(0).cells()).containsExactly(FROM, "Ana Souza", "123.456.789-01",
                "ana@dominio.com", "(11) 98765-4321", "Campinas/SP", BIRTH, "Indicação", "VIP, Retorno");
        assertThat(detail.rows().get(1).cells().get(5)).isEqualTo("RJ");
        assertThat(detail.rows().get(1).cells().get(7)).isEqualTo("—");
        Row total = detail.rows().get(2);
        assertThat(total.kind()).isEqualTo(RowKind.TOTAL);
        assertThat(total.cells().get(0)).isEqualTo("Total");
        assertThat(total.cells().get(1)).isEqualTo("2 contatos");
    }

    @Test
    void masksPersonalDataUnlessUnmasked() {
        ReportDocument doc = document();

        List<Row> masked = ReportFieldMasker.forExport(false).apply(doc).sections().get(0).rows();
        List<Object> ana = masked.get(0).cells();
        assertThat(ana.get(2)).isEqualTo("***.456.789-**");
        assertThat(ana.get(3)).isEqualTo("a***@dominio.com");
        assertThat((String) ana.get(4)).doesNotContain("98765").endsWith("4321");
        assertThat(ana.get(6)).isNotEqualTo(BIRTH);
        assertThat(ana.get(1)).isEqualTo("Ana Souza");
        // CNPJ é dado de empresa: não mascara.
        assertThat(masked.get(1).cells().get(2)).isEqualTo("12345678000199");

        List<Object> plain = ReportFieldMasker.forExport(true).apply(doc).sections().get(0).rows().get(0).cells();
        assertThat(plain.get(2)).isEqualTo("123.456.789-01");
        assertThat(plain.get(3)).isEqualTo("ana@dominio.com");
        assertThat(plain.get(4)).isEqualTo("(11) 98765-4321");
        assertThat(plain.get(6)).isEqualTo(BIRTH);
    }
}
