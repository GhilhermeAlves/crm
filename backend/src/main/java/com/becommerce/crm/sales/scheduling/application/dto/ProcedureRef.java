package com.becommerce.crm.sales.scheduling.application.dto;

import java.util.UUID;

/** Procedimento (serviço do catálogo) vinculado a um agendamento. */
public record ProcedureRef(UUID id, String name) {}
