package com.becommerce.crm.application.omnichannel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de envio de mensagem no Inbox. */
public record SendMessageRequest(
        @NotBlank @Size(max = 4096) String body
) {
}
