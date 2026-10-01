package com.becommerce.crm.automation.campaign.domain.exception;

import java.util.UUID;

public class CampaignNotFoundException extends RuntimeException {

    public CampaignNotFoundException(UUID id) {
        super("Campanha não encontrada: " + id);
    }
}
