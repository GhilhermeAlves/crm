package com.becommerce.crm.masterdata.contact.note.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Anotação do paciente exposta na ficha, com o nome do autor resolvido. */
public record ContactNoteResponse(
        UUID id,
        UUID contactId,
        String body,
        Integer tooth,
        UUID createdBy,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
