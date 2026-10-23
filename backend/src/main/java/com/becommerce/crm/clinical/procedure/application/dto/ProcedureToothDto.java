package com.becommerce.crm.clinical.procedure.application.dto;

import com.becommerce.crm.clinical.procedure.domain.ToothFace;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.Set;

public record ProcedureToothDto(@Min(11) @Max(85) int tooth, Set<ToothFace> faces) {}
