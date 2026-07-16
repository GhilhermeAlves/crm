package com.becommerce.crm.application.identity.dto;

import java.util.UUID;

public record RegisterRequest(String email, String password, String name, UUID companyId) {
}
