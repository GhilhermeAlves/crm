package com.becommerce.crm.clinical.trichology.application.service;

import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.CopilotView;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.Evolution;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.Hypothesis;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.MeasureDelta;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.SuggestedExam;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.AssessmentImageRepository;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.AssessmentRepository;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.CatalogRepository;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.RegionMeasure;
import com.becommerce.crm.clinical.trichology.domain.AssessmentImage;
import com.becommerce.crm.clinical.trichology.domain.CaptureProtocol;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.ExamRule;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.Marker;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.Settings;
import com.becommerce.crm.clinical.trichology.domain.Finding;
import com.becommerce.crm.clinical.trichology.domain.TrichologyAssessment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Copiloto do exame (doc v1.2, seção 3.3): pendências, hipóteses de padrão para
 * investigar, exames sugeridos pelas regras da clínica e comparação com o exame
 * anterior. Regras determinísticas, sempre citando o que sustenta cada
 * hipótese; usa só achados ACEITOS pelo profissional. Não emite diagnóstico.
 */
@Service
public class ExamCopilotService {

    static final String DISCLAIMER = "Hipóteses para investigar, geradas a partir dos achados aceitos e das "
            + "respostas do quiz. Não substituem a avaliação do profissional.";

    private record Rule(String code, String label, Set<String> findings, Map<String, Set<String>> quiz) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule("ANDROGENETIC_PATTERN", "Padrão androgenético",
                    Set.of("MINIATURIZATION", "DIAMETER_VARIATION", "SINGLE_HAIR_UNITS", "VELLUS_HAIRS"),
                    Map.of("pattern", Set.of("THINNING_TOP"), "family_history", Set.of("true"),
                            "acne_hirsutism", Set.of("true"), "irregular_cycles", Set.of("true"),
                            "anabolic_use", Set.of("true"))),
            new Rule("TELOGEN_EFFLUVIUM", "Eflúvio telógeno",
                    Set.of("UPRIGHT_REGROWING_HAIRS"),
                    Map.of("pattern", Set.of("DIFFUSE_SHEDDING"), "daily_shedding", Set.of("GT_100"),
                            "recent_triggers", Set.of("FEVER_INFECTION", "SURGERY", "WEIGHT_LOSS", "SEVERE_STRESS",
                                    "NEW_MEDICATION"),
                            "postpartum", Set.of("true"), "onset", Set.of("LT_3M", "3_12M"))),
            new Rule("INFLAMMATORY_PATTERN", "Padrão inflamatório",
                    Set.of("PERIFOLLICULAR_ERYTHEMA", "PERIFOLLICULAR_SCALING", "SCALING", "PERIPILAR_SIGN",
                            "LOSS_OF_FOLLICULAR_OPENINGS", "ARBORIZING_VESSELS"),
                    Map.of("scalp_symptoms", Set.of("ITCH", "BURNING", "PAIN", "FLAKING"),
                            "pattern", Set.of("SCALP_SYMPTOMS"))),
            new Rule("PATCHY_LOSS_PATTERN", "Perda em placas",
                    Set.of("EXCLAMATION_MARK_HAIRS", "BLACK_DOTS", "YELLOW_DOTS"),
                    Map.of("pattern", Set.of("PATCHES"))),
            new Rule("HAIR_SHAFT_DAMAGE", "Dano à haste por fatores externos",
                    Set.of("BROKEN_HAIRS"),
                    Map.of("pattern", Set.of("BREAKAGE"), "hair_procedures", Set.of("CHEMICAL", "HEAT",
                            "TIGHT_STYLES"))));

    private final AssessmentService assessmentService;
    private final AssessmentRepository assessments;
    private final AssessmentImageRepository images;
    private final CatalogRepository catalog;

    public ExamCopilotService(AssessmentService assessmentService, AssessmentRepository assessments,
                              AssessmentImageRepository images, CatalogRepository catalog) {
        this.assessmentService = assessmentService;
        this.assessments = assessments;
        this.images = images;
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public CopilotView copilot(UUID companyId, UUID contactId, UUID id) {
        return assessmentService.inTenant(companyId, () -> {
            TrichologyAssessment a = assessmentService.require(companyId, contactId, id);
            List<AssessmentImage> linked = images.listByAssessment(companyId, id);
            List<Hypothesis> hypotheses = hypotheses(a, linked);
            return new CopilotView(pending(a, linked), hypotheses, suggestedExams(companyId, a, hypotheses),
                    evolution(companyId, a, linked), DISCLAIMER);
        });
    }

    List<String> pending(TrichologyAssessment a, List<AssessmentImage> linked) {
        var s = a.state();
        List<String> pending = new ArrayList<>();
        if (s.quizCompletedAt() == null) {
            pending.add("Quiz do paciente não respondido.");
        }
        CaptureProtocol protocol = assessmentService.protocolOf(a);
        if (protocol != null) {
            for (var region : protocol.regions()) {
                long count = linked.stream().filter(i -> i.regionCode().equals(region.code())).count();
                boolean skipped = s.skippedRegions() != null && s.skippedRegions().stream()
                        .anyMatch(r -> r.regionCode().equals(region.code()));
                if (count < region.minImages() && !skipped) {
                    pending.add("Região " + region.label() + ": " + count + " de " + region.minImages() + " fotos.");
                }
            }
        }
        long running = linked.stream().filter(i -> AssessmentImage.AI_PENDING.equals(i.aiStatus())
                || AssessmentImage.AI_RUNNING.equals(i.aiStatus())).count();
        if (running > 0) {
            pending.add(running + " foto(s) ainda em análise pela IA.");
        }
        long failed = linked.stream().filter(i -> AssessmentImage.AI_FAILED.equals(i.aiStatus())).count();
        if (failed > 0) {
            pending.add(failed + " foto(s) com falha na análise da IA (reprocessar ou revisar manualmente).");
        }
        long unreviewed = linked.stream().filter(i -> i.reviewedAt() == null).count();
        if (unreviewed > 0) {
            pending.add(unreviewed + " foto(s) sem revisão dos achados.");
        }
        if (s.pullTest() == null) {
            pending.add("Pull test não registrado.");
        }
        if (s.requestedExams() != null && !s.requestedExams().isEmpty()) {
            Set<String> loaded = new LinkedHashSet<>();
            if (s.labResults() != null) {
                s.labResults().forEach(r -> loaded.add(r.markerCode()));
            }
            long missing = s.requestedExams().stream().filter(c -> !loaded.contains(c)).count();
            if (missing > 0) {
                pending.add(missing + " exame(s) pedido(s) sem resultado lançado.");
            }
        }
        return pending;
    }

    static List<Hypothesis> hypotheses(TrichologyAssessment a, List<AssessmentImage> linked) {
        Map<String, Object> quiz = a.state().quizAnswers() == null ? Map.of() : a.state().quizAnswers();
        Set<String> accepted = new LinkedHashSet<>();
        Map<String, String> acceptedLabels = new LinkedHashMap<>();
        for (AssessmentImage image : linked) {
            if (!image.includeInReport()) {
                continue;
            }
            for (Finding f : image.acceptedFindings()) {
                accepted.add(f.code());
                acceptedLabels.putIfAbsent(f.code(), f.label() == null ? f.code() : f.label());
            }
        }
        List<Hypothesis> result = new ArrayList<>();
        for (Rule rule : RULES) {
            List<String> support = new ArrayList<>();
            int findingHits = 0;
            for (String code : rule.findings()) {
                if (accepted.contains(code)) {
                    support.add("Achado: " + acceptedLabels.get(code));
                    findingHits++;
                }
            }
            int quizHits = 0;
            for (var entry : rule.quiz().entrySet()) {
                Object answer = quiz.get(entry.getKey());
                for (String value : values(answer)) {
                    if (entry.getValue().contains(value)) {
                        support.add("Quiz: " + entry.getKey() + " = " + value);
                        quizHits++;
                    }
                }
            }
            if ("TELOGEN_EFFLUVIUM".equals(rule.code()) && a.state().pullTest() != null
                    && a.state().pullTest().positive()) {
                support.add("Pull test positivo");
                findingHits++;
            }
            if (findingHits + quizHits >= 2) {
                result.add(new Hypothesis(rule.code(), rule.label() + " (investigar)", support));
            }
        }
        return result;
    }

    private List<SuggestedExam> suggestedExams(UUID companyId, TrichologyAssessment a, List<Hypothesis> hypotheses) {
        List<ExamRule> rules = catalog.settings(companyId).map(Settings::examRules).orElse(List.of());
        Map<String, Marker> markers = new LinkedHashMap<>();
        catalog.markers(companyId).stream().filter(Marker::active).forEach(m -> markers.putIfAbsent(m.code(), m));
        Set<String> already = a.state().requestedExams() == null ? Set.of() : Set.copyOf(a.state().requestedExams());
        Map<String, SuggestedExam> out = new LinkedHashMap<>();
        for (Hypothesis h : hypotheses) {
            rules.stream().filter(r -> r.hypothesis().equals(h.code())).forEach(r -> r.markerCodes().forEach(code -> {
                Marker m = markers.get(code);
                if (m != null && !already.contains(code)) {
                    out.putIfAbsent(code, new SuggestedExam(code, m.label(), h.label()));
                }
            }));
        }
        return List.copyOf(out.values());
    }

    Evolution evolution(UUID companyId, TrichologyAssessment a, List<AssessmentImage> linked) {
        var s = a.state();
        return assessments.findPreviousPublished(companyId, s.contactId(), s.createdAt(), s.id())
                .map(prev -> {
                    List<RegionMeasure> before = measures(images.listByAssessment(companyId, prev.getId()));
                    List<RegionMeasure> now = measures(linked);
                    List<MeasureDelta> deltas = new ArrayList<>();
                    for (RegionMeasure current : now) {
                        before.stream().filter(b -> b.regionCode().equals(current.regionCode())
                                        && b.measure().equals(current.measure()))
                                .findFirst()
                                .ifPresent(b -> deltas.add(new MeasureDelta(current.regionCode(), current.measure(),
                                        b.value(), current.value())));
                    }
                    var p = prev.state();
                    return new Evolution(prev.getId(), p.publishedAt(), deltas,
                            p.rpsSummary() == null ? null : p.rpsSummary().overallScore(),
                            s.rpsSummary() == null ? null : s.rpsSummary().overallScore());
                })
                .orElse(null);
    }

    /** Média das medidas confirmadas por região (só fotos incluídas no relatório). */
    static List<RegionMeasure> measures(List<AssessmentImage> linked) {
        Map<String, List<BigDecimal>> grouped = new LinkedHashMap<>();
        for (AssessmentImage image : linked) {
            if (!image.includeInReport()) {
                continue;
            }
            image.confirmedMeasures().forEach((k, v) ->
                    grouped.computeIfAbsent(image.regionCode() + "|" + k, x -> new ArrayList<>()).add(v));
        }
        List<RegionMeasure> out = new ArrayList<>();
        grouped.forEach((key, values) -> {
            String[] parts = key.split("\\|", 2);
            BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            out.add(new RegionMeasure(parts[0], parts[1],
                    sum.divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP)));
        });
        return out;
    }

    private static List<String> values(Object answer) {
        if (answer == null) {
            return List.of();
        }
        if (answer instanceof Collection<?> c) {
            return c.stream().map(String::valueOf).toList();
        }
        return List.of(String.valueOf(answer));
    }
}
