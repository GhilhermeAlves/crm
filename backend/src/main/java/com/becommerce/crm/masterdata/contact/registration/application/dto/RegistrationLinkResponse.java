package com.becommerce.crm.masterdata.contact.registration.application.dto;

import java.time.LocalDateTime;

public record RegistrationLinkResponse(String url, LocalDateTime expiresAt) {
}
