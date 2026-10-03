package com.becommerce.crm.sales.scheduling.domain.exception;

public class SlotUnavailableException extends RuntimeException {

    public SlotUnavailableException() {
        super("Esse horário acabou de ser ocupado. Escolha outro horário.");
    }
}
