package com.becommerce.crm.clinical.trichology.application.service;

import com.becommerce.crm.automation.ai.application.port.output.AiMediaProvider;
import com.becommerce.crm.automation.ai.application.port.output.AiMediaProvider.VisionJsonResult;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.AiRun;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.AiRunRepository;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.AssessmentImageRepository;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.AssessmentRepository;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.CatalogRepository;
import com.becommerce.crm.clinical.trichology.application.service.AssessmentService.ImageLinkedEvent;
import com.becommerce.crm.clinical.trichology.domain.AssessmentImage;
import com.becommerce.crm.clinical.trichology.domain.CaptureProtocol;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.FindingCatalogItem;
import com.becommerce.crm.clinical.trichology.domain.Finding;
import com.becommerce.crm.clinical.trichology.domain.GuardrailValidator;
import com.becommerce.crm.clinical.trichology.domain.TrichologyAssessment;
import com.becommerce.crm.clinical.trichoscopy.application.port.out.TrichoscopyRepository;
import com.becommerce.crm.clinical.trichoscopy.domain.PatientTrichoscopyImage;
import com.becommerce.crm.masterdata.storage.application.dto.StorageDownload;
import com.becommerce.crm.masterdata.storage.application.port.input.StorageUseCase;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Pré-análise de cada foto pelo modelo de visão (doc v1.2, seção 3.2). Roda
 * depois do commit, fora da requisição; a foto continua salva se a IA falhar
 * ({@code ai_status = FAILED}) e pode ser reprocessada. Ao provedor vão só a
 * imagem e contexto mínimo — nunca nome, CPF ou outro identificador.
 */
@Service
public class ImageAnalysisService {

    static final String PROMPT_VERSION = "img-1";
    private static final Logger log = LoggerFactory.getLogger(ImageAnalysisService.class);
    private static final int MAX_TOKENS = 1200;

    private final AssessmentRepository assessments;
    private final AssessmentImageRepository images;
    private final CatalogRepository catalog;
    private final AiRunRepository aiRuns;
    private final TrichoscopyRepository photos;
    private final StorageUseCase storage;
    private final ObjectProvider<AiMediaProvider> provider;
    private final TrichologyAccess access;
    private final ObjectMapper mapper;
    private final TransactionTemplate tx;
    private final Clock clock;

    public ImageAnalysisService(AssessmentRepository assessments, AssessmentImageRepository images,
                                CatalogRepository catalog, AiRunRepository aiRuns, TrichoscopyRepository photos,
                                StorageUseCase storage, ObjectProvider<AiMediaProvider> provider,
                                TrichologyAccess access, ObjectMapper mapper, PlatformTransactionManager txManager,
                                Clock clock) {
        this.assessments = assessments;
        this.images = images;
        this.catalog = catalog;
        this.aiRuns = aiRuns;
        this.photos = photos;
        this.storage = storage;
        this.provider = provider;
        this.access = access;
        this.mapper = mapper;
        this.tx = new TransactionTemplate(txManager);
        this.clock = clock;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onImageLinked(ImageLinkedEvent event) {
        try {
            analyze(event.companyId(), event.contactId(), event.assessmentId(), event.assessmentImageId(),
                    event.userId());
        } catch (RuntimeException e) {
            log.warn("Pré-análise tricológica falhou para a imagem {}: {}", event.assessmentImageId(),
                    e.getClass().getSimpleName());
        }
    }

    void analyze(UUID companyId, UUID contactId, UUID assessmentId, UUID assessmentImageId, UUID userId) {
        AiMediaProvider ai = provider.getIfAvailable();
        Context ctx = inTenant(companyId, () -> tx.execute(status -> prepare(companyId, contactId, assessmentId,
                assessmentImageId, ai != null)));
        if (ctx == null || ai == null) {
            return;
        }
        String sha = sha256(ctx.image().data());
        long started = System.nanoTime();
        VisionJsonResult result = null;
        RuntimeException failure = null;
        for (int attempt = 0; attempt <= access.maxRetries() && result == null; attempt++) {
            try {
                result = ai.analyzeImageJson(ctx.image().data(), ctx.image().contentType(), systemPrompt(ctx),
                        userPrompt(ctx), access.visionModel(), MAX_TOKENS);
            } catch (RuntimeException e) {
                failure = e;
            }
        }
        int latency = (int) ((System.nanoTime() - started) / 1_000_000);
        VisionJsonResult done = result;
        RuntimeException error = failure;
        inTenant(companyId, () -> tx.execute(status -> {
            AssessmentImage image = images.findImage(companyId, assessmentImageId).orElseThrow();
            Instant now = clock.instant();
            if (done == null) {
                images.save(image.withAiStatus(AssessmentImage.AI_FAILED, now));
                aiRuns.save(run(ctx, sha, null, "FAILED", error == null ? "NO_RESPONSE"
                        : error.getClass().getSimpleName(), latency, userId, now));
                return null;
            }
            Parsed parsed = parse(done.json(), ctx.catalogCodes(), ctx.labels());
            images.save(image.withAnalysis(parsed.quality(), parsed.findings(), parsed.estimates(), parsed.notes(),
                    now));
            aiRuns.save(run(ctx, sha, done, parsed.violations().isEmpty() ? "OK" : "BLOCKED",
                    parsed.violations().isEmpty() ? null : "GUARDRAIL", latency, userId, now));
            return null;
        }));
    }

    private record Context(UUID companyId, UUID assessmentId, UUID assessmentImageId, StorageDownload image,
                           String regionLabel, String magnification, Map<String, Object> quiz,
                           String chiefComplaint, Set<String> catalogCodes, Map<String, String> labels) {
    }

    record Parsed(String quality, List<Finding> findings, Map<String, BigDecimal> estimates, String notes,
                  List<String> violations) {
    }

    private Context prepare(UUID companyId, UUID contactId, UUID assessmentId, UUID assessmentImageId,
                            boolean providerAvailable) {
        AssessmentImage image = images.findInAssessment(companyId, assessmentId, assessmentImageId).orElse(null);
        TrichologyAssessment assessment = assessments.find(companyId, contactId, assessmentId).orElse(null);
        if (image == null || assessment == null || assessment.getStatus().isFinal()
                || !AssessmentImage.AI_PENDING.equals(image.aiStatus())) {
            return null;
        }
        Instant now = clock.instant();
        if (!providerAvailable || !access.aiEnabled()) {
            images.save(image.withAiStatus(AssessmentImage.AI_SKIPPED, now));
            return null;
        }
        PatientTrichoscopyImage photo = photos.findByIdAndCompanyIdAndContactId(image.imageId(), companyId, contactId)
                .orElse(null);
        if (photo == null) {
            images.save(image.withAiStatus(AssessmentImage.AI_FAILED, now));
            return null;
        }
        images.save(image.withAiStatus(AssessmentImage.AI_RUNNING, now));
        StorageDownload download = storage.download(companyId, photo.getStorageObjectId());
        TenantContext.setCompanyId(companyId); // download() limpa o contexto no fim
        String region = catalog.protocol(companyId, assessment.state().protocolId())
                .flatMap(p -> p.region(image.regionCode())).map(CaptureProtocol.Region::label)
                .orElse(image.regionCode());
        List<FindingCatalogItem> items = catalog.findings(companyId).stream().filter(FindingCatalogItem::active)
                .toList();
        Map<String, String> labels = new LinkedHashMap<>();
        items.forEach(f -> labels.put(f.code(), f.label()));
        return new Context(companyId, assessmentId, assessmentImageId, download, region, image.magnification(),
                clinicalAnswers(assessment), assessment.state().chiefComplaint(), labels.keySet(), labels);
    }

    private org.springframework.beans.factory.ObjectProvider<AssessmentService> assessmentService;

    /** Lazy: AssessmentService publica o evento que este serviço consome. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setAssessmentService(org.springframework.beans.factory.ObjectProvider<AssessmentService> provider) {
        this.assessmentService = provider;
    }

    private Map<String, Object> clinicalAnswers(TrichologyAssessment assessment) {
        AssessmentService core = assessmentService == null ? null : assessmentService.getIfAvailable();
        if (core != null) {
            return core.clinicalAnswers(assessment);
        }
        return assessment.state().quizAnswers();
    }

    private String systemPrompt(Context ctx) {
        String catalogList = ctx.labels().entrySet().stream()
                .map(e -> "- " + e.getKey() + ": " + e.getValue()).collect(Collectors.joining("\n"));
        return """
                Você apoia um profissional de tricologia na leitura de uma imagem de tricoscopia.
                Descreva apenas o que é visível na imagem. Não dê diagnóstico, não sugira tratamento.
                Use SOMENTE os códigos de achado desta lista (qualquer outro será descartado):
                %s

                Responda apenas com um objeto JSON neste formato:
                {"imageQuality":"OK|BLURRED|DARK|GLARE",
                 "findings":[{"code":"CODIGO","severity":"MILD|MODERATE|SEVERE","confidence":0.0,
                              "evidence":"o que aparece na imagem"}],
                 "estimates":{"hairDensityPerCm2":null,"thinHairPercent":null,
                              "singleHairFollicularUnitsPercent":null,"meanShaftDiameterMicrons":null},
                 "notes":"texto curto em português"}
                Regras: se a imagem estiver desfocada, escura ou com reflexo, informe a qualidade e
                devolva findings vazio. Estimativas só quando a magnificação permitir; senão null.
                confidence de 0 a 1.""".formatted(catalogList);
    }

    private String userPrompt(Context ctx) {
        Map<String, Object> quiz = ctx.quiz() == null ? Map.of() : ctx.quiz();
        return "Região: " + ctx.regionLabel()
                + "\nMagnificação: " + (ctx.magnification() == null ? "não informada" : ctx.magnification())
                + "\nSexo: " + quiz.getOrDefault("sex", "não informado")
                + "\nFaixa de idade: " + quiz.getOrDefault("age_range", "não informada")
                + "\nPadrão relatado: " + quiz.getOrDefault("pattern", "não informado")
                + "\nQueixa principal: " + (ctx.chiefComplaint() == null ? "não informada"
                : truncate(ctx.chiefComplaint(), 300));
    }

    /** Converte a resposta e aplica o guardrail: só catálogo, sem achados em imagem ruim, estimativas plausíveis. */
    Parsed parse(String json, Set<String> catalogCodes, Map<String, String> labels) {
        List<String> violations = new ArrayList<>();
        JsonNode root;
        try {
            root = mapper.readTree(json == null || json.isBlank() ? "{}" : json);
        } catch (Exception e) {
            return new Parsed(null, List.of(), Map.of(), null, List.of("Resposta da IA não é JSON válido."));
        }
        String quality = root.path("imageQuality").asText("OK");
        if (!Set.of("OK", "BLURRED", "DARK", "GLARE").contains(quality)) {
            quality = "OK";
        }
        List<Finding> proposed = new ArrayList<>();
        for (JsonNode f : root.path("findings")) {
            Double confidence = f.hasNonNull("confidence") ? f.get("confidence").asDouble() : null;
            String severity = f.path("severity").asText(null);
            proposed.add(new Finding(f.path("code").asText(null), null, severity, confidence,
                    truncate(f.path("evidence").asText(null), 500), Finding.SOURCE_AI));
        }
        violations.addAll(GuardrailValidator.validateFindings(proposed, catalogCodes, quality));
        List<Finding> findings = "OK".equals(quality) ? proposed.stream()
                .filter(f -> catalogCodes.contains(f.code()))
                .filter(f -> f.confidence() == null || (f.confidence() >= 0 && f.confidence() <= 1))
                .map(f -> new Finding(f.code(), labels.get(f.code()),
                        Set.of("MILD", "MODERATE", "SEVERE").contains(f.severity()) ? f.severity() : null,
                        f.confidence(), f.evidence(), Finding.SOURCE_AI))
                .toList() : List.of();
        Map<String, BigDecimal> estimates = new LinkedHashMap<>();
        if ("OK".equals(quality)) {
            root.path("estimates").fields().forEachRemaining(e -> {
                if (e.getValue().isNumber()) {
                    BigDecimal value = e.getValue().decimalValue();
                    if (GuardrailValidator.plausible(e.getKey(), value)) {
                        estimates.put(e.getKey(), value);
                    } else {
                        violations.add("Estimativa fora da faixa descartada: " + e.getKey());
                    }
                }
            });
        }
        return new Parsed(quality, findings, estimates, truncate(root.path("notes").asText(null), 1000),
                violations);
    }

    private AiRun run(Context ctx, String sha, VisionJsonResult result, String status, String error, int latency,
                      UUID userId, Instant now) {
        return new AiRun(UUID.randomUUID(), ctx.companyId(), ctx.assessmentId(), ctx.assessmentImageId(),
                "IMAGE_ANALYSIS", result == null ? access.visionModel() : result.model(), PROMPT_VERSION, sha,
                result == null ? null : result.inputTokens(), result == null ? null : result.outputTokens(), status,
                error, latency, userId, now);
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private <T> T inTenant(UUID companyId, Supplier<T> work) {
        try {
            TenantContext.setCompanyId(companyId);
            return work.get();
        } finally {
            TenantContext.clear();
        }
    }
}
