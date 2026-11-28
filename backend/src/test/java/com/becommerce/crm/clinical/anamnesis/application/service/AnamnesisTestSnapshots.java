package com.becommerce.crm.clinical.anamnesis.application.service;

import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisModelResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisOptionResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisQuestionResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisSectionResponse;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisBinding;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisComplementTrigger;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Monta snapshots de anamnese para os testes. */
final class AnamnesisTestSnapshots {

    private AnamnesisTestSnapshots() {
    }

    static AnamnesisQuestionResponse q(UUID id, String text, AnamnesisQuestionType type, AnamnesisBinding binding,
                                       String... options) {
        List<AnamnesisOptionResponse> opts = new ArrayList<>();
        for (int i = 0; i < options.length; i++) {
            String[] parts = options[i].split("\\|");
            opts.add(new AnamnesisOptionResponse(UUID.randomUUID(), parts[1], parts[0], i));
        }
        return new AnamnesisQuestionResponse(id, text, type, false, false, false, null,
                AnamnesisComplementTrigger.YES, null, 0, opts, binding);
    }

    static AnamnesisSectionResponse section(String title, boolean professional, AnamnesisQuestionResponse... qs) {
        return new AnamnesisSectionResponse(UUID.randomUUID(), title, professional, 0, List.of(qs));
    }

    static AnamnesisModelResponse snapshot(AnamnesisSectionResponse... sections) {
        return new AnamnesisModelResponse(UUID.randomUUID(), UUID.randomUUID(), "Ficha", null, true, false, 1,
                List.of(sections), null, null);
    }

    static AnamnesisModelResponse snapshot(AnamnesisQuestionResponse... qs) {
        return snapshot(section("Dados", false, qs));
    }
}
