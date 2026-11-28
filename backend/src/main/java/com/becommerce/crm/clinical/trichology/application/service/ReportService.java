package com.becommerce.crm.clinical.trichology.application.service;

import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider.ChatMessage;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider.ChatRequest;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider.GenerationParams;
import com.becommerce.crm.clinical.document.application.port.out.PatientDocumentRepository;
import com.becommerce.crm.clinical.document.domain.PatientDocument;
import com.becommerce.crm.clinical.documenttemplate.domain.DocumentType;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.ApproveRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.AssessmentDetail;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.Evolution;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.Hypothesis;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.ReportPdf;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.ReportUpdateRequest;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.AiRun;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.AiRunRepository;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.AssessmentImageRepository;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.AssessmentRepository;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.MarkerScore;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.ReportPayload;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.ReportSection;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.RpsSummary;
import com.becommerce.crm.clinical.trichology.domain.AssessmentImage;
import com.becommerce.crm.clinical.trichology.domain.AssessmentStatus;
import com.becommerce.crm.clinical.trichology.domain.CaptureProtocol;
import com.becommerce.crm.clinical.trichology.domain.Finding;
import com.becommerce.crm.clinical.trichology.domain.GuardrailValidator;
import com.becommerce.crm.clinical.trichology.domain.TrichologyAssessment;
import com.becommerce.crm.clinical.trichology.domain.VerificationCode;
import com.becommerce.crm.clinical.trichology.domain.exception.TrichologyStateException;
import com.becommerce.crm.clinical.trichology.domain.exception.TrichologyValidationException;
import com.becommerce.crm.clinical.trichoscopy.application.port.out.TrichoscopyRepository;
import com.becommerce.crm.clinical.trichoscopy.domain.PatientTrichoscopyImage;
import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.masterdata.contact.application.dto.response.ContactResponse;
import com.becommerce.crm.masterdata.contact.application.port.in.ContactUseCase;
import com.becommerce.crm.masterdata.storage.application.dto.StorageDownload;
import com.becommerce.crm.masterdata.storage.application.dto.StorageResponse;
import com.becommerce.crm.masterdata.storage.application.port.input.StorageUseCase;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Relatório do exame (doc v1.2, seção 4): rascunho por seção (código monta os
 * fatos e números; a IA só redige texto, a partir de achados ACEITOS), revisão
 * do profissional, aprovação com PDF, documento do paciente e código de
 * verificação, e nova versão de relatório publicado.
 */
@Service
public class ReportService {

    static final String PROMPT_VERSION = "report-2";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.of("America/Sao_Paulo"));
    private static final String TO_FILL = "A preencher pelo profissional.";

    private final AssessmentService core;
    private final ExamCopilotService copilot;
    private final AssessmentRepository assessments;
    private final AssessmentImageRepository images;
    private final AiRunRepository aiRuns;
    private final ObjectProvider<AiProvider> aiProvider;
    private final TrichologyAccess access;
    private final StorageUseCase storage;
    private final TrichoscopyRepository photos;
    private final PatientDocumentRepository documents;
    private final ContactUseCase contacts;
    private final CompanyRepository companies;
    private final TrichologyReportPdfRenderer renderer;
    private final ObjectMapper mapper;
    private final String publicBaseUrl;

    public ReportService(AssessmentService core, ExamCopilotService copilot, AssessmentRepository assessments,
                         AssessmentImageRepository images, AiRunRepository aiRuns,
                         ObjectProvider<AiProvider> aiProvider, TrichologyAccess access, StorageUseCase storage,
                         TrichoscopyRepository photos, PatientDocumentRepository documents, ContactUseCase contacts,
                         CompanyRepository companies, TrichologyReportPdfRenderer renderer, ObjectMapper mapper,
                         @Value("${app.mail.frontend-base-url:}") String publicBaseUrl) {
        this.core = core;
        this.copilot = copilot;
        this.assessments = assessments;
        this.images = images;
        this.aiRuns = aiRuns;
        this.aiProvider = aiProvider;
        this.access = access;
        this.storage = storage;
        this.photos = photos;
        this.documents = documents;
        this.contacts = contacts;
        this.companies = companies;
        this.renderer = renderer;
        this.mapper = mapper;
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
    }

    // ------------------------------------------------------------ rascunho

    @Transactional
    public AssessmentDetail draft(UUID companyId, UUID contactId, UUID id, UUID userId) {
        return core.inTenant(companyId, () -> {
            TrichologyAssessment a = core.require(companyId, contactId, id);
            a.requireEditable();
            List<AssessmentImage> linked = images.listByAssessment(companyId, id);
            if (linked.stream().anyMatch(i -> AssessmentImage.AI_PENDING.equals(i.aiStatus())
                    || AssessmentImage.AI_RUNNING.equals(i.aiStatus()))) {
                throw new TrichologyStateException("Aguarde a análise das fotos terminar antes de gerar o relatório.");
            }
            CaptureProtocol protocol = core.protocolOf(a);
            List<Hypothesis> hypotheses = core.linkedAnamnesis(a).isPresent()
                    ? ExamCopilotService.hypotheses(a, linked, core.clinicalAnswers(a), "Anamnese: ")
                    : ExamCopilotService.hypotheses(a, linked);
            Evolution evolution = copilot.evolution(companyId, a, linked);
            Map<String, Object> facts = facts(a, linked, protocol, hypotheses, evolution);
            Map<String, String> aiText = generate(companyId, a, facts, userId);

            var s = a.state();
            List<ReportSection> sections = new ArrayList<>();
            sections.add(new ReportSection("identification", "Identificação e dados do exame",
                    (String) facts.get("identificationText"), ReportSection.CODE));
            sections.add(textSection("complaint", "Queixa e histórico", aiText));
            String regionText = (String) facts.get("trichoscopyText");
            String narrative = aiText.get("trichoscopy");
            sections.add(new ReportSection("trichoscopy", "Tricoscopia por região",
                    s.trichoscopyPerformed()
                            ? regionText + (narrative == null ? "" : "\n\n" + narrative)
                            : "Tricoscopia não realizada neste exame.",
                    s.trichoscopyPerformed() && narrative != null ? ReportSection.AI : ReportSection.CODE));
            sections.add(new ReportSection("labs", "Exames laboratoriais e RPS",
                    (String) facts.get("labsText"), ReportSection.CODE));
            sections.add(textSection("analysis", "Análise integrada e hipóteses", aiText));
            sections.add(textSection("conduct", "Conduta e orientações", aiText));
            if (evolution != null) {
                sections.add(textSection("evolution", "Evolução", aiText));
            }
            List<String> violations = GuardrailValidator.validateReport(sections, s.trichoscopyPerformed());
            a.draftReady(new ReportPayload(sections, facts, violations), !violations.isEmpty(), core.now());
            core.audit(companyId, AuditAction.CREATE, id, "TRICHOLOGY_REPORT_DRAFTED", userId,
                    Map.of("blocked", !violations.isEmpty()));
            return core.detail(assessments.save(a));
        });
    }

    /** Edição do profissional: seções alteradas viram EDITED e passam de novo pelo guardrail. */
    @Transactional
    public AssessmentDetail update(UUID companyId, UUID contactId, UUID id, ReportUpdateRequest request,
                                   UUID userId) {
        return core.inTenant(companyId, () -> {
            TrichologyAssessment a = core.require(companyId, contactId, id);
            ReportPayload current = a.state().finalReportPayload();
            if (current == null) {
                throw new TrichologyStateException("Gere o rascunho do relatório antes de editar.");
            }
            Map<String, ReportSection> before = new LinkedHashMap<>();
            current.sections().forEach(sec -> before.put(sec.key(), sec));
            List<ReportSection> sections = new ArrayList<>();
            for (ReportSection incoming : request.sections() == null ? List.<ReportSection>of() : request.sections()) {
                ReportSection old = before.get(incoming.key());
                if (old == null) {
                    throw new TrichologyValidationException("Seção desconhecida: " + incoming.key());
                }
                String content = incoming.content() == null ? "" : incoming.content().trim();
                if (content.length() > 20000) {
                    throw new TrichologyValidationException("Seção \"" + old.title() + "\" longa demais.");
                }
                boolean changed = !content.equals(old.content() == null ? "" : old.content().trim());
                sections.add(new ReportSection(old.key(), old.title(), content,
                        changed ? ReportSection.EDITED : old.origin()));
            }
            List<String> violations = GuardrailValidator.validateReport(sections, a.state().trichoscopyPerformed());
            a.review(new ReportPayload(sections, current.facts(), violations), userId, core.now());
            core.audit(companyId, AuditAction.UPDATE, id, "TRICHOLOGY_REPORT_REVIEWED", userId,
                    Map.of("blocked", !violations.isEmpty()));
            return core.detail(assessments.save(a));
        });
    }

    // ------------------------------------------------------------ aprovação

    @Transactional
    public AssessmentDetail approve(UUID companyId, UUID contactId, UUID id, ApproveRequest request, UUID userId) {
        core.inTenant(companyId, () -> {
            TrichologyAssessment a = core.require(companyId, contactId, id);
            if (a.getStatus() != AssessmentStatus.REVIEWED) {
                throw new TrichologyStateException("Revise e salve o relatório sem pendências antes de publicar.");
            }
            return a;
        });
        // Serviços de contato/storage limpam o TenantContext; cada etapa reabre o contexto.
        ContactResponse contact = contacts.getById(companyId, contactId);
        String patientName = (safe(contact.firstName()) + " " + safe(contact.lastName())).trim();
        String clinicName = companies.findById(companyId).map(Company::getTradingName).orElse("");

        return core.inTenant(companyId, () -> {
            TrichologyAssessment a = core.require(companyId, contactId, id);
            var s = a.state();
            String professionalName = request.professionalName();
            if (professionalName == null || professionalName.isBlank()) {
                throw new TrichologyValidationException("Informe o nome do profissional que assina.");
            }
            String code = uniqueCode();
            Instant now = core.now();
            List<TrichologyReportPdfRenderer.Photo> pdfPhotos = photosFor(companyId, contactId, a);
            TenantContext.setCompanyId(companyId);
            byte[] pdf = renderer.render(new TrichologyReportPdfRenderer.Input(clinicName, patientName,
                    professionalName.trim(), blankToNull(request.professionalRegistry()), s.reportVersion(), now,
                    s.finalReportPayload().sections(), pdfPhotos, s.rpsSummary(), code, verifyUrl(code)));
            String sha = sha256(pdf);
            StorageResponse stored = storage.uploadForContact(companyId, contactId,
                    "relatorio-tricologia-" + DATE.format(now).replace('/', '-') + "-v" + s.reportVersion() + ".pdf",
                    "application/pdf", pdf, userId);
            TenantContext.setCompanyId(companyId);
            PatientDocument document = documents.save(PatientDocument.issue(companyId, contactId, null,
                    DocumentType.TRICHOLOGY_REPORT, "Relatório de tricologia"
                            + (s.reportVersion() > 1 ? " (versão " + s.reportVersion() + ")" : ""),
                    TrichologyReportPdfRenderer.plainText(s.finalReportPayload().sections()),
                    professionalName.trim(), blankToNull(request.professionalRegistry()) == null ? "—"
                            : request.professionalRegistry().trim(), userId));
            a.publish(code, sha, stored.id(), document.getId(), professionalName, request.professionalRegistry(),
                    now);
            TrichologyAssessment saved = assessments.save(a);
            core.note(companyId, contactId, "Relatório de tricologia publicado (código " + code + ").", userId);
            core.audit(companyId, AuditAction.APPROVE, id, "TRICHOLOGY_REPORT_PUBLISHED", userId,
                    Map.of("verificationCode", code, "pdfSha256", sha, "version", s.reportVersion()));
            return core.detail(saved);
        });
    }

    @Transactional
    public AssessmentDetail newVersion(UUID companyId, UUID contactId, UUID id, UUID userId) {
        return core.inTenant(companyId, () -> {
            TrichologyAssessment a = core.require(companyId, contactId, id);
            TrichologyAssessment next = assessments.save(a.newVersion(userId, core.now()));
            Instant now = core.now();
            for (AssessmentImage i : images.listByAssessment(companyId, id)) {
                images.save(new AssessmentImage(UUID.randomUUID(), companyId, next.getId(), i.imageId(),
                        i.regionCode(), i.magnification(), i.qualityStatus(), i.aiStatus(), i.aiImageQuality(),
                        i.aiFindings(), i.aiEstimates(), i.aiNotes(), i.acceptedFindings(), i.confirmedMeasures(),
                        i.includeInReport(), i.reviewedBy(), i.reviewedAt(), now, now));
            }
            core.audit(companyId, AuditAction.CREATE, next.getId(), "TRICHOLOGY_REPORT_NEW_VERSION", userId,
                    Map.of("supersedes", id.toString()));
            return core.detail(next);
        });
    }

    @Transactional(readOnly = true)
    public ReportPdf pdf(UUID companyId, UUID contactId, UUID id, UUID userId) {
        UUID storageId = core.inTenant(companyId, () -> {
            TrichologyAssessment a = core.require(companyId, contactId, id);
            if (a.state().pdfStorageObjectId() == null) {
                throw new TrichologyStateException("O relatório ainda não foi publicado.");
            }
            core.audit(companyId, AuditAction.EXPORT, id, "TRICHOLOGY_REPORT_DOWNLOADED", userId, Map.of());
            return a.state().pdfStorageObjectId();
        });
        StorageDownload file = storage.download(companyId, storageId);
        return new ReportPdf(file.fileName(), file.data());
    }

    // ------------------------------------------------------------ fatos (código)

    Map<String, Object> facts(TrichologyAssessment a, List<AssessmentImage> linked, CaptureProtocol protocol,
                              List<Hypothesis> hypotheses, Evolution evolution) {
        var s = a.state();
        List<String> done = new ArrayList<>();
        List<String> notDone = new ArrayList<>();
        (s.quizCompletedAt() != null || core.linkedAnamnesis(a).isPresent() ? done : notDone).add("Anamnese");
        (s.trichoscopyPerformed() ? done : notDone).add("Tricoscopia digital");
        (s.pullTest() != null ? done : notDone).add("Pull test");
        (s.labResults() != null && !s.labResults().isEmpty() ? done : notDone).add("Exames laboratoriais");

        StringBuilder id = new StringBuilder();
        id.append("Data do exame: ").append(DATE.format(s.createdAt())).append('\n');
        id.append("Realizado: ").append(done.isEmpty() ? "—" : String.join(", ", done)).append('\n');
        id.append("Não realizado: ").append(notDone.isEmpty() ? "—" : String.join(", ", notDone));
        if (s.pullTest() != null) {
            id.append("\nPull test: ").append(s.pullTest().positive() ? "positivo" : "negativo");
            s.pullTest().sites().forEach(site -> id.append(" · ").append(regionLabel(protocol, site.regionCode()))
                    .append(' ').append(site.hairsPulled()).append(" fios"));
        }

        StringBuilder tricho = new StringBuilder();
        List<Map<String, Object>> regions = new ArrayList<>();
        Map<String, List<AssessmentImage>> byRegion = linked.stream().filter(AssessmentImage::includeInReport)
                .collect(Collectors.groupingBy(AssessmentImage::regionCode, LinkedHashMap::new, Collectors.toList()));
        byRegion.forEach((region, list) -> {
            List<String> findings = list.stream().flatMap(i -> i.acceptedFindings().stream())
                    .map(f -> f.label() + (f.severity() == null ? "" : " (" + severity(f.severity()) + ")"))
                    .distinct().toList();
            Map<String, Object> measures = new LinkedHashMap<>();
            ExamCopilotService.measures(list).forEach(m -> measures.put(m.measure(), m.value()));
            tricho.append(regionLabel(protocol, region)).append(": ")
                    .append(findings.isEmpty() ? "sem achados aceitos" : String.join("; ", findings));
            if (!measures.isEmpty()) {
                tricho.append(" — ").append(measures.entrySet().stream()
                        .map(e -> measureLabel(e.getKey()) + " " + e.getValue()).collect(Collectors.joining(", ")));
            }
            tricho.append('\n');
            regions.add(Map.of("region", regionLabel(protocol, region), "findings", findings, "measures", measures));
        });
        if (s.skippedRegions() != null) {
            s.skippedRegions().forEach(r -> tricho.append(regionLabel(protocol, r.regionCode()))
                    .append(": não fotografada (").append(r.reason()).append(")\n"));
        }

        StringBuilder labs = new StringBuilder();
        RpsSummary rps = s.rpsSummary();
        if (rps == null || rps.markers().isEmpty()) {
            labs.append("Exames laboratoriais não lançados neste exame.");
        } else {
            labs.append("Coleta: ").append(s.labCollectedOn() == null ? "não informada" : s.labCollectedOn())
                    .append(". RPS geral ").append(rps.overallScore()).append("/100.");
            List<String> off = rps.markers().stream().filter(m -> !"IN_RANGE".equals(m.position())
                            && !"NO_RANGE".equals(m.position()))
                    .map(m -> m.label() + " " + ("LOW".equals(m.position()) ? "abaixo" : "acima") + " da referência")
                    .toList();
            if (!off.isEmpty()) {
                labs.append(" Fora da referência: ").append(String.join("; ", off)).append('.');
            }
        }

        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("identificationText", id.toString());
        facts.put("trichoscopyText", tricho.toString().trim());
        facts.put("labsText", labs.toString());
        facts.put("performed", done);
        facts.put("notPerformed", notDone);
        facts.put("regions", regions);
        facts.put("hypotheses", hypotheses.stream().map(h -> Map.of("label", h.label(), "support", h.support()))
                .toList());
        facts.put("rpsOverall", rps == null ? null : rps.overallScore());
        facts.put("markers", rps == null ? List.of() : rps.markers().stream().map(this::markerFact).toList());
        if (evolution != null) {
            facts.put("evolution", Map.of("previousPublishedAt", String.valueOf(evolution.previousPublishedAt()),
                    "deltas", evolution.deltas(), "previousRps", String.valueOf(evolution.previousRps()),
                    "currentRps", String.valueOf(evolution.currentRps())));
        }
        return facts;
    }

    private Map<String, Object> markerFact(MarkerScore m) {
        return Map.of("label", m.label(), "value", m.value().toPlainString() + " " + safe(m.unit()),
                "position", m.position(), "quartile", String.valueOf(m.quartile()));
    }

    // ------------------------------------------------------------ IA (texto)

    private Map<String, String> generate(UUID companyId, TrichologyAssessment a, Map<String, Object> facts,
                                         UUID userId) {
        AiProvider ai = aiProvider.getIfAvailable();
        if (ai == null || !access.aiEnabled()) {
            return Map.of();
        }
        var s = a.state();
        Map<String, Object> input = new LinkedHashMap<>();
        var anamnesis = core.linkedAnamnesis(a);
        if (anamnesis.isPresent()) {
            // perguntas e respostas da anamnese, sem as vinculadas ao cadastro (nada identificável)
            input.put("anamnesis", anamnesis.get().answers().stream()
                    .map(qa -> Map.of("section", qa.section(), "question", qa.question(), "answer", qa.answer()))
                    .toList());
        } else {
            input.put("quiz", s.quizAnswers() == null ? Map.of() : s.quizAnswers());
        }
        input.put("chiefComplaint", s.chiefComplaint());
        input.put("clinicalNotes", s.clinicalNotes());
        input.put("trichoscopyPerformed", s.trichoscopyPerformed());
        input.putAll(facts);
        input.remove("identificationText");
        String system = """
                Você redige seções de um relatório de avaliação tricológica para o profissional revisar.
                Use SOMENTE os dados fornecidos (achados aceitos pelo profissional, anamnese ou quiz, laboratório).
                Não invente achados, números ou exames. Não afirme diagnóstico: fale em hipóteses a investigar.
                Não prescreva medicamentos nem doses. Se a tricoscopia não foi realizada, não a mencione como feita.
                Português do Brasil, frases curtas, tom técnico.
                Responda apenas com JSON: {"complaint":"...","trichoscopy":"...","analysis":"...",
                "conduct":"...","evolution":"..."} (use null quando não houver dados para a seção).""";
        long started = System.nanoTime();
        String status = "OK";
        String error = null;
        Map<String, String> out = new LinkedHashMap<>();
        try {
            String json = ai.chatStructured(new ChatRequest(companyId, userId, List.of(
                    new ChatMessage("system", system),
                    new ChatMessage("user", mapper.writeValueAsString(input))))
                    .withParams(new GenerationParams(access.textModel(), 0.2, 1800,
                            Duration.ofSeconds(access.timeoutSeconds()))));
            JsonNode root = mapper.readTree(json);
            for (String key : List.of("complaint", "trichoscopy", "analysis", "conduct", "evolution")) {
                JsonNode node = root.get(key);
                if (node != null && node.isTextual() && !node.asText().isBlank()) {
                    out.put(key, node.asText().trim());
                }
            }
        } catch (Exception e) {
            status = "FAILED";
            error = e.getClass().getSimpleName();
        }
        aiRuns.save(new AiRun(UUID.randomUUID(), companyId, a.getId(), null, "REPORT_SECTION", access.textModel(),
                PROMPT_VERSION, null, null, null, status, error,
                (int) ((System.nanoTime() - started) / 1_000_000), userId, core.now()));
        return out;
    }

    private static ReportSection textSection(String key, String title, Map<String, String> aiText) {
        String text = aiText.get(key);
        return text == null ? new ReportSection(key, title, TO_FILL, ReportSection.CODE)
                : new ReportSection(key, title, text, ReportSection.AI);
    }

    // ------------------------------------------------------------ apoio

    private List<TrichologyReportPdfRenderer.Photo> photosFor(UUID companyId, UUID contactId, TrichologyAssessment a) {
        CaptureProtocol protocol = core.protocolOf(a);
        List<TrichologyReportPdfRenderer.Photo> out = new ArrayList<>();
        for (AssessmentImage i : images.listByAssessment(companyId, a.getId())) {
            if (!i.includeInReport()) {
                continue;
            }
            TenantContext.setCompanyId(companyId);
            PatientTrichoscopyImage photo = photos.findByIdAndCompanyIdAndContactId(i.imageId(), companyId, contactId)
                    .orElse(null);
            if (photo == null) {
                continue;
            }
            StorageDownload file = storage.download(companyId, photo.getStorageObjectId());
            String caption = i.acceptedFindings().stream().map(Finding::label).distinct()
                    .collect(Collectors.joining(", "));
            out.add(new TrichologyReportPdfRenderer.Photo(regionLabel(protocol, i.regionCode()), i.magnification(),
                    caption, file.data()));
        }
        TenantContext.setCompanyId(companyId);
        return out;
    }

    private String uniqueCode() {
        for (int i = 0; i < 5; i++) {
            String code = VerificationCode.generate();
            if (!assessments.verificationCodeExists(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Não foi possível gerar código de verificação único.");
    }

    private String verifyUrl(String code) {
        return publicBaseUrl.isEmpty() ? null : publicBaseUrl + "/verificar-relatorio/" + code;
    }

    private static String regionLabel(CaptureProtocol protocol, String code) {
        return protocol == null ? code
                : protocol.region(code).map(CaptureProtocol.Region::label).orElse(code);
    }

    private static String severity(String severity) {
        return switch (severity) {
            case "MILD" -> "leve";
            case "MODERATE" -> "moderada";
            case "SEVERE" -> "intensa";
            default -> severity;
        };
    }

    private static String measureLabel(String key) {
        return switch (key) {
            case "hairDensityPerCm2" -> "densidade (fios/cm²)";
            case "thinHairPercent" -> "fios finos (%)";
            case "singleHairFollicularUnitsPercent" -> "UF com fio único (%)";
            case "meanShaftDiameterMicrons" -> "diâmetro médio (µm)";
            default -> key;
        };
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
