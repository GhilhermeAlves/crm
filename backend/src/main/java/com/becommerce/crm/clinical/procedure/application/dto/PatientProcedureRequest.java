package com.becommerce.crm.clinical.procedure.application.dto;

import com.becommerce.crm.clinical.procedure.domain.Dentition;
import com.becommerce.crm.clinical.procedure.domain.ProcedureState;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PatientProcedureRequest(
        @NotNull UUID catalogItemId,
        @NotNull ProcedureState state,
        @NotNull Dentition dentition,
        @NotNull @PositiveOrZero Long priceCents,
        UUID professionalId,
        @Size(max = 5000) String notes,
        LocalDate performedAt,
        @Valid List<ProcedureToothDto> teeth,
        boolean generateTransaction,
        LocalDate dueDate) {}
