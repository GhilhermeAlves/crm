package com.becommerce.crm.reports.export.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportExportTypeTest {

    @Test
    void catalogMetadataIsConsistent() {
        for (ReportExportType type : ReportExportType.values()) {
            assertThat(type.label()).isNotBlank();
            assertThat(type.description()).isNotBlank();
            assertThat(type.formats()).isNotEmpty();
            assertThat(type.maxMonths()).isBetween(1, ReportExportType.Defaults.MAX_MONTHS);
            assertThat(type.slug()).matches("[a-z0-9-]+");
        }
        assertThat(Arrays.stream(ReportExportType.values()).map(ReportExportType::slug).distinct())
                .hasSize(ReportExportType.values().length);
        assertThat(ReportExportType.MONTHLY_DRE.periodMode()).isEqualTo(ReportPeriodMode.MONTH);
        assertThat(ReportExportType.BUDGETS.supports(ExportFormat.CSV)).isTrue();
    }

    @Test
    void crmAdminAlwaysAllowed() {
        assertThat(ReportExportType.AGENDA.allowedFor(List.of("CRM_ADMIN"), List.of())).isTrue();
        assertThat(ReportExportType.AGENDA.allowedFor(List.of(), List.of("finance:read"))).isFalse();
        assertThat(ReportExportType.AGENDA.allowedFor(null, List.of("appointment:read"))).isTrue();
    }
}
