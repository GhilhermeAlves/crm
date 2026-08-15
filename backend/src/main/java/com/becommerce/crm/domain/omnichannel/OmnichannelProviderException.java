package com.becommerce.crm.domain.omnichannel;

/** Falha determinística de um provider de WhatsApp (envio/consulta). */
public class OmnichannelProviderException extends RuntimeException {

    public OmnichannelProviderException(String message) {
        super(message);
    }

    public OmnichannelProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}