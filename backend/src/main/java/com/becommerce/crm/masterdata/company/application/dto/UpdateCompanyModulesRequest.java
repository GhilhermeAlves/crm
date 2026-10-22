package com.becommerce.crm.masterdata.company.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateCompanyModulesRequest(@NotBlank String segment, @NotNull List<String> enabledModules) {
}
