package com.becommerce.crm.clinical.anamnesis.formrequest;

import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisModelResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisSectionResponse;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatientFormRequestServiceTest {

    private static AnamnesisSectionResponse section(String title, boolean professional) {
        return new AnamnesisSectionResponse(UUID.randomUUID(), title, professional, 0, List.of());
    }

    @Test
    void patientViewHidesProfessionalSections() {
        AnamnesisModelResponse model = new AnamnesisModelResponse(UUID.randomUUID(), UUID.randomUUID(),
                "Anamnese", null, true, false, 1,
                List.of(section("Saúde", false), section("Exame clínico", true)),
                LocalDateTime.now(), LocalDateTime.now());

        AnamnesisModelResponse view = PatientFormRequestService.patientView(model);

        assertThat(view.sections()).extracting(AnamnesisSectionResponse::title).containsExactly("Saúde");
    }

    @Test
    void acceptsRealPngSignature() {
        byte[] png = new byte[300];
        png[0] = (byte) 0x89;
        png[1] = 'P';
        png[2] = 'N';
        png[3] = 'G';
        String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(png);

        assertThat(PatientFormRequestService.decodeSignature(dataUrl)).hasSize(300);
    }

    @Test
    void rejectsMissingBlankOrNonPngSignature() {
        assertThatThrownBy(() -> PatientFormRequestService.decodeSignature(null))
                .isInstanceOf(ContactValidationException.class);
        assertThatThrownBy(() -> PatientFormRequestService.decodeSignature(
                "data:image/png;base64," + Base64.getEncoder().encodeToString(new byte[10])))
                .isInstanceOf(ContactValidationException.class);
        assertThatThrownBy(() -> PatientFormRequestService.decodeSignature(
                "data:image/jpeg;base64,AAAA"))
                .isInstanceOf(ContactValidationException.class);
    }
}
