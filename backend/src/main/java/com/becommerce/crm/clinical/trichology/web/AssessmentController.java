package com.becommerce.crm.clinical.trichology.web;

import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.ApproveRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.AssessmentDetail;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.AssessmentSummary;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.CancelRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.ClinicalRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.CopilotView;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.ExamsRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.FinishImagingRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.ImageView;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.LabsRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.LinkImageRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.OpenAssessmentRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.QuizDefinition;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.QuizLinkResponse;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.QuizRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.ReportPdf;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.ReportUpdateRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.ReviewImageRequest;
import com.becommerce.crm.clinical.trichology.application.service.AssessmentService;
import com.becommerce.crm.clinical.trichology.application.service.ExamCopilotService;
import com.becommerce.crm.clinical.trichology.application.service.ReportService;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/** Exame tricológico do paciente (doc v1.2, seção 7). */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/patients/{contactId}/trichology")
public class AssessmentController {

    private static final String OWN_COMPANY = "Você só pode acessar exames de tricologia da sua própria empresa.";

    private final AssessmentService assessments;
    private final ExamCopilotService copilot;
    private final ReportService reports;

    public AssessmentController(AssessmentService assessments, ExamCopilotService copilot, ReportService reports) {
        this.assessments = assessments;
        this.copilot = copilot;
        this.reports = reports;
    }

    @PostMapping("/assessments")
    @PreAuthorize("hasAuthority('trichology:write')")
    public ResponseEntity<AssessmentDetail> open(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                 @PathVariable UUID contactId,
                                                 @RequestBody OpenAssessmentRequest request,
                                                 @AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assessments.open(companyId, contactId, request, user.userId()));
    }

    @GetMapping("/assessments")
    @PreAuthorize("hasAuthority('trichology:read')")
    public List<AssessmentSummary> list(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                        @PathVariable UUID contactId) {
        return assessments.list(companyId, contactId);
    }

    @GetMapping("/assessments/{id}")
    @PreAuthorize("hasAuthority('trichology:read')")
    public AssessmentDetail get(@CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID contactId,
                                @PathVariable UUID id) {
        return assessments.get(companyId, contactId, id);
    }

    @PostMapping("/assessments/{id}/cancel")
    @PreAuthorize("hasAuthority('trichology:write')")
    public AssessmentDetail cancel(@CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID contactId,
                                   @PathVariable UUID id, @RequestBody(required = false) CancelRequest request,
                                   @AuthenticationPrincipal CurrentUser user) {
        return assessments.cancel(companyId, contactId, id, request, user.userId());
    }

    @GetMapping("/quiz-definition")
    @PreAuthorize("hasAuthority('trichology:read')")
    public QuizDefinition quizDefinition(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                         @PathVariable UUID contactId) {
        return assessments.quizDefinition();
    }

    @PostMapping("/assessments/{id}/quiz-link")
    @PreAuthorize("hasAuthority('trichology:write')")
    public QuizLinkResponse quizLink(@CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID contactId,
                                     @PathVariable UUID id, @AuthenticationPrincipal CurrentUser user) {
        return assessments.quizLink(companyId, contactId, id, user.userId());
    }

    @PutMapping("/assessments/{id}/quiz")
    @PreAuthorize("hasAuthority('trichology:write')")
    public AssessmentDetail quiz(@CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID contactId,
                                 @PathVariable UUID id, @RequestBody QuizRequest request,
                                 @AuthenticationPrincipal CurrentUser user) {
        return assessments.saveQuiz(companyId, contactId, id, request, user.userId());
    }

    @PostMapping(value = "/assessments/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('trichology:write')")
    public ResponseEntity<ImageView> capture(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                             @PathVariable UUID contactId, @PathVariable UUID id,
                                             @RequestParam("file") MultipartFile file,
                                             @RequestParam("regionCode") String regionCode,
                                             @RequestParam(value = "magnification", required = false)
                                             String magnification,
                                             @RequestParam(value = "qualityStatus", required = false)
                                             String qualityStatus,
                                             @AuthenticationPrincipal CurrentUser user) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(assessments.captureAndLink(companyId, contactId, id,
                file.getOriginalFilename(), file.getContentType(), file.getBytes(), regionCode, magnification,
                qualityStatus, user.userId()));
    }

    @PostMapping(value = "/assessments/{id}/images/link", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('trichology:write')")
    public ResponseEntity<ImageView> link(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                          @PathVariable UUID contactId, @PathVariable UUID id,
                                          @RequestBody LinkImageRequest request,
                                          @AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assessments.link(companyId, contactId, id, request, user.userId()));
    }

    @PostMapping("/assessments/{id}/images/{imageId}/reanalyze")
    @PreAuthorize("hasAuthority('trichology:write')")
    public ResponseEntity<Void> reanalyze(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                          @PathVariable UUID contactId, @PathVariable UUID id,
                                          @PathVariable UUID imageId, @AuthenticationPrincipal CurrentUser user) {
        assessments.reanalyze(companyId, contactId, id, imageId, user.userId());
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/assessments/{id}/images/{imageId}/review")
    @PreAuthorize("hasAuthority('trichology:write')")
    public ImageView review(@CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID contactId,
                            @PathVariable UUID id, @PathVariable UUID imageId,
                            @RequestBody ReviewImageRequest request, @AuthenticationPrincipal CurrentUser user) {
        return assessments.reviewImage(companyId, contactId, id, imageId, request, user.userId());
    }

    @PutMapping("/assessments/{id}/imaging-done")
    @PreAuthorize("hasAuthority('trichology:write')")
    public AssessmentDetail imagingDone(@CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID contactId,
                                        @PathVariable UUID id, @RequestBody FinishImagingRequest request,
                                        @AuthenticationPrincipal CurrentUser user) {
        return assessments.finishImaging(companyId, contactId, id, request, user.userId());
    }

    @PutMapping("/assessments/{id}/clinical")
    @PreAuthorize("hasAuthority('trichology:write')")
    public AssessmentDetail clinical(@CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID contactId,
                                     @PathVariable UUID id, @RequestBody ClinicalRequest request,
                                     @AuthenticationPrincipal CurrentUser user) {
        return assessments.saveClinical(companyId, contactId, id, request, user.userId());
    }

    @GetMapping("/assessments/{id}/copilot")
    @PreAuthorize("hasAuthority('trichology:read')")
    public CopilotView copilot(@CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID contactId,
                               @PathVariable UUID id) {
        return copilot.copilot(companyId, contactId, id);
    }

    @PutMapping("/assessments/{id}/exams")
    @PreAuthorize("hasAuthority('trichology:write')")
    public AssessmentDetail exams(@CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID contactId,
                                  @PathVariable UUID id, @RequestBody ExamsRequest request,
                                  @AuthenticationPrincipal CurrentUser user) {
        return assessments.requestExams(companyId, contactId, id, request, user.userId());
    }

    @PutMapping("/assessments/{id}/labs")
    @PreAuthorize("hasAuthority('trichology:write')")
    public AssessmentDetail labs(@CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID contactId,
                                 @PathVariable UUID id, @RequestBody LabsRequest request,
                                 @AuthenticationPrincipal CurrentUser user) {
        return assessments.loadLabs(companyId, contactId, id, request, user.userId());
    }

    @PostMapping("/assessments/{id}/report/draft")
    @PreAuthorize("hasAuthority('trichology:write')")
    public AssessmentDetail draft(@CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID contactId,
                                  @PathVariable UUID id, @AuthenticationPrincipal CurrentUser user) {
        return reports.draft(companyId, contactId, id, user.userId());
    }

    @PutMapping("/assessments/{id}/report")
    @PreAuthorize("hasAuthority('trichology:write')")
    public AssessmentDetail updateReport(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                         @PathVariable UUID contactId, @PathVariable UUID id,
                                         @RequestBody ReportUpdateRequest request,
                                         @AuthenticationPrincipal CurrentUser user) {
        return reports.update(companyId, contactId, id, request, user.userId());
    }

    @PostMapping("/assessments/{id}/report/approve")
    @PreAuthorize("hasAuthority('trichology:approve')")
    public AssessmentDetail approve(@CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID contactId,
                                    @PathVariable UUID id, @RequestBody ApproveRequest request,
                                    @AuthenticationPrincipal CurrentUser user) {
        return reports.approve(companyId, contactId, id, request, user.userId());
    }

    @PostMapping("/assessments/{id}/report/new-version")
    @PreAuthorize("hasAuthority('trichology:approve')")
    public ResponseEntity<AssessmentDetail> newVersion(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                       @PathVariable UUID contactId, @PathVariable UUID id,
                                                       @AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reports.newVersion(companyId, contactId, id, user.userId()));
    }

    @GetMapping("/assessments/{id}/report/pdf")
    @PreAuthorize("hasAuthority('trichology:read')")
    public ResponseEntity<byte[]> pdf(@CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID contactId,
                                      @PathVariable UUID id, @AuthenticationPrincipal CurrentUser user) {
        ReportPdf pdf = reports.pdf(companyId, contactId, id, user.userId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pdf.fileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.content());
    }
}
