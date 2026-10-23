package com.becommerce.crm.masterdata.contact.registration.domain.exception;

/** Excesso de requisições às rotas públicas de cadastro (429). */
public class RegistrationRateLimitException extends RuntimeException {
    public RegistrationRateLimitException() {
        super("Muitas tentativas. Tente novamente mais tarde.");
    }
}
