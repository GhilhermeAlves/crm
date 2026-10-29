package com.becommerce.crm.clinical.anamnesis.formrequest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenFormServiceTest {

    @Test
    void phoneKeyIgnoresCountryCodeAndExtraNine() {
        String key = OpenFormService.phoneKey("(11) 98765-4321");
        assertThat(OpenFormService.phoneKey("5511987654321")).isEqualTo(key);
        assertThat(OpenFormService.phoneKey("1187654321")).isEqualTo(key);
        assertThat(OpenFormService.phoneKey("+55 11 8765-4321")).isEqualTo(key);
    }

    @Test
    void phoneKeyDistinguishesAreaCodeAndShortNumbers() {
        assertThat(OpenFormService.phoneKey("21987654321"))
                .isNotEqualTo(OpenFormService.phoneKey("11987654321"));
        assertThat(OpenFormService.phoneKey("87654321")).isNull();
        assertThat(OpenFormService.phoneKey(null)).isNull();
    }
}
