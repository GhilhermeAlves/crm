package com.becommerce.crm.billing.web;

/** Empresa bloqueada por mensalidade em atraso (HTTP 402, code BILLING_BLOCKED). */
public class BillingBlockedException extends RuntimeException {
    public BillingBlockedException() {
        super("Mensalidade em atraso: regularize o pagamento para continuar.");
    }
}
