package com.becommerce.crm.clinical.trichology.domain.exception;

/** Exame, imagem, protocolo ou código de verificação inexistente para a empresa (404). */
public class TrichologyNotFoundException extends RuntimeException {
    public TrichologyNotFoundException(String message) {
        super(message);
    }
}
