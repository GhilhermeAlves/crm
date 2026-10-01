package com.becommerce.crm.masterdata.contact.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateContactRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Email @Size(max = 255) String email,
        @Size(max = 20) String phone,
        @Size(max = 500) String notes) {
}