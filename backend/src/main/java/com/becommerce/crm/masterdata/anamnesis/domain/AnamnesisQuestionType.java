package com.becommerce.crm.masterdata.anamnesis.domain;

/**
 * Tipo de resposta de uma pergunta de anamnese. Persistido como {@code String}
 * (coluna {@code question_type}) e validado pelo CHECK da V091.
 *
 * <p>{@link #YES_NO_WITH_TEXT} é o atalho de "Sim/Não com campo complementar"
 * (equivale a {@code YES_NO} + {@code allowComplement=true}); o campo
 * complementar também pode ser configurado de forma ortogonal em qualquer tipo,
 * permitindo condicionar sua exibição a "Sim", "Não", "Sempre" ou a uma opção
 * específica ({@link AnamnesisComplementTrigger}).
 */
public enum AnamnesisQuestionType {
    SHORT_TEXT,
    LONG_TEXT,
    YES_NO,
    YES_NO_WITH_TEXT,
    NUMBER,
    SINGLE_SELECT,
    MULTI_SELECT
}
