package com.becommerce.crm.automation.ai.application.memory;

import com.becommerce.crm.automation.ai.domain.MemoryType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryWritePolicyTest {

    private final MemoryWritePolicy policy = new MemoryWritePolicy();

    @ParameterizedTest
    @ValueSource(strings = {"Oi", "Obrigado!", "Bom dia", "ok", "tudo bem?", "Oi, bom dia!", "Ok, obrigada."})
    void rejectsTrivialMessages(String content) {
        assertTrue(policy.rejectionReason(MemoryType.FACT, content).isPresent(), content);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Consulta agendada para quinta-feira às 15h",
            "Maria está marcada amanhã",
            "Às 14:30 ficou confirmado o retorno"})
    void rejectsTransactionalSchedulingData(String content) {
        assertTrue(policy.rejectionReason(MemoryType.CONTEXTUAL, content).isPresent(), content);
    }

    @Test
    void rejectsPersonalDocuments() {
        assertTrue(policy.rejectionReason(MemoryType.FACT, "O CPF dele é 123.456.789-09").isPresent());
    }

    @Test
    void rejectsMissingType() {
        assertTrue(policy.rejectionReason(null, "Prefere atendimento pela manhã").isPresent());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Prefere atendimento no período da manhã.",
            "Já realizou avaliação odontológica.",
            "Demonstrou interesse em clareamento dental.",
            "Aguardando retorno sobre orçamento."})
    void acceptsDurableInformation(String content) {
        assertFalse(policy.rejectionReason(MemoryType.PREFERENCE, content).isPresent(), content);
    }
}
