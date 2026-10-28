package com.becommerce.crm.masterdata.company.mail;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyMailServiceTest {

    @Test
    void deducesServerFromCommonProviders() {
        assertThat(CompanyMailService.knownServer("clinica@Gmail.com"))
                .contains(new CompanyMailService.Server("smtp.gmail.com", 587));
        assertThat(CompanyMailService.knownServer("contato@hotmail.com"))
                .contains(new CompanyMailService.Server("smtp-mail.outlook.com", 587));
        assertThat(CompanyMailService.knownServer("contato@minhaclinica.com.br")).isEmpty();
    }

    @Test
    void explainsAuthenticationFailure() {
        assertThat(CompanyMailService.explain(new RuntimeException("535 Authentication failed")))
                .contains("senha de app");
    }

    @Test
    void cipherRoundTripsAndRejectsOtherKey() {
        MailPasswordCipher cipher = new MailPasswordCipher("chave-a", "");
        String stored = cipher.encrypt("abcd efgh");
        assertThat(stored).doesNotContain("abcd");
        assertThat(cipher.decrypt(stored)).isEqualTo("abcd efgh");
        assertThatThrownBy(() -> new MailPasswordCipher("chave-b", "").decrypt(stored))
                .isInstanceOf(IllegalStateException.class);
    }
}
