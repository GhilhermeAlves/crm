package com.becommerce.crm.masterdata.anamnesis.domain;

/**
 * Condição que dispara a exibição do campo complementar de uma pergunta:
 * resposta "Sim", resposta "Não", sempre, ou quando uma opção específica for
 * selecionada ({@code OPTION}, com o valor em {@code complementOptionValue}).
 */
public enum AnamnesisComplementTrigger {
    YES,
    NO,
    ALWAYS,
    OPTION
}
