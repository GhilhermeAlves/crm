package com.becommerce.crm.domain.omnichannel;

/** Recurso de omnichannel não encontrado ou pertencente a outra empresa. */
public class OmnichannelNotFoundException extends RuntimeException {

    public OmnichannelNotFoundException(java.util.UUID id, String resource) {
        super(resource + " não encontrado: " + id);
    }
}
