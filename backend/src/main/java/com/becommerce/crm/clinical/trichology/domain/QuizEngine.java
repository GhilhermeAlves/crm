package com.becommerce.crm.clinical.trichology.domain;

import com.becommerce.crm.clinical.trichology.domain.exception.TrichologyValidationException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Quiz tricológico com ramificação determinística (doc v1.2, etapa 2). A
 * definição é a mesma para o link do paciente e para a tela do profissional;
 * respostas de perguntas ocultas pela ramificação são descartadas.
 *
 * <p>Perguntas iniciais; revisão clínica em aberto (doc v1.2, seção 13).</p>
 */
public final class QuizEngine {

    public static final String VERSION = "1";

    public enum Type { SINGLE, MULTI, TEXT, NUMBER, BOOLEAN }

    public record Option(String value, String label) {
    }

    /** {@code showIf}: pergunta só aparece quando a resposta de {@code dependsOn} está em {@code values}. */
    public record Condition(String dependsOn, List<String> values) {
    }

    public record Question(String id, String text, Type type, boolean required, List<Option> options,
                           Condition showIf) {
    }

    private static final List<Question> QUESTIONS = List.of(
            single("sex", "Sexo biológico", true, opt("F", "Feminino"), opt("M", "Masculino")),
            single("age_range", "Faixa de idade", true, opt("UNDER_18", "Menos de 18"), opt("18_29", "18 a 29"),
                    opt("30_44", "30 a 44"), opt("45_59", "45 a 59"), opt("60_PLUS", "60 ou mais")),
            text("chief_complaint", "O que mais incomoda no seu cabelo ou couro cabeludo?", true),
            single("onset", "Há quanto tempo isso começou?", true, opt("LT_3M", "Menos de 3 meses"),
                    opt("3_12M", "De 3 a 12 meses"), opt("GT_1Y", "Mais de 1 ano")),
            single("pattern", "Como é a perda ou o afinamento?", true,
                    opt("DIFFUSE_SHEDDING", "Queda espalhada pela cabeça"),
                    opt("THINNING_TOP", "Afinamento no topo, entradas ou coroa"),
                    opt("PATCHES", "Falhas arredondadas"),
                    opt("SCALP_SYMPTOMS", "Principalmente coceira, ardor ou descamação"),
                    opt("BREAKAGE", "Fios quebrando no comprimento")),
            single("daily_shedding", "Quantos fios você vê caindo por dia (banho, travesseiro, escova)?", true,
                    opt("LT_50", "Menos de 50"), opt("50_100", "Entre 50 e 100"), opt("GT_100", "Mais de 100")),
            multi("recent_triggers", "Nos últimos 6 meses aconteceu algo destes?", true,
                    opt("FEVER_INFECTION", "Febre alta, COVID ou infecção"), opt("SURGERY", "Cirurgia"),
                    opt("WEIGHT_LOSS", "Perda de peso rápida ou dieta restritiva"),
                    opt("SEVERE_STRESS", "Estresse intenso"), opt("NEW_MEDICATION", "Remédio novo"),
                    opt("NONE", "Nenhum")),
            bool("family_history", "Pais ou irmãos têm calvície ou afinamento?", true),
            multi("scalp_symptoms", "Sente algo no couro cabeludo?", true, opt("ITCH", "Coceira"),
                    opt("BURNING", "Ardor"), opt("PAIN", "Dor"), opt("OILINESS", "Oleosidade"),
                    opt("FLAKING", "Descamação"), opt("NONE", "Nada")),
            when("sex", List.of("F"), bool("postpartum", "Teve parto nos últimos 12 meses?", true)),
            when("sex", List.of("F"), bool("irregular_cycles", "Ciclo menstrual irregular?", true)),
            when("sex", List.of("F"), bool("acne_hirsutism", "Acne ou aumento de pelos no rosto/corpo?", true)),
            when("sex", List.of("F"), bool("hormonal_contraceptive", "Usa anticoncepcional hormonal?", false)),
            when("sex", List.of("M"), bool("anabolic_use", "Usa ou usou testosterona/anabolizantes?", true)),
            multi("diet", "Alimentação", false, opt("VEGETARIAN", "Vegetariana"), opt("VEGAN", "Vegana"),
                    opt("LOW_CARB", "Low carb"), opt("NO_RESTRICTION", "Sem restrição")),
            multi("hair_procedures", "Procedimentos no cabelo", false, opt("CHEMICAL", "Química/alisamento"),
                    opt("HEAT", "Chapinha ou secador quase todo dia"),
                    opt("TIGHT_STYLES", "Penteados presos/apertados"), opt("NONE", "Nenhum")),
            text("medications", "Remédios e suplementos em uso", false),
            text("previous_treatments", "Tratamentos capilares que já fez", false));

    private QuizEngine() {
    }

    public static List<Question> questions() {
        return QUESTIONS;
    }

    /**
     * Valida e limpa as respostas: só perguntas visíveis, valores das opções,
     * obrigatórias presentes quando {@code requireComplete}.
     */
    public static Map<String, Object> sanitize(Map<String, Object> answers, boolean requireComplete) {
        Map<String, Object> input = answers == null ? Map.of() : answers;
        Map<String, Object> clean = new LinkedHashMap<>();
        for (Question q : QUESTIONS) {
            if (!visible(q, clean)) {
                continue;
            }
            Object value = input.get(q.id());
            if (value == null || (value instanceof String str && str.isBlank())
                    || (value instanceof Collection<?> c && c.isEmpty())) {
                if (requireComplete && q.required()) {
                    throw new TrichologyValidationException("Responda: " + q.text());
                }
                continue;
            }
            clean.put(q.id(), check(q, value));
        }
        clean.put("_version", VERSION);
        return clean;
    }

    private static boolean visible(Question q, Map<String, Object> answered) {
        if (q.showIf() == null) {
            return true;
        }
        Object dep = answered.get(q.showIf().dependsOn());
        return dep != null && q.showIf().values().contains(String.valueOf(dep));
    }

    private static Object check(Question q, Object value) {
        switch (q.type()) {
            case SINGLE -> {
                String v = String.valueOf(value);
                requireOption(q, v);
                return v;
            }
            case MULTI -> {
                if (!(value instanceof Collection<?> values)) {
                    throw new TrichologyValidationException("Resposta inválida para: " + q.text());
                }
                List<String> list = values.stream().map(String::valueOf).distinct().toList();
                list.forEach(v -> requireOption(q, v));
                return list;
            }
            case BOOLEAN -> {
                if (value instanceof Boolean b) {
                    return b;
                }
                throw new TrichologyValidationException("Responda sim ou não: " + q.text());
            }
            case NUMBER -> {
                if (value instanceof Number n) {
                    return n;
                }
                throw new TrichologyValidationException("Informe um número: " + q.text());
            }
            default -> {
                String v = String.valueOf(value).trim();
                if (v.length() > 2000) {
                    throw new TrichologyValidationException("Resposta longa demais: " + q.text());
                }
                return v;
            }
        }
    }

    private static void requireOption(Question q, String value) {
        if (q.options().stream().noneMatch(o -> o.value().equals(value))) {
            throw new TrichologyValidationException("Opção inválida para: " + q.text());
        }
    }

    private static Option opt(String value, String label) {
        return new Option(value, label);
    }

    private static Question single(String id, String text, boolean required, Option... options) {
        return new Question(id, text, Type.SINGLE, required, List.of(options), null);
    }

    private static Question multi(String id, String text, boolean required, Option... options) {
        return new Question(id, text, Type.MULTI, required, List.of(options), null);
    }

    private static Question text(String id, String text, boolean required) {
        return new Question(id, text, Type.TEXT, required, List.of(), null);
    }

    private static Question bool(String id, String text, boolean required) {
        return new Question(id, text, Type.BOOLEAN, required, List.of(), null);
    }

    private static Question when(String dependsOn, List<String> values, Question q) {
        return new Question(q.id(), q.text(), q.type(), q.required(), q.options(), new Condition(dependsOn, values));
    }
}
