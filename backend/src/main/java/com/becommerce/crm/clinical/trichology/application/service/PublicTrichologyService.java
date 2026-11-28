package com.becommerce.crm.clinical.trichology.application.service;

import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.clinical.anamnesis.formrequest.FormTokenContextHolder;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.PublicQuizSubmit;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.PublicQuizView;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.VerifyView;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.AssessmentRepository;
import com.becommerce.crm.clinical.trichology.domain.QuizEngine;
import com.becommerce.crm.clinical.trichology.domain.TrichologyAssessment;
import com.becommerce.crm.clinical.trichology.domain.VerificationCode;
import com.becommerce.crm.clinical.trichology.domain.exception.TrichologyNotFoundException;
import com.becommerce.crm.clinical.trichology.domain.exception.TrichologyValidationException;
import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.masterdata.contact.application.port.in.ContactUseCase;
import com.becommerce.crm.masterdata.contact.registration.domain.RegistrationLink;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rotas sem login: quiz respondido pelo paciente pelo link (token com hash na
 * policy RLS, padrão V115) e verificação pública do relatório pelo código (função
 * SECURITY DEFINER da V153, sem dados clínicos).
 */
@Service
public class PublicTrichologyService {

    private static final String INVALID = "Link inválido ou expirado. Peça um novo link à clínica.";

    private final AssessmentRepository assessments;
    private final FormTokenContextHolder tokenContext;
    private final CompanyRepository companies;
    private final ContactUseCase contacts;
    private final TrichologyAccess access;
    private final AssessmentService core;
    private final JdbcTemplate jdbc;

    public PublicTrichologyService(AssessmentRepository assessments, FormTokenContextHolder tokenContext,
                                   CompanyRepository companies, ContactUseCase contacts, TrichologyAccess access,
                                   AssessmentService core, JdbcTemplate jdbc) {
        this.assessments = assessments;
        this.tokenContext = tokenContext;
        this.companies = companies;
        this.contacts = contacts;
        this.access = access;
        this.core = core;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public PublicQuizView quiz(String token) {
        access.requireEnabled();
        TrichologyAssessment a = usable(token);
        try {
            UUID companyId = a.getCompanyId();
            String clinic = companies.findById(companyId).map(Company::getTradingName).orElse("");
            String firstName = contacts.getById(companyId, a.getContactId()).firstName();
            return new PublicQuizView(clinic, firstName, core.quizDefinition());
        } finally {
            tokenContext.clear();
            TenantContext.clear();
        }
    }

    @Transactional
    public void submit(String token, PublicQuizSubmit submit) {
        access.requireEnabled();
        if (submit.website() != null && !submit.website().isBlank()) {
            throw new TrichologyValidationException("Requisição inválida.");
        }
        TrichologyAssessment a = usable(token);
        try {
            UUID companyId = a.getCompanyId();
            TenantContext.setCompanyId(companyId);
            Map<String, Object> answers = QuizEngine.sanitize(submit.answers(), true);
            a.completeQuiz(answers, (String) answers.get("chief_complaint"), core.now());
            assessments.save(a);
            core.note(companyId, a.getContactId(), "Quiz capilar respondido pelo paciente.", null);
            core.audit(companyId, AuditAction.UPDATE, a.getId(), "TRICHOLOGY_QUIZ_ANSWERED_BY_PATIENT", null,
                    Map.of());
        } finally {
            tokenContext.clear();
            TenantContext.clear();
        }
    }

    @Transactional(readOnly = true)
    public VerifyView verify(String rawCode) {
        access.requireEnabled();
        String code = VerificationCode.normalize(rawCode);
        if (!VerificationCode.isWellFormed(code)) {
            return new VerifyView(false, null, null, null, null, null, null, false);
        }
        List<VerifyView> rows = jdbc.query("SELECT * FROM app.trichology_verify(?)", (rs, n) -> {
            Timestamp published = rs.getTimestamp("published_at");
            return new VerifyView(true, published == null ? null : published.toInstant(),
                    rs.getString("professional_name"), rs.getString("professional_registry"),
                    rs.getString("company_name"), rs.getInt("report_version"), rs.getString("pdf_sha256"),
                    rs.getBoolean("superseded"));
        }, code);
        return rows.isEmpty() ? new VerifyView(false, null, null, null, null, null, null, false) : rows.get(0);
    }

    /** Lê o exame pelo hash do token; valida expiração e se já foi respondido. */
    private TrichologyAssessment usable(String token) {
        if (token == null || token.isBlank() || token.length() > 200) {
            throw new TrichologyNotFoundException(INVALID);
        }
        String hash = RegistrationLink.hash(token);
        tokenContext.setTokenHash(hash);
        TrichologyAssessment a = assessments.findByQuizTokenHash(hash).orElse(null);
        if (a == null || a.getStatus().isFinal() || a.state().quizFormExpiresAt() == null
                || a.state().quizFormExpiresAt().isBefore(core.now())) {
            tokenContext.clear();
            throw new TrichologyNotFoundException(INVALID);
        }
        return a;
    }
}
