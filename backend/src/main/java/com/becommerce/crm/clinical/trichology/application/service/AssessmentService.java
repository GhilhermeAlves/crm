package com.becommerce.crm.clinical.trichology.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.AssessmentDetail;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.AssessmentSummary;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.CancelRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.ClinicalRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.ExamsRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.FindingView;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.FinishImagingRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.ImageView;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.LabsRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.LinkImageRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.OpenAssessmentRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.RegionProgress;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.ReviewImageRequest;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.AssessmentImageRepository;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.AssessmentRepository;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.CatalogRepository;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.LabResult;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.RpsSummary;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.SkippedRegion;
import com.becommerce.crm.clinical.trichology.domain.AssessmentImage;
import com.becommerce.crm.clinical.trichology.domain.CaptureProtocol;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.FindingCatalogItem;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.Settings;
import com.becommerce.crm.clinical.trichology.domain.Finding;
import com.becommerce.crm.clinical.trichology.domain.RpsCalculator;
import com.becommerce.crm.clinical.trichology.domain.TrichologyAssessment;
import com.becommerce.crm.clinical.trichology.domain.exception.TrichologyNotFoundException;
import com.becommerce.crm.clinical.trichology.domain.exception.TrichologyValidationException;
import com.becommerce.crm.clinical.trichoscopy.application.dto.TrichoscopyImageResponse;
import com.becommerce.crm.clinical.trichoscopy.application.port.in.TrichoscopyUseCase;
import com.becommerce.crm.clinical.trichoscopy.application.port.out.TrichoscopyRepository;
import com.becommerce.crm.clinical.trichoscopy.domain.PatientTrichoscopyImage;
import com.becommerce.crm.masterdata.contact.note.application.service.PatientTimelineRecorder;
import com.becommerce.crm.masterdata.contact.registration.domain.RegistrationLink;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Exame tricológico: abertura, quiz, captura por região, exame clínico e
 * laboratório (doc v1.2, seções 2, 3.1 e 7). Relatório fica em
 * {@link ReportService}; análise das imagens em {@link ImageAnalysisService}.
 */
@Service
public class AssessmentService {

    /** Disparado depois do commit para a pré-análise assíncrona da imagem. */
    public record ImageLinkedEvent(UUID companyId, UUID contactId, UUID assessmentId, UUID assessmentImageId,
                                   UUID userId) {
    }

    private final AssessmentRepository assessments;
    private final AssessmentImageRepository images;
    private final CatalogRepository catalog;
    private final TrichologyAccess access;
    private final TrichologySeeder seeder;
    private final TrichoscopyUseCase trichoscopy;
    private final TrichoscopyRepository trichoscopyImages;
    private final TenantAuditRecorder auditor;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final String publicBaseUrl;
    private PatientTimelineRecorder timeline;

    public AssessmentService(AssessmentRepository assessments, AssessmentImageRepository images,
                             CatalogRepository catalog, TrichologyAccess access, TrichologySeeder seeder,
                             TrichoscopyUseCase trichoscopy, TrichoscopyRepository trichoscopyImages,
                             TenantAuditRecorder auditor, ApplicationEventPublisher events, Clock clock,
                             @Value("${app.mail.frontend-base-url:}") String publicBaseUrl) {
        this.assessments = assessments;
        this.images = images;
        this.catalog = catalog;
        this.access = access;
        this.seeder = seeder;
        this.trichoscopy = trichoscopy;
        this.trichoscopyImages = trichoscopyImages;
        this.auditor = auditor;
        this.events = events;
        this.clock = clock;
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
    }

    private com.becommerce.crm.clinical.trichology.application.port.AnamnesisClinicalView clinicalView;
    private AssessmentAnamnesisLinks anamnesisLinks;

    /** Opcional: o exame lê a anamnese concluída do paciente. */
    @Autowired(required = false)
    public void setAnamnesis(com.becommerce.crm.clinical.trichology.application.port.AnamnesisClinicalView clinicalView,
                             AssessmentAnamnesisLinks anamnesisLinks) {
        this.clinicalView = clinicalView;
        this.anamnesisLinks = anamnesisLinks;
    }

    /** Anamnese ligada ao exame (concluída), se houver. */
    java.util.Optional<com.becommerce.crm.clinical.trichology.application.port.AnamnesisClinicalView.ClinicalAnamnesis>
            linkedAnamnesis(TrichologyAssessment a) {
        if (clinicalView == null || anamnesisLinks == null) {
            return java.util.Optional.empty();
        }
        var s = a.state();
        return anamnesisLinks.anamnesisOf(s.companyId(), s.id())
                .flatMap(id -> clinicalView.byId(s.companyId(), s.contactId(), id));
    }

    /** Respostas clínicas: da anamnese ligada; exames antigos usam o quiz gravado. */
    public Map<String, Object> clinicalAnswers(TrichologyAssessment a) {
        var linked = linkedAnamnesis(a);
        if (linked.isPresent()) {
            return linked.get().quizLike();
        }
        return a.state().quizAnswers() == null ? Map.of() : a.state().quizAnswers();
    }

    @Autowired(required = false)
    public void setPatientTimelineRecorder(PatientTimelineRecorder timeline) {
        this.timeline = timeline;
    }

    // ------------------------------------------------------------ abertura

    @Transactional
    public AssessmentDetail open(UUID companyId, UUID contactId, OpenAssessmentRequest request, UUID userId) {
        return inTenant(companyId, () -> {
            seeder.ensureSeeded(companyId);
            UUID protocolId = request.protocolId() != null ? request.protocolId() : defaultProtocol(companyId);
            catalog.protocol(companyId, protocolId)
                    .filter(CaptureProtocol::active)
                    .orElseThrow(() -> new TrichologyValidationException("Protocolo de captura inválido."));
            var anamnesis = clinicalView == null ? java.util.Optional
                    .<com.becommerce.crm.clinical.trichology.application.port.AnamnesisClinicalView.ClinicalAnamnesis>empty()
                    : clinicalView.latestCompleted(companyId, contactId);
            String complaint = request.chiefComplaint();
            if ((complaint == null || complaint.isBlank()) && anamnesis.isPresent()
                    && anamnesis.get().quizLike().get("chief_complaint") instanceof String fromAnamnesis) {
                complaint = fromAnamnesis;
            }
            TrichologyAssessment saved = assessments.save(TrichologyAssessment.open(companyId, contactId, userId,
                    protocolId, request.consentGiven(), complaint, userId, now()));
            if (anamnesis.isPresent() && anamnesisLinks != null) {
                anamnesisLinks.link(companyId, saved.getId(), anamnesis.get().anamnesisId());
            }
            note(companyId, contactId, "Exame tricológico aberto.", userId);
            audit(companyId, AuditAction.CREATE, saved.getId(), "TRICHOLOGY_ASSESSMENT_OPENED", userId,
                    Map.of("contactId", contactId.toString()));
            return detail(saved);
        });
    }

    @Transactional(readOnly = true)
    public List<AssessmentSummary> list(UUID companyId, UUID contactId) {
        return inTenant(companyId, () -> assessments.listByContact(companyId, contactId).stream()
                .map(a -> {
                    var s = a.state();
                    return new AssessmentSummary(s.id(), s.status().name(), s.reportVersion(),
                            s.trichoscopyPerformed(), s.supersedesId(), s.professionalName(), s.createdAt(),
                            s.publishedAt(), s.cancelledAt());
                }).toList());
    }

    @Transactional(readOnly = true)
    public AssessmentDetail get(UUID companyId, UUID contactId, UUID id) {
        return inTenant(companyId, () -> detail(require(companyId, contactId, id)));
    }

    @Transactional
    public AssessmentDetail cancel(UUID companyId, UUID contactId, UUID id, CancelRequest request, UUID userId) {
        return inTenant(companyId, () -> {
            TrichologyAssessment a = require(companyId, contactId, id);
            a.cancel(request == null ? null : request.reason(), now());
            TrichologyAssessment saved = assessments.save(a);
            audit(companyId, AuditAction.UPDATE, id, "TRICHOLOGY_ASSESSMENT_CANCELLED", userId, Map.of());
            return detail(saved);
        });
    }

    // ------------------------------------------------------------ captura

    /** Foto nova do tricoscópio já ligada ao exame (upload pela galeria existente + vínculo). */
    @Transactional
    public ImageView captureAndLink(UUID companyId, UUID contactId, UUID id, String fileName, String contentType,
                                    byte[] data, String regionCode, String magnification, String qualityStatus,
                                    UUID userId) {
        access.require(companyId);
        runInTenant(companyId, () -> requireRegion(require(companyId, contactId, id), regionCode));
        // capture() limpa o TenantContext no fim; o vínculo reabre o contexto.
        TrichoscopyImageResponse captured = trichoscopy.capture(companyId, contactId, fileName, contentType, data,
                null, userId);
        return link(companyId, contactId, id,
                new LinkImageRequest(captured.id(), regionCode, magnification, qualityStatus), userId);
    }

    @Transactional
    public ImageView link(UUID companyId, UUID contactId, UUID id, LinkImageRequest request, UUID userId) {
        return inTenant(companyId, () -> {
            TrichologyAssessment a = require(companyId, contactId, id);
            a.requireEditable();
            requireRegion(a, request.regionCode());
            PatientTrichoscopyImage photo = trichoscopyImages
                    .findByIdAndCompanyIdAndContactId(request.imageId(), companyId, contactId)
                    .orElseThrow(() -> new TrichologyNotFoundException("Imagem de tricoscopia não encontrada."));
            boolean ai = access.aiEnabled() && !"BLURRED".equals(request.qualityStatus());
            AssessmentImage image = images.save(AssessmentImage.link(companyId, id, photo.getId(),
                    request.regionCode(), blankToNull(request.magnification()), request.qualityStatus(), ai, now()));
            a.imageLinked(now());
            assessments.save(a);
            audit(companyId, AuditAction.CREATE, id, "TRICHOLOGY_IMAGE_LINKED", userId,
                    Map.of("imageId", photo.getId().toString(), "region", request.regionCode()));
            if (ai) {
                events.publishEvent(new ImageLinkedEvent(companyId, contactId, id, image.id(), userId));
            }
            return view(companyId, image, protocolOf(a), minConfidence(companyId), photo);
        });
    }

    /** Volta a foto para PENDING e agenda nova análise depois do commit. */
    @Transactional
    public void reanalyze(UUID companyId, UUID contactId, UUID id, UUID assessmentImageId, UUID userId) {
        runInTenant(companyId, () -> {
            TrichologyAssessment a = require(companyId, contactId, id);
            a.requireEditable();
            if (!access.aiEnabled()) {
                throw new TrichologyValidationException("A análise por IA está desligada.");
            }
            AssessmentImage image = images.findInAssessment(companyId, id, assessmentImageId)
                    .orElseThrow(() -> new TrichologyNotFoundException("Imagem do exame não encontrada."));
            if (AssessmentImage.AI_RUNNING.equals(image.aiStatus())) {
                throw new com.becommerce.crm.clinical.trichology.domain.exception.TrichologyStateException(
                        "A análise desta imagem ainda está em andamento.");
            }
            images.save(image.withAiStatus(AssessmentImage.AI_PENDING, now()));
            audit(companyId, AuditAction.UPDATE, id, "TRICHOLOGY_IMAGE_REANALYSIS", userId,
                    Map.of("assessmentImageId", assessmentImageId.toString()));
            events.publishEvent(new ImageLinkedEvent(companyId, contactId, id, assessmentImageId, userId));
        });
    }

    @Transactional
    public AssessmentDetail finishImaging(UUID companyId, UUID contactId, UUID id, FinishImagingRequest request,
                                          UUID userId) {
        return inTenant(companyId, () -> {
            TrichologyAssessment a = require(companyId, contactId, id);
            List<SkippedRegion> skipped = request == null || request.skippedRegions() == null
                    ? List.of() : request.skippedRegions();
            skipped.forEach(r -> requireRegion(a, r.regionCode()));
            a.finishImaging(skipped, now());
            return detail(assessments.save(a));
        });
    }

    /** Revisão do profissional: aceita/edita achados e confirma medidas (só o aceito vai ao relatório). */
    @Transactional
    public ImageView reviewImage(UUID companyId, UUID contactId, UUID id, UUID assessmentImageId,
                                 ReviewImageRequest request, UUID userId) {
        return inTenant(companyId, () -> {
            TrichologyAssessment a = require(companyId, contactId, id);
            a.requireEditable();
            AssessmentImage image = images.findInAssessment(companyId, id, assessmentImageId)
                    .orElseThrow(() -> new TrichologyNotFoundException("Imagem do exame não encontrada."));
            Set<String> codes = catalogCodes(companyId);
            Map<String, String> labels = catalogLabels(companyId);
            List<Finding> accepted = new ArrayList<>();
            for (Finding f : request.acceptedFindings() == null ? List.<Finding>of() : request.acceptedFindings()) {
                if (!codes.contains(f.code())) {
                    throw new TrichologyValidationException("Achado fora do catálogo: " + f.code());
                }
                boolean fromAi = image.aiFindings().stream().anyMatch(x -> x.code().equals(f.code()));
                accepted.add(new Finding(f.code(), labels.get(f.code()), f.severity(), null, f.evidence(),
                        fromAi ? Finding.SOURCE_AI : Finding.SOURCE_PROFESSIONAL));
            }
            AssessmentImage saved = images.save(image.reviewed(accepted, request.confirmedMeasures(),
                    request.includeInReport() == null || request.includeInReport(), userId, now()));
            audit(companyId, AuditAction.APPROVE, id, "TRICHOLOGY_FINDINGS_REVIEWED", userId,
                    Map.of("assessmentImageId", assessmentImageId.toString(),
                            "accepted", accepted.stream().map(Finding::code).toList(),
                            "discarded", image.aiFindings().stream().map(Finding::code)
                                    .filter(c -> accepted.stream().noneMatch(x -> x.code().equals(c))).toList()));
            PatientTrichoscopyImage photo = trichoscopyImages
                    .findByIdAndCompanyIdAndContactId(saved.imageId(), companyId, contactId).orElse(null);
            return view(companyId, saved, protocolOf(a), minConfidence(companyId), photo);
        });
    }

    // ------------------------------------------------------------ clínico e laboratório

    @Transactional
    public AssessmentDetail saveClinical(UUID companyId, UUID contactId, UUID id, ClinicalRequest request,
                                         UUID userId) {
        return inTenant(companyId, () -> {
            TrichologyAssessment a = require(companyId, contactId, id);
            if (request.pullTest() != null && request.pullTest().sites() != null) {
                request.pullTest().sites().forEach(site -> requireRegion(a, site.regionCode()));
            }
            a.saveClinical(request.pullTest(), request.notes(), now());
            audit(companyId, AuditAction.UPDATE, id, "TRICHOLOGY_CLINICAL_SAVED", userId, Map.of());
            return detail(assessments.save(a));
        });
    }

    @Transactional
    public AssessmentDetail requestExams(UUID companyId, UUID contactId, UUID id, ExamsRequest request,
                                         UUID userId) {
        return inTenant(companyId, () -> {
            TrichologyAssessment a = require(companyId, contactId, id);
            Set<String> known = catalog.markers(companyId).stream().map(m -> m.code()).collect(Collectors.toSet());
            List<String> codes = request.markerCodes() == null ? List.of() : request.markerCodes().stream()
                    .distinct().toList();
            codes.forEach(c -> {
                if (!known.contains(c)) {
                    throw new TrichologyValidationException("Marcador desconhecido: " + c);
                }
            });
            a.requestExams(codes, now());
            audit(companyId, AuditAction.UPDATE, id, "TRICHOLOGY_EXAMS_REQUESTED", userId, Map.of("markers", codes));
            return detail(assessments.save(a));
        });
    }

    @Transactional
    public AssessmentDetail loadLabs(UUID companyId, UUID contactId, UUID id, LabsRequest request, UUID userId) {
        return inTenant(companyId, () -> {
            TrichologyAssessment a = require(companyId, contactId, id);
            List<LabResult> results = request.results() == null ? List.of() : request.results();
            if (request.collectedOn() != null && request.collectedOn().isAfter(now().atZone(ZoneOffset.UTC)
                    .toLocalDate().plusDays(1))) {
                throw new TrichologyValidationException("A data da coleta não pode ser futura.");
            }
            Object sexAnswer = clinicalAnswers(a).get("sex");
            String sex = sexAnswer == null ? null : String.valueOf(sexAnswer);
            RpsSummary rps = RpsCalculator.calculate(results, catalog.markers(companyId), sex);
            a.loadLabs(results, request.collectedOn(), rps, now());
            audit(companyId, AuditAction.UPDATE, id, "TRICHOLOGY_LABS_LOADED", userId, Map.of());
            return detail(assessments.save(a));
        });
    }

    // ------------------------------------------------------------ apoio (usado por outros serviços)

    TrichologyAssessment require(UUID companyId, UUID contactId, UUID id) {
        return assessments.find(companyId, contactId, id)
                .orElseThrow(() -> new TrichologyNotFoundException("Exame tricológico não encontrado."));
    }

    CaptureProtocol protocolOf(TrichologyAssessment a) {
        UUID protocolId = a.state().protocolId();
        return protocolId == null ? null : catalog.protocol(a.getCompanyId(), protocolId).orElse(null);
    }

    BigDecimal minConfidence(UUID companyId) {
        return catalog.settings(companyId).map(Settings::minConfidence).orElse(access.defaultMinConfidence());
    }

    AssessmentDetail detail(TrichologyAssessment a) {
        var s = a.state();
        var anamnesis = linkedAnamnesis(a);
        CaptureProtocol protocol = protocolOf(a);
        BigDecimal min = minConfidence(s.companyId());
        Map<UUID, PatientTrichoscopyImage> photos = new HashMap<>();
        trichoscopyImages.findByCompanyIdAndContactId(s.companyId(), s.contactId())
                .forEach(p -> photos.put(p.getId(), p));
        List<AssessmentImage> linked = images.listByAssessment(s.companyId(), s.id());
        List<ImageView> imageViews = linked.stream()
                .map(i -> view(s.companyId(), i, protocol, min, photos.get(i.imageId()))).toList();
        return new AssessmentDetail(s.id(), s.contactId(), s.status().name(), s.reportVersion(), s.supersedesId(),
                protocol, progress(protocol, linked, s.skippedRegions()), s.consentAt(), s.chiefComplaint(),
                anamnesis.isPresent() ? anamnesis.get().quizLike() : s.quizAnswers(), s.quizCompletedAt(),
                s.quizFormExpiresAt(), s.clinicalNotes(), s.pullTest(),
                s.pullTest() != null && s.pullTest().positive(), s.trichoscopyPerformed(), imageViews,
                s.requestedExams(), s.labResults(), s.labCollectedOn(), s.rpsSummary(), s.finalReportPayload(),
                s.verificationCode(), s.professionalName(), s.professionalRegistry(), s.reviewedAt(),
                s.publishedAt(), s.cancelledAt(), s.cancelReason(), s.createdAt(), s.updatedAt(),
                anamnesis.map(x -> x.anamnesisId()).orElse(null), anamnesis.map(x -> x.completedAt()).orElse(null));
    }

    ImageView view(UUID companyId, AssessmentImage i, CaptureProtocol protocol, BigDecimal minConfidence,
                   PatientTrichoscopyImage photo) {
        String label = protocol == null ? i.regionCode()
                : protocol.region(i.regionCode()).map(CaptureProtocol.Region::label).orElse(i.regionCode());
        double min = minConfidence.doubleValue();
        List<FindingView> ai = i.aiFindings().stream()
                .map(f -> new FindingView(f.code(), f.label(), f.severity(), f.confidence(), f.evidence(),
                        f.source(), f.confidence() != null && f.confidence() < min))
                .toList();
        String url = photo == null ? null : "/api/v1/companies/" + companyId + "/storage/"
                + photo.getStorageObjectId() + "?inline=true";
        return new ImageView(i.id(), i.imageId(), i.regionCode(), label, i.magnification(), i.qualityStatus(),
                i.aiStatus(), i.aiImageQuality(), ai, i.aiEstimates(), i.aiNotes(), i.acceptedFindings(),
                i.confirmedMeasures(), i.includeInReport(), i.reviewedAt(),
                photo == null ? null : photo.getTakenAt().toInstant(ZoneOffset.UTC), url);
    }

    Set<String> catalogCodes(UUID companyId) {
        return catalog.findings(companyId).stream().filter(FindingCatalogItem::active)
                .map(FindingCatalogItem::code).collect(Collectors.toSet());
    }

    Map<String, String> catalogLabels(UUID companyId) {
        Map<String, String> labels = new LinkedHashMap<>();
        catalog.findings(companyId).forEach(f -> labels.put(f.code(), f.label()));
        return labels;
    }

    static List<RegionProgress> progress(CaptureProtocol protocol, List<AssessmentImage> linked,
                                         List<SkippedRegion> skipped) {
        if (protocol == null) {
            return List.of();
        }
        return protocol.regions().stream().map(r -> {
            int count = (int) linked.stream().filter(i -> i.regionCode().equals(r.code())).count();
            SkippedRegion skip = skipped == null ? null : skipped.stream()
                    .filter(s -> s.regionCode().equals(r.code())).findFirst().orElse(null);
            return new RegionProgress(r.code(), r.label(), r.minImages(), count, skip != null,
                    skip == null ? null : skip.reason());
        }).toList();
    }

    void note(UUID companyId, UUID contactId, String body, UUID userId) {
        if (timeline != null) {
            timeline.record(companyId, contactId, body, userId);
        }
    }

    void audit(UUID companyId, AuditAction action, UUID assessmentId, String description, UUID userId,
               Map<String, Object> values) {
        auditor.record(companyId, action, AuditModule.CONTACTS, "TrichologyAssessment", assessmentId.toString(),
                description, userId, values);
    }

    Instant now() {
        return clock.instant();
    }

    <T> T inTenant(UUID companyId, Supplier<T> work) {
        access.require(companyId);
        try {
            TenantContext.setCompanyId(companyId);
            return work.get();
        } finally {
            TenantContext.clear();
        }
    }

    private void runInTenant(UUID companyId, Runnable work) {
        inTenant(companyId, () -> {
            work.run();
            return null;
        });
    }

    private UUID defaultProtocol(UUID companyId) {
        return catalog.protocols(companyId).stream().filter(p -> p.active() && p.isDefault()).findFirst()
                .or(() -> catalog.protocols(companyId).stream().filter(CaptureProtocol::active).findFirst())
                .map(CaptureProtocol::id)
                .orElseThrow(() -> new TrichologyValidationException("Cadastre um protocolo de captura."));
    }

    private void requireRegion(TrichologyAssessment a, String regionCode) {
        CaptureProtocol protocol = protocolOf(a);
        if (regionCode == null || protocol == null || protocol.region(regionCode).isEmpty()) {
            throw new TrichologyValidationException("Região fora do protocolo do exame: " + regionCode);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
