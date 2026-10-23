package com.becommerce.crm.masterdata.contact.registration.application.dto;

/** Dados exibidos na página pública /cadastro/{token}. */
public record RegistrationPreviewResponse(String companyName, String firstName, String lastName, String phone) {
}
