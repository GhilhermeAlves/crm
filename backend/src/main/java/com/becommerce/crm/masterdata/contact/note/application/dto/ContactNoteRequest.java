package com.becommerce.crm.masterdata.contact.note.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de criação/edição de uma anotação do paciente. */
public record ContactNoteRequest(
        @NotBlank(message = "A anotação não pode ser vazia.")
        @Size(max = 5000, message = "A anotação deve ter no máximo 5000 caracteres.")
        String body,
        /** Dente (FDI) ao qual a anotação se refere; null = anotação geral. */
        Integer tooth) {

    public ContactNoteRequest(String body) {
        this(body, null);
    }
}
