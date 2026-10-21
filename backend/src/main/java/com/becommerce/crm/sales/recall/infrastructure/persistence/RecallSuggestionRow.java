package com.becommerce.crm.sales.recall.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

/**
 * Projeção da query nativa de sugestões de retorno.
 */
public interface RecallSuggestionRow {

    UUID getContactId();

    String getFirstName();

    String getLastName();

    String getMobile();

    String getPhone();

    UUID getHostId();

    Instant getLastAppointmentAt();

    String getRecentProcedures();

    Boolean getHasFutureAppointment();

    Boolean getHasPendingRecall();

    Boolean getDismissed();
}
