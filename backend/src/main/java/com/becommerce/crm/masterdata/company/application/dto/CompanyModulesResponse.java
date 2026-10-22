package com.becommerce.crm.masterdata.company.application.dto;

import java.util.List;

public record CompanyModulesResponse(String companyId, String segment, List<String> enabledModules) {
}
