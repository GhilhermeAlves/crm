package com.becommerce.crm.clinical.trichology.domain;

import com.becommerce.crm.clinical.trichology.domain.AssessmentData.LabResult;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.MarkerScore;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.RpsSummary;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.SystemScore;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.Marker;
import com.becommerce.crm.clinical.trichology.domain.exception.TrichologyValidationException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * RPS (pontuação por sistema) calculado só no código — a IA nunca produz esses
 * números. Critério inicial, editável pela clínica nas faixas e quartis-alvo:
 * <ul>
 *   <li>fora da faixa de referência: nota 0 (LOW/HIGH);</li>
 *   <li>dentro da faixa: quartil 1–4; nota 100 no quartil-alvo, −25 por quartil de distância;</li>
 *   <li>sem quartil-alvo: dentro da faixa vale 100;</li>
 *   <li>nota do sistema = média dos marcadores; RPS = média dos sistemas.</li>
 * </ul>
 */
public final class RpsCalculator {

    private RpsCalculator() {
    }

    public static RpsSummary calculate(List<LabResult> results, List<Marker> markers, String sex) {
        List<MarkerScore> scores = new ArrayList<>();
        List<String> unknown = new ArrayList<>();
        for (LabResult result : results == null ? List.<LabResult>of() : results) {
            if (result.value() == null) {
                throw new TrichologyValidationException("Informe o valor de " + result.markerCode() + ".");
            }
            Optional<Marker> marker = find(markers, result.markerCode(), sex);
            if (marker.isEmpty()) {
                unknown.add(result.markerCode());
                continue;
            }
            scores.add(score(marker.get(), result.value()));
        }
        Map<String, List<Integer>> bySystem = new LinkedHashMap<>();
        scores.forEach(s -> bySystem.computeIfAbsent(s.system(), k -> new ArrayList<>()).add(s.score()));
        List<SystemScore> systems = bySystem.entrySet().stream()
                .map(e -> new SystemScore(e.getKey(), mean(e.getValue()), e.getValue().size()))
                .toList();
        int overall = mean(systems.stream().map(SystemScore::score).toList());
        return new RpsSummary(overall, systems, scores, unknown);
    }

    static MarkerScore score(Marker m, BigDecimal value) {
        BigDecimal min = m.referenceMin();
        BigDecimal max = m.referenceMax();
        if (min == null || max == null) {
            return new MarkerScore(m.code(), m.label(), m.system(), value, m.unit(), min, max, null,
                    m.targetQuartile(), "NO_RANGE", 100);
        }
        if (value.compareTo(min) < 0) {
            return new MarkerScore(m.code(), m.label(), m.system(), value, m.unit(), min, max, null,
                    m.targetQuartile(), "LOW", 0);
        }
        if (value.compareTo(max) > 0) {
            return new MarkerScore(m.code(), m.label(), m.system(), value, m.unit(), min, max, null,
                    m.targetQuartile(), "HIGH", 0);
        }
        BigDecimal fraction = value.subtract(min).divide(max.subtract(min), MathContext.DECIMAL64);
        int quartile = Math.min(4, 1 + fraction.multiply(BigDecimal.valueOf(4)).intValue());
        int score = m.targetQuartile() == null ? 100 : 100 - 25 * Math.abs(quartile - m.targetQuartile());
        return new MarkerScore(m.code(), m.label(), m.system(), value, m.unit(), min, max, quartile,
                m.targetQuartile(), "IN_RANGE", score);
    }

    private static Optional<Marker> find(List<Marker> markers, String code, String sex) {
        Optional<Marker> specific = markers.stream()
                .filter(m -> m.active() && m.code().equals(code) && m.sex() != null && m.sex().equals(sex))
                .findFirst();
        return specific.isPresent() ? specific : markers.stream()
                .filter(m -> m.active() && m.code().equals(code) && m.sex() == null)
                .findFirst();
    }

    private static int mean(List<Integer> values) {
        if (values.isEmpty()) {
            return 0;
        }
        return (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0));
    }
}
