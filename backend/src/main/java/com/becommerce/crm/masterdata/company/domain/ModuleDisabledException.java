package com.becommerce.crm.masterdata.company.domain;

/** Recurso de um módulo desligado para a empresa (HTTP 403, code MODULE_DISABLED). */
public class ModuleDisabledException extends RuntimeException {

    private final CompanyModule module;

    public ModuleDisabledException(CompanyModule module) {
        super("O módulo " + module.name() + " não está habilitado para esta empresa.");
        this.module = module;
    }

    public CompanyModule getModule() {
        return module;
    }
}
