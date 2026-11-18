package com.becommerce.crm.finance.patientbilling;

/** Regra de negócio da cobrança de pacientes (vira 400 com a mensagem). */
public class PatientBillingException extends RuntimeException {
    public PatientBillingException(String message) {
        super(message);
    }
}
