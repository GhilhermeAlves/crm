package com.becommerce.crm.clinical.budget.application.service;

import java.util.List;
import java.util.UUID;

/** Orçamento aprovado com "Enviar cobrança pelo Asaas": parcelas a cobrar (ouvido após o commit). */
public record BudgetChargeRequestedEvent(UUID companyId, List<UUID> transactionIds) {}
