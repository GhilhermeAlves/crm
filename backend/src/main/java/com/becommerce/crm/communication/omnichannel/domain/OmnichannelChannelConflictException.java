package com.becommerce.crm.communication.omnichannel.domain;

/** O external_id (instância/número) já pertence a outro canal, de qualquer empresa. */
public class OmnichannelChannelConflictException extends RuntimeException {

    public OmnichannelChannelConflictException(String externalId) {
        super("Instância já vinculada a outro canal: " + externalId);
    }
}
