package com.becommerce.crm.masterdata.importing.application.dto;
import java.util.List;
public record ImportRowResult(int line, String name, String status, String message, List<String> warnings) {}
