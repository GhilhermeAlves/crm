package com.becommerce.crm.reports.export.application.service.render;

import com.becommerce.crm.reports.export.application.dto.ActivitiesDataset;
import com.becommerce.crm.reports.export.application.dto.ActivitiesDataset.TypeCount;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.ActivityRow;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.CompanyHeader;
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

/** Relatório de atividades: contagem por tipo, rótulos pt-BR e sem descrição livre. */
class ActivitiesReportTest {

    private static final LocalDate FROM = LocalDate.of(2026, 6, 1);
    private static final LocalDate TO = LocalDate.of(2026, 6, 30);

    private static List<ActivityRow> rows() {
        return List.of(
                new ActivityRow(FROM, "CALL", "Retorno do orçamento", "Ana Souza", "Carla"),
                new ActivityRow(FROM.plusDays(1), "CALL", "Confirmação", "", "Carla"),
                new ActivityRow(FROM.plusDays(2), "MEETING", "Avaliação", "Bruno Lima", ""));
    }

    private static ActivitiesDataset dataset() {
        ReportDataGateway gateway = mock(ReportDataGateway.class);
        when(gateway.activities(any(UUID.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(rows());
        return new ReportDatasetBuilder(gateway).activities(UUID.randomUUID(), FROM, TO);
    }

    private static ReportDocument document() {
        return ReportDocumentFactory.activities(dataset(), FROM, TO,
                new CompanyHeader("Clínica", "00.000.000/0001-00"), LocalDateTime.of(2026, 7, 1, 9, 0));
    }

    @Test
    void countsByTypeInEnumOrder() {
        ActivitiesDataset data = dataset();

        assertThat(data.count()).isEqualTo(3);
        assertThat(data.byType()).extracting(TypeCount::type).containsExactly("CALL", "MEETING", "EMAIL",
                "MESSAGE", "NOTE", "PROPOSAL", "FOLLOW_UP", "OTHER");
        assertThat(data.byType()).extracting(TypeCount::count).containsExactly(2L, 1L, 0L, 0L, 0L, 0L, 0L, 0L);
    }

    @Test
    void documentHasSummaryAndDetailWithPtBrLabels() {
        ReportDocument doc = document();

        assertThat(doc.title()).isEqualTo(ReportExportType.ACTIVITIES.label());
        ReportTable summary = doc.sections().get(0);
        Row total = summary.rows().get(summary.rows().size() - 1);
        assertThat(total.kind()).isEqualTo(RowKind.TOTAL);
        assertThat(total.cells()).containsExactly("Total", 3L);
        assertThat(summary.rows().get(0).cells()).containsExactly("Ligação", 2L);
        assertThat(summary.rows().get(1).cells()).containsExactly("Reunião", 1L);

        ReportTable detail = doc.sections().get(1);
        assertThat(detail.columns()).extracting(ReportDocument.Column::header)
                .containsExactly("Data", "Tipo", "Assunto", "Contato", "Registrado por");
        assertThat(detail.rows()).hasSize(3);
        assertThat(detail.rows().get(0).cells()).containsExactly(FROM, "Ligação", "Retorno do orçamento",
                "Ana Souza", "Carla");
        assertThat(detail.rows().get(1).cells().get(3)).isEqualTo("—");
        assertThat(detail.rows().get(2).cells().get(4)).isEqualTo("—");
    }

    @Test
    void unknownTypeKeepsRawValue() {
        assertThat(ReportDocumentFactory.activityTypeLabel("FOLLOW_UP")).isEqualTo("Follow-up");
        assertThat(ReportDocumentFactory.activityTypeLabel("XYZ")).isEqualTo("XYZ");
    }
}
