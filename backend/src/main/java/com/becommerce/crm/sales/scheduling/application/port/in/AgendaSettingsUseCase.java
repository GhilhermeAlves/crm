package com.becommerce.crm.sales.scheduling.application.port.in;

import com.becommerce.crm.sales.scheduling.application.dto.AgendaSettingsDto;

import java.util.UUID;

public interface AgendaSettingsUseCase {
    /** Ajustes salvos ou, se a empresa ainda não salvou, os valores padrão. */
    AgendaSettingsDto get(UUID companyId);
    AgendaSettingsDto save(UUID companyId, AgendaSettingsDto request);
}
