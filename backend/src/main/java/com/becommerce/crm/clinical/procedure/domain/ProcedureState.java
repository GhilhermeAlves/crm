package com.becommerce.crm.clinical.procedure.domain;

/** Estado do procedimento na ficha clínica. Persistido como {@code String} (V106). */
public enum ProcedureState {
    PLANNED,
    DONE,
    PRE_EXISTING
}
