package com.becommerce.crm.clinical.documenttemplate.application.dto;

import com.becommerce.crm.clinical.documenttemplate.domain.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo de criação/edição de um modelo de documento. */
public record DocumentTemplateRequest(
        @NotNull(message = "O tipo do documento é obrigatório.")
        DocumentType type,
        @NotBlank(message = "O título do modelo não pode ser vazio.")
        @Size(max = 160, message = "O título do modelo deve ter no máximo 160 caracteres.")
        String title,
        @NotBlank(message = "O corpo do modelo não pode ser vazio.")
        @Size(max = 20000, message = "O corpo do modelo deve ter no máximo 20000 caracteres.")
        String body,
        Boolean active) {
}
