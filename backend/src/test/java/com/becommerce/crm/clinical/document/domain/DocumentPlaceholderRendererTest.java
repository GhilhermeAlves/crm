package com.becommerce.crm.clinical.document.domain;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentPlaceholderRendererTest {

    @Test
    void replacesAllSupportedKeys() {
        Map<String, String> values = new HashMap<>();
        values.put("paciente.nome", "Maria Silva");
        values.put("paciente.cpf", "123.456.789-01");
        values.put("paciente.nascimento", "10/05/1990");
        values.put("paciente.prontuario", "P-001");
        values.put("data", "22/10/2026");
        values.put("data.extenso", "22 de outubro de 2026");
        values.put("cidade", "São Paulo");
        values.put("empresa.nome", "Clínica Bem");
        values.put("profissional.nome", "Dra. Ana");

        String body = "{{paciente.nome}} | {{paciente.cpf}} | {{paciente.nascimento}} | "
                + "{{paciente.prontuario}} | {{data}} | {{data.extenso}} | {{cidade}} | "
                + "{{empresa.nome}} | {{profissional.nome}}";

        assertThat(DocumentPlaceholderRenderer.render(body, values))
                .isEqualTo("Maria Silva | 123.456.789-01 | 10/05/1990 | P-001 | 22/10/2026 | "
                        + "22 de outubro de 2026 | São Paulo | Clínica Bem | Dra. Ana");
    }

    @Test
    void acceptsSpacesInsideBraces() {
        assertThat(DocumentPlaceholderRenderer.render("Olá {{ paciente.nome }}!",
                Map.of("paciente.nome", "Maria"))).isEqualTo("Olá Maria!");
    }

    @Test
    void unknownKeyBecomesEmpty() {
        assertThat(DocumentPlaceholderRenderer.render("A{{desconhecido}}B", Map.of()))
                .isEqualTo("AB");
    }

    @Test
    void nullValueBecomesEmpty() {
        Map<String, String> values = new HashMap<>();
        values.put("paciente.nome", null);
        assertThat(DocumentPlaceholderRenderer.render("A{{paciente.nome}}B", values)).isEqualTo("AB");
    }
}
