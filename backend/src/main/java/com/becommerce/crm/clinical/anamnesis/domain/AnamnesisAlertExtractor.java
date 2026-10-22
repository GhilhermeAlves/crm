package com.becommerce.crm.clinical.anamnesis.domain;

import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisModelResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisOptionResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisQuestionResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisSectionResponse;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionType;
import com.becommerce.crm.clinical.anamnesis.application.dto.PatientAnamnesisAnswer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Calcula os alertas da anamnese ao concluí-la: uma linha por pergunta marcada
 * com {@code highlight} e respondida "positivamente" (Sim, texto preenchido,
 * número informado ou alguma opção selecionada). O texto do alerta usa a
 * pergunta e, quando houver, o complemento ou as opções escolhidas.
 */
public final class AnamnesisAlertExtractor {

    private AnamnesisAlertExtractor() {
    }

    public static List<String> extract(AnamnesisModelResponse snapshot,
                                       Map<UUID, PatientAnamnesisAnswer> answers) {
        List<String> alerts = new ArrayList<>();
        if (snapshot == null || snapshot.sections() == null || answers == null) {
            return alerts;
        }
        for (AnamnesisSectionResponse section : snapshot.sections()) {
            if (section.questions() == null) {
                continue;
            }
            for (AnamnesisQuestionResponse question : section.questions()) {
                if (!question.highlight()) {
                    continue;
                }
                PatientAnamnesisAnswer answer = answers.get(question.id());
                if (answer == null || !isPositive(question, answer.value())) {
                    continue;
                }
                alerts.add(alertText(question, answer));
            }
        }
        return alerts;
    }

    private static boolean isPositive(AnamnesisQuestionResponse question, Object value) {
        AnamnesisQuestionType type = question.type();
        if (type == AnamnesisQuestionType.YES_NO || type == AnamnesisQuestionType.YES_NO_WITH_TEXT) {
            if (value instanceof Boolean bool) {
                return bool;
            }
            String text = value == null ? "" : value.toString().trim();
            return text.equalsIgnoreCase("YES") || text.equalsIgnoreCase("SIM");
        }
        if (type == AnamnesisQuestionType.SINGLE_SELECT || type == AnamnesisQuestionType.MULTI_SELECT) {
            if (value instanceof Collection<?> collection) {
                return collection.stream().anyMatch(AnamnesisAlertExtractor::hasContent);
            }
            return hasContent(value);
        }
        return hasContent(value);
    }

    private static boolean hasContent(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String text) {
            return !text.isBlank();
        }
        return true;
    }

    private static String alertText(AnamnesisQuestionResponse question, PatientAnamnesisAnswer answer) {
        String complement = answer.complement();
        if (complement != null && !complement.isBlank()) {
            return question.text() + ": " + complement.trim();
        }
        if (question.type() == AnamnesisQuestionType.SINGLE_SELECT
                || question.type() == AnamnesisQuestionType.MULTI_SELECT) {
            String labels = labels(question, answer.value());
            if (!labels.isBlank()) {
                return question.text() + ": " + labels;
            }
        }
        return question.text();
    }

    private static String labels(AnamnesisQuestionResponse question, Object value) {
        Set<String> selected = new LinkedHashSet<>();
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item != null) {
                    selected.add(item.toString());
                }
            }
        } else if (hasContent(value)) {
            selected.add(value.toString());
        }
        List<String> labels = new ArrayList<>();
        for (String raw : selected) {
            labels.add(labelOf(question, raw));
        }
        return String.join(", ", labels);
    }

    private static String labelOf(AnamnesisQuestionResponse question, String value) {
        if (question.options() != null) {
            for (AnamnesisOptionResponse option : question.options()) {
                if (value.equals(option.value()) || value.equals(option.label())) {
                    return option.label();
                }
            }
        }
        return value;
    }
}
