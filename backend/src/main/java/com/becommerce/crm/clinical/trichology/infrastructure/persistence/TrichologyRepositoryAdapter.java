package com.becommerce.crm.clinical.trichology.infrastructure.persistence;

import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.AiRun;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.AiRunRepository;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.AssessmentImageRepository;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.AssessmentRepository;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.CatalogRepository;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.LabResult;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.PullTest;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.ReportPayload;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.RpsSummary;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.SkippedRegion;
import com.becommerce.crm.clinical.trichology.domain.AssessmentImage;
import com.becommerce.crm.clinical.trichology.domain.AssessmentStatus;
import com.becommerce.crm.clinical.trichology.domain.CaptureProtocol;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.ExamRule;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.FindingCatalogItem;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.Marker;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.Settings;
import com.becommerce.crm.clinical.trichology.domain.Finding;
import com.becommerce.crm.clinical.trichology.domain.TrichologyAssessment;
import com.becommerce.crm.clinical.trichology.infrastructure.persistence.TrichologyJpaEntities.AiRunEntity;
import com.becommerce.crm.clinical.trichology.infrastructure.persistence.TrichologyJpaEntities.AssessmentEntity;
import com.becommerce.crm.clinical.trichology.infrastructure.persistence.TrichologyJpaEntities.AssessmentImageEntity;
import com.becommerce.crm.clinical.trichology.infrastructure.persistence.TrichologyJpaEntities.FindingEntity;
import com.becommerce.crm.clinical.trichology.infrastructure.persistence.TrichologyJpaEntities.MarkerEntity;
import com.becommerce.crm.clinical.trichology.infrastructure.persistence.TrichologyJpaEntities.ProtocolEntity;
import com.becommerce.crm.clinical.trichology.infrastructure.persistence.TrichologyJpaEntities.SettingsEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TrichologyRepositoryAdapter
        implements AssessmentRepository, AssessmentImageRepository, CatalogRepository, AiRunRepository {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() { };
    private static final TypeReference<Map<String, BigDecimal>> MEASURES = new TypeReference<>() { };
    private static final TypeReference<List<Finding>> FINDINGS = new TypeReference<>() { };
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() { };
    private static final TypeReference<List<LabResult>> LABS = new TypeReference<>() { };
    private static final TypeReference<List<SkippedRegion>> SKIPPED = new TypeReference<>() { };
    private static final TypeReference<List<CaptureProtocol.Region>> REGIONS = new TypeReference<>() { };
    private static final TypeReference<List<ExamRule>> RULES = new TypeReference<>() { };

    private final SpringDataTrichologyAssessments assessments;
    private final SpringDataTrichologyImages images;
    private final SpringDataTrichologyProtocols protocols;
    private final SpringDataTrichologyFindings findings;
    private final SpringDataTrichologyMarkers markers;
    private final SpringDataTrichologySettings settings;
    private final SpringDataTrichologyAiRuns aiRuns;
    private final ObjectMapper mapper;

    public TrichologyRepositoryAdapter(SpringDataTrichologyAssessments assessments, SpringDataTrichologyImages images,
                                       SpringDataTrichologyProtocols protocols, SpringDataTrichologyFindings findings,
                                       SpringDataTrichologyMarkers markers, SpringDataTrichologySettings settings,
                                       SpringDataTrichologyAiRuns aiRuns, ObjectMapper mapper) {
        this.assessments = assessments;
        this.images = images;
        this.protocols = protocols;
        this.findings = findings;
        this.markers = markers;
        this.settings = settings;
        this.aiRuns = aiRuns;
        this.mapper = mapper;
    }

    // ------------------------------------------------------------ exames

    @Override
    public TrichologyAssessment save(TrichologyAssessment assessment) {
        return toDomain(assessments.save(toEntity(assessment.state())));
    }

    @Override
    public Optional<TrichologyAssessment> find(UUID companyId, UUID contactId, UUID id) {
        return assessments.findByIdAndCompanyIdAndContactId(id, companyId, contactId).map(this::toDomain);
    }

    @Override
    public List<TrichologyAssessment> listByContact(UUID companyId, UUID contactId) {
        return assessments.findByCompanyIdAndContactIdOrderByCreatedAtDesc(companyId, contactId).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public Optional<TrichologyAssessment> findByQuizTokenHash(String tokenHash) {
        return assessments.findByQuizFormTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public Optional<TrichologyAssessment> findPreviousPublished(UUID companyId, UUID contactId, Instant before,
                                                                UUID excludeId) {
        return assessments.findPublishedBefore(companyId, contactId, before, excludeId).stream().findFirst()
                .map(this::toDomain);
    }

    @Override
    public boolean verificationCodeExists(String code) {
        return assessments.existsByVerificationCode(code);
    }

    private AssessmentEntity toEntity(TrichologyAssessment.State s) {
        AssessmentEntity e = new AssessmentEntity();
        e.id = s.id();
        e.companyId = s.companyId();
        e.contactId = s.contactId();
        e.professionalId = s.professionalId();
        e.protocolId = s.protocolId();
        e.status = s.status().name();
        e.consentAt = s.consentAt();
        e.consentBy = s.consentBy();
        e.chiefComplaint = s.chiefComplaint();
        e.quizAnswers = write(s.quizAnswers());
        e.quizCompletedAt = s.quizCompletedAt();
        e.quizFormTokenHash = s.quizFormTokenHash();
        e.quizFormExpiresAt = s.quizFormExpiresAt();
        e.clinicalNotes = s.clinicalNotes();
        e.pullTest = write(s.pullTest());
        e.trichoscopyPerformed = s.trichoscopyPerformed();
        e.skippedRegions = write(s.skippedRegions() == null ? List.of() : s.skippedRegions());
        e.requestedExams = write(s.requestedExams());
        e.labResults = write(s.labResults());
        e.labCollectedOn = s.labCollectedOn();
        e.rpsSummary = write(s.rpsSummary());
        e.aiDraftPayload = write(s.aiDraftPayload());
        e.finalReportPayload = write(s.finalReportPayload());
        e.reportVersion = s.reportVersion();
        e.supersedesId = s.supersedesId();
        e.patientDocumentId = s.patientDocumentId();
        e.verificationCode = s.verificationCode();
        e.pdfSha256 = s.pdfSha256();
        e.pdfStorageObjectId = s.pdfStorageObjectId();
        e.professionalName = s.professionalName();
        e.professionalRegistry = s.professionalRegistry();
        e.reviewedBy = s.reviewedBy();
        e.reviewedAt = s.reviewedAt();
        e.publishedAt = s.publishedAt();
        e.cancelledAt = s.cancelledAt();
        e.cancelReason = s.cancelReason();
        e.createdBy = s.createdBy();
        e.createdAt = s.createdAt();
        e.updatedAt = s.updatedAt();
        return e;
    }

    private TrichologyAssessment toDomain(AssessmentEntity e) {
        return TrichologyAssessment.reconstitute(new TrichologyAssessment.State(e.id, e.companyId, e.contactId,
                e.professionalId, e.protocolId, AssessmentStatus.valueOf(e.status), e.consentAt, e.consentBy,
                e.chiefComplaint, read(e.quizAnswers, MAP), e.quizCompletedAt, e.quizFormTokenHash,
                e.quizFormExpiresAt, e.clinicalNotes, read(e.pullTest, PullTest.class), e.trichoscopyPerformed,
                orEmpty(read(e.skippedRegions, SKIPPED)), read(e.requestedExams, STRINGS), read(e.labResults, LABS),
                e.labCollectedOn, read(e.rpsSummary, RpsSummary.class), read(e.aiDraftPayload, ReportPayload.class),
                read(e.finalReportPayload, ReportPayload.class), e.reportVersion, e.supersedesId,
                e.patientDocumentId, e.verificationCode, e.pdfSha256, e.pdfStorageObjectId, e.professionalName,
                e.professionalRegistry, e.reviewedBy, e.reviewedAt, e.publishedAt, e.cancelledAt, e.cancelReason,
                e.createdBy, e.createdAt, e.updatedAt));
    }

    // ------------------------------------------------------------ imagens

    @Override
    public AssessmentImage save(AssessmentImage i) {
        AssessmentImageEntity e = new AssessmentImageEntity();
        e.id = i.id();
        e.companyId = i.companyId();
        e.assessmentId = i.assessmentId();
        e.imageId = i.imageId();
        e.regionCode = i.regionCode();
        e.magnification = i.magnification();
        e.qualityStatus = i.qualityStatus();
        e.aiStatus = i.aiStatus();
        e.aiImageQuality = i.aiImageQuality();
        e.aiFindings = write(i.aiFindings());
        e.aiEstimates = write(i.aiEstimates());
        e.aiNotes = i.aiNotes();
        e.acceptedFindings = write(orEmpty(i.acceptedFindings()));
        e.confirmedMeasures = write(i.confirmedMeasures() == null ? Map.of() : i.confirmedMeasures());
        e.includeInReport = i.includeInReport();
        e.reviewedBy = i.reviewedBy();
        e.reviewedAt = i.reviewedAt();
        e.createdAt = i.createdAt();
        e.updatedAt = i.updatedAt();
        return toDomain(images.save(e));
    }

    @Override
    public Optional<AssessmentImage> findInAssessment(UUID companyId, UUID assessmentId, UUID id) {
        return images.findByIdAndCompanyIdAndAssessmentId(id, companyId, assessmentId).map(this::toDomain);
    }

    @Override
    public Optional<AssessmentImage> findImage(UUID companyId, UUID id) {
        return images.findByIdAndCompanyId(id, companyId).map(this::toDomain);
    }

    @Override
    public List<AssessmentImage> listByAssessment(UUID companyId, UUID assessmentId) {
        return images.findByCompanyIdAndAssessmentIdOrderByCreatedAt(companyId, assessmentId).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public boolean isLinked(UUID companyId, UUID imageId) {
        return images.existsByCompanyIdAndImageId(companyId, imageId);
    }

    @Override
    public boolean isLinkedToPublished(UUID companyId, UUID imageId) {
        return images.linkedToPublished(companyId, imageId);
    }

    private AssessmentImage toDomain(AssessmentImageEntity e) {
        return new AssessmentImage(e.id, e.companyId, e.assessmentId, e.imageId, e.regionCode, e.magnification,
                e.qualityStatus, e.aiStatus, e.aiImageQuality, orEmpty(read(e.aiFindings, FINDINGS)),
                orEmptyMap(read(e.aiEstimates, MEASURES)), e.aiNotes, orEmpty(read(e.acceptedFindings, FINDINGS)),
                orEmptyMap(read(e.confirmedMeasures, MEASURES)), e.includeInReport, e.reviewedBy, e.reviewedAt,
                e.createdAt, e.updatedAt);
    }

    // ------------------------------------------------------------ catálogos

    @Override
    public List<CaptureProtocol> protocols(UUID companyId) {
        return protocols.findByCompanyIdOrderByName(companyId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<CaptureProtocol> protocol(UUID companyId, UUID id) {
        return protocols.findByIdAndCompanyId(id, companyId).map(this::toDomain);
    }

    @Override
    public CaptureProtocol saveProtocol(CaptureProtocol p) {
        Instant now = Instant.now();
        ProtocolEntity e = protocols.findById(p.id()).orElseGet(() -> {
            ProtocolEntity n = new ProtocolEntity();
            n.createdAt = now;
            return n;
        });
        e.id = p.id();
        e.companyId = p.companyId();
        e.name = p.name();
        e.regions = write(p.regions());
        e.isDefault = p.isDefault();
        e.active = p.active();
        e.updatedAt = now;
        return toDomain(protocols.save(e));
    }

    private CaptureProtocol toDomain(ProtocolEntity e) {
        return new CaptureProtocol(e.id, e.companyId, e.name, read(e.regions, REGIONS), e.isDefault, e.active);
    }

    @Override
    public List<FindingCatalogItem> findings(UUID companyId) {
        return findings.findByCompanyIdOrderByLabel(companyId).stream()
                .map(e -> new FindingCatalogItem(e.id, e.companyId, e.code, e.label, e.description, e.active))
                .toList();
    }

    @Override
    public FindingCatalogItem saveFinding(FindingCatalogItem item) {
        Instant now = Instant.now();
        FindingEntity e = findings.findById(item.id()).orElseGet(() -> {
            FindingEntity n = new FindingEntity();
            n.createdAt = now;
            return n;
        });
        e.id = item.id();
        e.companyId = item.companyId();
        e.code = item.code();
        e.label = item.label();
        e.description = item.description();
        e.active = item.active();
        e.updatedAt = now;
        findings.save(e);
        return item;
    }

    @Override
    public List<Marker> markers(UUID companyId) {
        return markers.findByCompanyIdOrderBySystemAscLabelAsc(companyId).stream()
                .map(e -> new Marker(e.id, e.companyId, e.code, e.label, e.unit, e.system, e.sex, e.referenceMin,
                        e.referenceMax, e.targetQuartile, e.active))
                .toList();
    }

    @Override
    public Marker saveMarker(Marker m) {
        Instant now = Instant.now();
        MarkerEntity e = markers.findById(m.id()).orElseGet(() -> {
            MarkerEntity n = new MarkerEntity();
            n.createdAt = now;
            return n;
        });
        e.id = m.id();
        e.companyId = m.companyId();
        e.code = m.code();
        e.label = m.label();
        e.unit = m.unit();
        e.system = m.system();
        e.sex = m.sex();
        e.referenceMin = m.referenceMin();
        e.referenceMax = m.referenceMax();
        e.targetQuartile = m.targetQuartile();
        e.active = m.active();
        e.updatedAt = now;
        markers.save(e);
        return m;
    }

    @Override
    public Optional<Settings> settings(UUID companyId) {
        return settings.findById(companyId).map(e -> new Settings(e.companyId, e.minConfidence,
                Arrays.stream(e.reportHorizonsMonths.split(",")).map(String::trim).filter(v -> !v.isEmpty())
                        .map(Integer::valueOf).toList(),
                orEmpty(read(e.examRules, RULES))));
    }

    @Override
    public Settings saveSettings(Settings s) {
        Instant now = Instant.now();
        SettingsEntity e = settings.findById(s.companyId()).orElseGet(() -> {
            SettingsEntity n = new SettingsEntity();
            n.createdAt = now;
            return n;
        });
        e.companyId = s.companyId();
        e.minConfidence = s.minConfidence();
        e.reportHorizonsMonths = String.join(",", s.reportHorizonsMonths().stream().map(String::valueOf).toList());
        e.examRules = write(s.examRules());
        e.updatedAt = now;
        settings.save(e);
        return s;
    }

    // ------------------------------------------------------------ IA

    @Override
    public void save(AiRun run) {
        AiRunEntity e = new AiRunEntity();
        e.id = run.id();
        e.companyId = run.companyId();
        e.assessmentId = run.assessmentId();
        e.assessmentImageId = run.assessmentImageId();
        e.stage = run.stage();
        e.model = run.model();
        e.promptVersion = run.promptVersion();
        e.imageSha256 = run.imageSha256();
        e.inputTokens = run.inputTokens();
        e.outputTokens = run.outputTokens();
        e.status = run.status();
        e.errorCode = run.errorCode();
        e.latencyMs = run.latencyMs();
        e.createdBy = run.createdBy();
        e.createdAt = run.createdAt();
        aiRuns.save(e);
    }

    @Override
    public List<AiRun> runsOf(UUID companyId, UUID assessmentId) {
        return aiRuns.findByCompanyIdAndAssessmentIdOrderByCreatedAt(companyId, assessmentId).stream()
                .map(e -> new AiRun(e.id, e.companyId, e.assessmentId, e.assessmentImageId, e.stage, e.model,
                        e.promptVersion, e.imageSha256, e.inputTokens, e.outputTokens, e.status, e.errorCode,
                        e.latencyMs, e.createdBy, e.createdAt))
                .toList();
    }

    // ------------------------------------------------------------ JSON

    private String write(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar dado de tricologia", e);
        }
    }

    private <T> T read(String json, TypeReference<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao ler dado de tricologia", e);
        }
    }

    private <T> T read(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao ler dado de tricologia", e);
        }
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static <K, V> Map<K, V> orEmptyMap(Map<K, V> map) {
        return map == null ? Map.of() : map;
    }
}
