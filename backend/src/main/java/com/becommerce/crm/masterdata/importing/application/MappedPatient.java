package com.becommerce.crm.masterdata.importing.application;

import com.becommerce.crm.masterdata.contact.application.dto.request.CreateContactRequest;
import java.util.List;

public record MappedPatient(int line, String name, CreateContactRequest request,
                            boolean discarded, List<String> warnings) {}
