package com.becommerce.crm.automation.campaign.domain.exception;

/** Regra de negócio de campanha violada; a mensagem (pt-BR) é exibida ao usuário com 400. */
public class CampaignValidationException extends RuntimeException {

    public CampaignValidationException(String message) {
        super(message);
    }
}
