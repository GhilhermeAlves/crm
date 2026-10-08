package com.becommerce.crm.automation.ai.application.agent.context;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Contexto da clínica/empresa, carregado do CRM a cada execução (nunca gravado
 * no prompt). {@code now} já está no fuso da empresa.
 */
public record ClinicContext(String name, String phone, String address, String businessHours,
                            ZoneId zone, ZonedDateTime now) {
}
