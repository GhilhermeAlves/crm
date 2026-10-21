package com.becommerce.crm.sales.recall.domain;

import com.becommerce.crm.sales.recall.domain.exception.RecallValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecallTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID contactId = UUID.randomUUID();
    private final UUID hostId = UUID.randomUUID();
    private final UUID createdBy = UUID.randomUUID();
    private final LocalDate dueDate = LocalDate.of(2026, 11, 1);

    @Test
    void createsPendingRecall() {
        Recall recall = Recall.create(companyId, contactId, " (11) 99999-8888 ", hostId, " Limpeza ",
                dueDate, createdBy);

        assertEquals(RecallStatus.PENDING, recall.getStatus());
        assertEquals("(11) 99999-8888", recall.getPhone());
        assertEquals("Limpeza", recall.getTag());
        assertEquals(dueDate, recall.getDueDate());
        assertNull(recall.getAppointmentId());
        assertEquals(companyId, recall.getCompanyId());
    }

    @Test
    void rejectsMissingPhone() {
        assertThrows(RecallValidationException.class,
                () -> Recall.create(companyId, contactId, " ", hostId, null, dueDate, createdBy));
    }

    @Test
    void rejectsMissingContact() {
        assertThrows(RecallValidationException.class,
                () -> Recall.create(companyId, null, "11999998888", hostId, null, dueDate, createdBy));
    }

    @Test
    void updateChangesFieldsAndKeepsStatus() {
        Recall recall = Recall.create(companyId, contactId, "11999998888", hostId, null, dueDate, createdBy);
        UUID newHost = UUID.randomUUID();

        recall.update("11988887777", newHost, "Retorno anual", dueDate.plusDays(10));

        assertEquals("11988887777", recall.getPhone());
        assertEquals(newHost, recall.getHostId());
        assertEquals("Retorno anual", recall.getTag());
        assertEquals(dueDate.plusDays(10), recall.getDueDate());
        assertEquals(RecallStatus.PENDING, recall.getStatus());
    }

    @Test
    void scheduleLinksAppointmentAndMarksScheduled() {
        Recall recall = Recall.create(companyId, contactId, "11999998888", hostId, null, dueDate, createdBy);
        UUID appointmentId = UUID.randomUUID();

        recall.schedule(appointmentId);

        assertEquals(RecallStatus.SCHEDULED, recall.getStatus());
        assertEquals(appointmentId, recall.getAppointmentId());
    }

    @Test
    void scheduleRejectsDismissedRecall() {
        Recall recall = Recall.create(companyId, contactId, "11999998888", hostId, null, dueDate, createdBy);
        recall.dismiss();

        assertThrows(RecallValidationException.class, () -> recall.schedule(UUID.randomUUID()));
    }

    @Test
    void dismissMarksDismissedAndClearsAppointment() {
        Recall recall = Recall.create(companyId, contactId, "11999998888", hostId, null, dueDate, createdBy);
        recall.schedule(UUID.randomUUID());

        recall.dismiss();
        recall.dismiss();

        assertEquals(RecallStatus.DISMISSED, recall.getStatus());
        assertNull(recall.getAppointmentId());
    }

    @Test
    void dismissedRecallCannotBeUpdated() {
        Recall recall = Recall.create(companyId, contactId, "11999998888", hostId, null, dueDate, createdBy);
        recall.dismiss();

        assertThrows(RecallValidationException.class, () -> recall.update("11911112222", null, null, null));
    }

    @Test
    void reconstituteKeepsPersistedState() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.now();

        Recall recall = Recall.reconstitute(id, companyId, contactId, "11999998888", hostId, "tag",
                dueDate, RecallStatus.SCHEDULED, UUID.randomUUID(), createdBy, createdAt, createdAt);

        assertEquals(id, recall.getId());
        assertEquals(RecallStatus.SCHEDULED, recall.getStatus());
        assertEquals(createdAt, recall.getCreatedAt());
    }
}
