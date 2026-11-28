package com.becommerce.crm.masterdata.anamnesis.domain;

import java.util.Set;

import static com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionType.DATE;
import static com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionType.LONG_TEXT;
import static com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionType.MULTI_SELECT;
import static com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionType.SHORT_TEXT;
import static com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionType.SINGLE_SELECT;
import static com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionType.YES_NO;
import static com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionType.YES_NO_WITH_TEXT;

/**
 * Vínculo de uma pergunta de anamnese com o cadastro da pessoa ou com o exame
 * tricológico (spec 2026-11-28-anamnese-fonte-unica). Os vínculos do exame usam
 * as mesmas chaves e códigos que as regras do Copiloto usavam no antigo quiz.
 */
public enum AnamnesisBinding {
    PERSON_NAME(Group.PERSON, Set.of(SHORT_TEXT), null, Set.of()),
    PERSON_BIRTH_DATE(Group.PERSON, Set.of(DATE), null, Set.of()),
    PERSON_SEX(Group.PERSON, Set.of(SINGLE_SELECT), "sex", Set.of("F", "M")),
    PERSON_CPF(Group.PERSON, Set.of(SHORT_TEXT), null, Set.of()),
    PERSON_MOBILE(Group.PERSON, Set.of(SHORT_TEXT), null, Set.of()),
    PERSON_EMAIL(Group.PERSON, Set.of(SHORT_TEXT), null, Set.of()),
    PERSON_ADDRESS(Group.PERSON, Set.of(SHORT_TEXT, LONG_TEXT), null, Set.of()),
    TRICHO_SEX(Group.TRICHO, Set.of(SINGLE_SELECT), "sex", Set.of("F", "M")),
    TRICHO_PATTERN(Group.TRICHO, Set.of(SINGLE_SELECT, MULTI_SELECT), "pattern",
            Set.of("DIFFUSE_SHEDDING", "THINNING_TOP", "PATCHES", "SCALP_SYMPTOMS", "BREAKAGE")),
    TRICHO_ONSET(Group.TRICHO, Set.of(SINGLE_SELECT), "onset", Set.of("LT_3M", "3_12M", "GT_1Y")),
    TRICHO_DAILY_SHEDDING(Group.TRICHO, Set.of(SINGLE_SELECT), "daily_shedding",
            Set.of("LT_50", "50_100", "GT_100")),
    TRICHO_FAMILY_HISTORY(Group.TRICHO, Set.of(YES_NO, YES_NO_WITH_TEXT), "family_history", Set.of()),
    TRICHO_RECENT_TRIGGERS(Group.TRICHO, Set.of(MULTI_SELECT), "recent_triggers",
            Set.of("FEVER_INFECTION", "SURGERY", "WEIGHT_LOSS", "SEVERE_STRESS", "NEW_MEDICATION", "NONE")),
    TRICHO_POSTPARTUM(Group.TRICHO, Set.of(YES_NO, YES_NO_WITH_TEXT), "postpartum", Set.of()),
    TRICHO_SCALP_SYMPTOMS(Group.TRICHO, Set.of(MULTI_SELECT), "scalp_symptoms",
            Set.of("ITCH", "BURNING", "PAIN", "OILINESS", "FLAKING", "NONE")),
    TRICHO_HAIR_PROCEDURES(Group.TRICHO, Set.of(MULTI_SELECT), "hair_procedures",
            Set.of("CHEMICAL", "HEAT", "TIGHT_STYLES", "NONE")),
    TRICHO_ACNE_HIRSUTISM(Group.TRICHO, Set.of(YES_NO, YES_NO_WITH_TEXT), "acne_hirsutism", Set.of()),
    TRICHO_IRREGULAR_CYCLES(Group.TRICHO, Set.of(YES_NO, YES_NO_WITH_TEXT), "irregular_cycles", Set.of()),
    TRICHO_ANABOLIC_USE(Group.TRICHO, Set.of(YES_NO, YES_NO_WITH_TEXT), "anabolic_use", Set.of()),
    TRICHO_CHIEF_COMPLAINT(Group.TRICHO, Set.of(SHORT_TEXT, LONG_TEXT), "chief_complaint", Set.of());

    public enum Group { PERSON, TRICHO }

    private final Group group;
    private final Set<AnamnesisQuestionType> types;
    private final String quizKey;
    private final Set<String> codes;

    AnamnesisBinding(Group group, Set<AnamnesisQuestionType> types, String quizKey, Set<String> codes) {
        this.group = group;
        this.types = types;
        this.quizKey = quizKey;
        this.codes = codes;
    }

    public Group group() { return group; }

    public Set<AnamnesisQuestionType> types() { return types; }

    /** Chave usada pelas regras do Copiloto; {@code null} quando o vínculo não alimenta o exame. */
    public String quizKey() { return quizKey; }

    /** Valores aceitos nas opções; vazio = sem restrição. */
    public Set<String> codes() { return codes; }
}
