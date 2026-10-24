package com.becommerce.crm.identity.invitation.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfessionalSpecialtyTest {

    @Test
    void blankBecomesNull() {
        assertThat(ProfessionalSpecialty.normalize(null)).isNull();
        assertThat(ProfessionalSpecialty.normalize("  ")).isNull();
    }

    @Test
    void acceptsKnownValuesIgnoringCase() {
        assertThat(ProfessionalSpecialty.normalize(" trichology ")).isEqualTo("TRICHOLOGY");
        assertThat(ProfessionalSpecialty.normalize("DENTAL")).isEqualTo("DENTAL");
    }

    @Test
    void rejectsUnknownValues() {
        assertThatThrownBy(() -> ProfessionalSpecialty.normalize("GENERAL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Especialidade inválida");
    }
}
