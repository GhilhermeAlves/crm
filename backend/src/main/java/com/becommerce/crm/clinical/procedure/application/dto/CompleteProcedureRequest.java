package com.becommerce.crm.clinical.procedure.application.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CompleteProcedureRequest(@NotNull LocalDate performedAt, UUID professionalId,
                                       boolean generateTransaction, LocalDate dueDate) {}
