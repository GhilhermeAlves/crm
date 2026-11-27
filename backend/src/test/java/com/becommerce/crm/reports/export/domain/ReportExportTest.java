package com.becommerce.crm.reports.export.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportExportTest {

    private ReportExport pending() {
        return ReportExport.pending(UUID.randomUUID(), UUID.randomUUID(), ReportExportType.CASH_FLOW,
                ExportFormat.PDF, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
    }

    @Test
    void startsPendingAndBecomesReady() {
        ReportExport export = pending();
        assertThat(export.status()).isEqualTo(ReportExportStatus.PENDING);

        UUID objectId = UUID.randomUUID();
        export.markReady(objectId, "fluxo.pdf");

        assertThat(export.status()).isEqualTo(ReportExportStatus.READY);
        assertThat(export.storageObjectId()).isEqualTo(objectId);
        assertThat(export.fileName()).isEqualTo("fluxo.pdf");
        assertThat(export.finishedAt()).isNotNull();
    }

    @Test
    void failedTruncatesLongMessage() {
        ReportExport export = pending();
        export.markFailed("x".repeat(800));

        assertThat(export.status()).isEqualTo(ReportExportStatus.FAILED);
        assertThat(export.errorMessage()).hasSize(500);
    }

    @Test
    void terminalStatesDoNotChange() {
        ReportExport ready = pending();
        ready.markReady(UUID.randomUUID(), "a.pdf");
        assertThatThrownBy(() -> ready.markFailed("boom")).isInstanceOf(IllegalStateException.class);

        ReportExport failed = pending();
        failed.markFailed("boom");
        assertThatThrownBy(() -> failed.markReady(UUID.randomUUID(), "a.pdf"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void readyExpiresKeepingFileNameForHistory() {
        ReportExport export = pending();
        export.markReady(UUID.randomUUID(), "fluxo.pdf");

        export.markExpired();

        assertThat(export.status()).isEqualTo(ReportExportStatus.EXPIRED);
        assertThat(export.storageObjectId()).isNull();
        assertThat(export.fileName()).isEqualTo("fluxo.pdf");
        assertThat(export.finishedAt()).isNotNull();
    }

    @Test
    void onlyReadyCanExpire() {
        assertThatThrownBy(() -> pending().markExpired()).isInstanceOf(IllegalStateException.class);

        ReportExport failed = pending();
        failed.markFailed("boom");
        assertThatThrownBy(failed::markExpired).isInstanceOf(IllegalStateException.class);

        ReportExport expired = pending();
        expired.markReady(UUID.randomUUID(), "a.pdf");
        expired.markExpired();
        assertThatThrownBy(expired::markExpired).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> expired.markFailed("x")).isInstanceOf(IllegalStateException.class);
    }
}
