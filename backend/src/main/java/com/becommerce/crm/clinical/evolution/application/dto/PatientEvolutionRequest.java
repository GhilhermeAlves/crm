package com.becommerce.crm.clinical.evolution.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PatientEvolutionRequest(
        @NotBlank @Size(max = 20000) String body,
        UUID appointmentId) {
}
