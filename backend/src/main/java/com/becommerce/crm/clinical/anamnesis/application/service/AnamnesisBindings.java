package com.becommerce.crm.clinical.anamnesis.application.service;

import com.becommerce.crm.clinical.anamnesis.application.dto.PatientAnamnesisAnswer;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisModelResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisQuestionResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisSectionResponse;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisBinding;
import com.becommerce.crm.masterdata.person.PersonService;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Leitura dos vínculos de uma anamnese (snapshot + respostas). Sem estado. */
public final class AnamnesisBindings {

    private AnamnesisBindings() {
    }

    /** Pergunta de cada vínculo no snapshot (primeira ocorrência). */
    public static Map<AnamnesisBinding, AnamnesisQuestionResponse> questions(AnamnesisModelResponse snapshot) {
        Map<AnamnesisBinding, AnamnesisQuestionResponse> out = new EnumMap<>(AnamnesisBinding.class);
        if (snapshot == null || snapshot.sections() == null) {
            return out;
        }
        for (AnamnesisSectionResponse section : snapshot.sections()) {
            if (section.questions() == null) {
                continue;
            }
            for (AnamnesisQuestionResponse q : section.questions()) {
                if (q.binding() != null) {
                    out.putIfAbsent(q.binding(), q);
                }
            }
        }
        return out;
    }

    /** Valor respondido de cada vínculo; respostas vazias ficam de fora. */
    public static Map<AnamnesisBinding, Object> values(AnamnesisModelResponse snapshot,
                                                       Map<UUID, PatientAnamnesisAnswer> answers) {
        Map<AnamnesisBinding, Object> out = new EnumMap<>(AnamnesisBinding.class);
        questions(snapshot).forEach((binding, q) -> {
            PatientAnamnesisAnswer a = answers == null ? null : answers.get(q.id());
            Object v = a == null ? null : a.value();
            if (v == null || (v instanceof String s && s.isBlank())
                    || (v instanceof java.util.Collection<?> c && c.isEmpty())) {
                return;
            }
            out.put(binding, v);
        });
        return out;
    }

    /** Valores do cadastro para as perguntas vinculadas ao cadastro (e ao sexo do exame). */
    public static Map<UUID, Object> prefill(AnamnesisModelResponse snapshot, PersonService.ProfileValues p) {
        Map<UUID, Object> out = new LinkedHashMap<>();
        if (p == null) {
            return out;
        }
        questions(snapshot).forEach((binding, q) -> {
            Object v = switch (binding) {
                case PERSON_NAME -> p.name();
                case PERSON_BIRTH_DATE -> p.birthDate() == null ? null : p.birthDate().toString();
                case PERSON_SEX, TRICHO_SEX -> toSexCode(p.gender());
                case PERSON_CPF -> p.cpf();
                case PERSON_MOBILE -> p.mobile();
                case PERSON_EMAIL -> p.email();
                case PERSON_ADDRESS -> p.address();
                default -> null;
            };
            if (v != null && !(v instanceof String s && s.isBlank())) {
                out.put(q.id(), v);
            }
        });
        return out;
    }

    /** "Feminino"/"FEMININO"/"F" → F; "Masculino"/"M" → M; demais → null. */
    public static String toSexCode(String gender) {
        if (gender == null || gender.isBlank()) {
            return null;
        }
        String g = gender.trim().toUpperCase(Locale.ROOT);
        if (g.startsWith("F")) {
            return "F";
        }
        if (g.startsWith("M")) {
            return "M";
        }
        return null;
    }

    /** F → Feminino; M → Masculino (rótulos usados no cadastro de pessoas). */
    public static String fromSexCode(String code) {
        if (code == null) {
            return null;
        }
        return switch (code.trim().toUpperCase(Locale.ROOT)) {
            case "F" -> "Feminino";
            case "M" -> "Masculino";
            default -> null;
        };
    }
}
