package com.becommerce.crm.sales.recall.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecallSuggestionResponse(
        UUID contactId,
        String contactName,
        String mobile,
        String phone,
        UUID hostId,
        Instant lastAppointmentAt,
        List<String> recentProcedures
) {}
