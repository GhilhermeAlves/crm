package com.becommerce.crm.reports.export.application.service.render;

import com.becommerce.crm.reports.export.application.service.render.ReportDocument.Column;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.ReportTable;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.Row;
import com.becommerce.crm.reports.export.application.service.render.ReportFieldMasker.SensitiveField;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportFieldMaskerTest {

    private static final ReportFieldMasker MASKED = ReportFieldMasker.forExport(false);
    private static final ReportFieldMasker UNMASKED = ReportFieldMasker.forExport(true);

    @Test
    void cpfEmailAndPhoneKeepOnlyPartialDigits() {
        assertThat(MASKED.mask(SensitiveField.CPF, "123.456.789-01")).isEqualTo("***.456.789-**");
        assertThat(MASKED.mask(SensitiveField.CPF, "12345678901")).isEqualTo("***.456.789-**");
        assertThat(MASKED.mask(SensitiveField.CPF, "123")).isEqualTo(ReportFieldMasker.OMITTED);
        assertThat(MASKED.mask(SensitiveField.EMAIL, "joao@dominio.com")).isEqualTo("j***@dominio.com");
        assertThat(MASKED.mask(SensitiveField.EMAIL, "invalido")).isEqualTo(ReportFieldMasker.OMITTED);
        assertThat(MASKED.mask(SensitiveField.PHONE, "(11) 98765-1234")).isEqualTo("(11) *****-1234");
        assertThat(MASKED.mask(SensitiveField.PHONE, "+55 11 98765-1234")).isEqualTo("(11) *****-1234");
        assertThat(MASKED.mask(SensitiveField.PHONE, "98765-1234")).isEqualTo("*****-1234");
    }

    @Test
    void otherSensitiveFieldsAreOmitted() {
        assertThat(MASKED.mask(SensitiveField.ADDRESS, "Rua A, 10")).isEqualTo("—");
        assertThat(MASKED.mask(SensitiveField.BIRTH_DATE, LocalDate.of(1990, 1, 2))).isEqualTo("—");
        assertThat(MASKED.mask(SensitiveField.BANK_DATA, "chave-pix")).isEqualTo("—");
        assertThat(MASKED.mask(SensitiveField.CLINICAL, "Alopecia")).isEqualTo("—");
        assertThat(MASKED.mask(SensitiveField.EMAIL, null)).isNull();
    }

    @Test
    void unmaskedKeepsValues() {
        assertThat(UNMASKED.mask(SensitiveField.CPF, "123.456.789-01")).isEqualTo("123.456.789-01");
        assertThat(UNMASKED.mask(SensitiveField.CLINICAL, "Alopecia")).isEqualTo("Alopecia");
    }

    @Test
    void crmAdminAndSensitivePermissionSeeUnmasked() {
        assertThat(ReportFieldMasker.canSeeUnmasked(List.of("CRM_ADMIN"), List.of())).isTrue();
        assertThat(ReportFieldMasker.canSeeUnmasked(List.of(), List.of("person:sensitive"))).isTrue();
        assertThat(ReportFieldMasker.canSeeUnmasked(List.of("USER"), List.of("agenda:read"))).isFalse();
        assertThat(ReportFieldMasker.canSeeUnmasked(null, null)).isFalse();
    }

    @Test
    void documentPipelineMasksOnlyClassifiedColumnsInAllFormats() {
        ReportDocument document = document();

        ReportDocument masked = MASKED.apply(document);
        List<Object> cells = masked.sections().get(0).rows().get(1).cells();
        assertThat(cells).containsExactly("Maria Souza", "***.456.789-**", "m***@x.com", "(11) *****-1234",
                "—", "—");
        // Linha de grupo e nome intactos.
        assertThat(masked.sections().get(0).rows().get(0)).isEqualTo(document.sections().get(0).rows().get(0));

        String csv = new String(CsvReportWriter.write(masked), StandardCharsets.UTF_8);
        assertThat(csv).contains("Maria Souza").contains("***.456.789-**").doesNotContain("123.456.789-01")
                .doesNotContain("maria@x.com").doesNotContain("98765");
        assertThat(XlsxReportWriter.write(masked)).isNotEmpty();
        assertThat(PdfReportLayout.render(masked, null)).isNotEmpty();
    }

    @Test
    void documentPipelineWithUnmaskedFlagIsUntouched() {
        ReportDocument document = document();
        assertThat(UNMASKED.apply(document)).isSameAs(document);
        String csv = new String(CsvReportWriter.write(UNMASKED.apply(document)), StandardCharsets.UTF_8);
        assertThat(csv).contains("123.456.789-01").contains("maria@x.com");
    }

    @Test
    void documentWithoutSensitiveColumnsIsReturnedAsIs() {
        ReportDocument plain = new ReportDocument("T", "P", "Empresa", null, LocalDateTime.now(), false,
                List.of(new ReportTable(null, List.of(Column.text("Paciente", 2f)),
                        List.of(Row.item("Maria Souza")))));
        assertThat(MASKED.apply(plain)).isSameAs(plain);
    }

    private static ReportDocument document() {
        List<Column> columns = List.of(
                Column.text("Nome", 2f),
                Column.personal("CPF", 1.2f, SensitiveField.CPF),
                Column.personal("E-mail", 1.6f, SensitiveField.EMAIL),
                Column.personal("Telefone", 1.2f, SensitiveField.PHONE),
                new Column("Nascimento", ReportDocument.ColumnKind.DATE, 1.1f, SensitiveField.BIRTH_DATE),
                Column.personal("Endereço", 2f, SensitiveField.ADDRESS));
        List<Row> rows = List.of(
                Row.group("Grupo"),
                Row.item("Maria Souza", "123.456.789-01", "maria@x.com", "(11) 98765-1234",
                        LocalDate.of(1990, 1, 2), "Rua A, 10"));
        return new ReportDocument("Contatos", "jun/2026", "Empresa", null, LocalDateTime.of(2026, 6, 30, 10, 0),
                true, List.of(new ReportTable("Lista", columns, rows)));
    }
}
