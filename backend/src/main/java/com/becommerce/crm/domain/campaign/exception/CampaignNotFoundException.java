package com.becommerce.crm.domain.campaign.exception;

import java.util.UUID;

public class CampaignNotFoundException extends RuntimeException {

    public CampaignNotFoundException(UUID id) {
        super("Campanha não encontrada: " + id);
    }
}
