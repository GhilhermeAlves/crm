package com.becommerce.crm.reports.export.application.service.render;

import com.becommerce.crm.reports.export.application.dto.LeadsDataset;
import com.becommerce.crm.reports.export.application.dto.LeadsDataset.StatusCount;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.CompanyHeader;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.LeadRow;
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

/** Relatório de leads: contagem por status, rótulos pt-BR e máscara LGPD de e-mail/telefone. */
class LeadsReportTest {

    private static final LocalDate FROM = LocalDate.of(2026, 6, 1);
    private static final LocalDate TO = LocalDate.of(2026, 6, 30);

    private static List<LeadRow> rows() {
        return List.of(
                new LeadRow(FROM, "Ana Souza", "ana@dominio.com", "(11) 98765-4321", "WHATSAPP", "NEW",
                        "Carla", 40),
                new LeadRow(FROM.plusDays(1), "Bruno Lima", "bruno@dominio.com", "(21) 91234-5678", "FORM",
                        "NEW", "", 10),
                new LeadRow(FROM.plusDays(2), "Caio", null, null, "INSTAGRAM", "CONVERTED", "Carla", 90));
    }

    private static LeadsDataset dataset() {
        ReportDataGateway gateway = mock(ReportDataGateway.class);
        when(gateway.leads(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(rows());
        return new ReportDatasetBuilder(gateway).leads(UUID.randomUUID(), FROM, TO);
    }

    private static ReportDocument document() {
        return ReportDocumentFactory.leads(dataset(), FROM, TO, new CompanyHeader("Clínica", "00.000.000/0001-00"),
                LocalDateTime.of(2026, 7, 1, 9, 0));
    }

    @Test
    void countsByStatusInFunnelOrder() {
        LeadsDataset data = dataset();

        assertThat(data.count()).isEqualTo(3);
        assertThat(data.byStatus()).extracting(StatusCount::status)
                .containsExactly("NEW", "CONTACTED", "QUALIFIED", "UNQUALIFIED", "CONVERTED", "LOST");
        assertThat(data.byStatus()).extracting(StatusCount::count).containsExactly(2L, 0L, 0L, 0L, 1L, 0L);
    }

    @Test
    void documentHasSummaryAndDetailWithPtBrLabels() {
        ReportDocument doc = document();

        assertThat(doc.title()).isEqualTo(ReportExportType.LEADS.label());
        ReportTable summary = doc.sections().get(0);
        Row total = summary.rows().get(summary.rows().size() - 1);
        assertThat(total.kind()).isEqualTo(RowKind.TOTAL);
        assertThat(total.cells()).containsExactly("Total", 3L);
        assertThat(summary.rows().get(0).cells()).containsExactly("Novo", 2L);

        ReportTable detail = doc.sections().get(1);
        assertThat(detail.rows()).hasSize(3);
        assertThat(detail.rows().get(0).cells()).containsExactly(FROM, "Ana Souza", "ana@dominio.com",
                "(11) 98765-4321", "WhatsApp", "Novo", "Carla", 40L);
        assertThat(detail.rows().get(1).cells().get(4)).isEqualTo("Formulário");
        assertThat(detail.rows().get(1).cells().get(6)).isEqualTo("—");
        assertThat(detail.rows().get(2).cells().get(5)).isEqualTo("Convertido");
    }

    @Test
    void masksEmailAndPhoneUnlessUnmasked() {
        ReportDocument doc = document();

        List<Object> masked = ReportFieldMasker.forExport(false).apply(doc).sections().get(1).rows().get(0).cells();
        assertThat(masked.get(2)).isEqualTo("a***@dominio.com");
        assertThat((String) masked.get(3)).doesNotContain("98765").endsWith("4321");
        assertThat(masked.get(1)).isEqualTo("Ana Souza");

        List<Object> plain = ReportFieldMasker.forExport(true).apply(doc).sections().get(1).rows().get(0).cells();
        assertThat(plain.get(2)).isEqualTo("ana@dominio.com");
        assertThat(plain.get(3)).isEqualTo("(11) 98765-4321");
    }
}
