package com.becommerce.crm.sales.recall.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.sales.recall.application.dto.CreateRecallRequest;
import com.becommerce.crm.sales.recall.application.dto.RecallResponse;
import com.becommerce.crm.sales.recall.application.dto.RecallSuggestionResponse;
import com.becommerce.crm.sales.recall.application.dto.ScheduleRecallRequest;
import com.becommerce.crm.sales.recall.application.dto.UpdateRecallRequest;
import com.becommerce.crm.sales.recall.application.port.in.RecallUseCase;
import com.becommerce.crm.sales.recall.application.port.out.RecallRepository;
import com.becommerce.crm.sales.recall.application.port.out.RecallSuggestionDismissalRepository;
import com.becommerce.crm.sales.recall.domain.Recall;
import com.becommerce.crm.sales.recall.domain.RecallWindow;
import com.becommerce.crm.sales.recall.domain.exception.RecallNotFoundException;
import com.becommerce.crm.sales.recall.domain.exception.RecallValidationException;
import com.becommerce.crm.shared.application.dto.PageResponse;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class RecallService implements RecallUseCase {

    private static final List<Integer> SUGGESTION_WINDOWS = List.of(1, 3, 6);

    private final RecallRepository recallRepository;
    private final RecallSuggestionDismissalRepository dismissalRepository;
    private final TenantAuditRecorder auditor;

    public RecallService(RecallRepository recallRepository,
                         RecallSuggestionDismissalRepository dismissalRepository,
                         TenantAuditRecorder auditor) {
        this.recallRepository = recallRepository;
        this.dismissalRepository = dismissalRepository;
        this.auditor = auditor;
    }

    @Override
    @Transactional
    public RecallResponse create(UUID companyId, CreateRecallRequest request, UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            LocalDate dueDate = Recall.dueDateFrom(LocalDate.now(), request.dueInAmount(), request.dueInUnit());
            Recall recall = Recall.create(companyId, request.contactId(), request.phone(), request.hostId(),
                    request.tag(), dueDate, createdBy);
            recallRepository.save(recall);
            auditor.record(companyId, AuditAction.CREATE, AuditModule.SCHEDULING, "Recall",
                    recall.getId().toString(), "Retorno criado para o contato " + request.contactId(),
                    createdBy, null);
            return toResponse(recall);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecallResponse> list(UUID companyId, UUID hostId, String tag, String q, String window,
                                             int page, int pageSize) {
        try {
            TenantContext.setCompanyId(companyId);
            RecallWindow recallWindow = RecallWindow.from(window);
            LocalDate today = LocalDate.now();
            RecallRepository.PageResult result = recallRepository.findByCompany(
                    companyId, hostId, normalizeLowercase(tag), normalizeLike(q),
                    recallWindow.fromInclusive(today), recallWindow.toExclusive(today),
                    Math.max(page, 0), Math.max(pageSize, 1));
            List<RecallResponse> content = result.content().stream().map(RecallService::toResponse).toList();
            return PageResponse.of(content, Math.max(page, 0), Math.max(pageSize, 1), result.totalElements());
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public RecallResponse update(UUID companyId, UUID recallId, UpdateRecallRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            Recall recall = requireOwned(companyId, recallId);
            if ((request.dueInAmount() == null) != (request.dueInUnit() == null)) {
                throw new RecallValidationException("Informe a quantidade e a unidade do prazo juntas.");
            }
            LocalDate dueDate = request.dueInAmount() != null
                    ? Recall.dueDateFrom(LocalDate.now(), request.dueInAmount(), request.dueInUnit())
                    : null;
            recall.update(request.phone(), request.hostId(), request.tag(), dueDate);
            recallRepository.save(recall);
            auditor.record(companyId, AuditAction.UPDATE, AuditModule.SCHEDULING, "Recall",
                    recall.getId().toString(), "Retorno atualizado", null, null);
            return toResponse(recall);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public RecallResponse dismiss(UUID companyId, UUID recallId) {
        try {
            TenantContext.setCompanyId(companyId);
            Recall recall = requireOwned(companyId, recallId);
            recall.dismiss();
            recallRepository.save(recall);
            auditor.record(companyId, AuditAction.UPDATE, AuditModule.SCHEDULING, "Recall",
                    recall.getId().toString(), "Retorno descartado", null, null);
            return toResponse(recall);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public RecallResponse schedule(UUID companyId, UUID recallId, ScheduleRecallRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            Recall recall = requireOwned(companyId, recallId);
            recall.schedule(request.appointmentId());
            recallRepository.save(recall);
            auditor.record(companyId, AuditAction.UPDATE, AuditModule.SCHEDULING, "Recall",
                    recall.getId().toString(), "Retorno agendado", null, null);
            return toResponse(recall);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecallSuggestionResponse> suggestions(UUID companyId, int sinceMonths) {
        try {
            TenantContext.setCompanyId(companyId);
            if (!SUGGESTION_WINDOWS.contains(sinceMonths)) {
                throw new RecallValidationException("Período de sugestão inválido. Use 1, 3 ou 6 meses.");
            }
            ZoneId zone = ZoneId.systemDefault();
            Instant cutoff = LocalDate.now().minusMonths(sinceMonths).atStartOfDay(zone).toInstant();
            return recallRepository.findSuggestionCandidates(companyId, cutoff, Instant.now()).stream()
                    .filter(candidate -> !candidate.hasFutureAppointment())
                    .filter(candidate -> !candidate.hasPendingRecall())
                    .filter(candidate -> !candidate.dismissed())
                    .map(RecallService::toSuggestionResponse)
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void dismissSuggestion(UUID companyId, UUID contactId, UUID dismissedBy) {
        try {
            TenantContext.setCompanyId(companyId);
            if (contactId == null) {
                throw new RecallValidationException("Paciente é obrigatório.");
            }
            if (dismissalRepository.existsByCompanyAndContact(companyId, contactId)) {
                return;
            }
            dismissalRepository.save(companyId, contactId, dismissedBy);
            auditor.record(companyId, AuditAction.UPDATE, AuditModule.SCHEDULING, "RecallSuggestion",
                    contactId.toString(), "Sugestão de retorno descartada", dismissedBy, null);
        } finally {
            TenantContext.clear();
        }
    }

    private Recall requireOwned(UUID companyId, UUID recallId) {
        Recall recall = recallRepository.findById(recallId)
                .orElseThrow(() -> new RecallNotFoundException("Retorno", recallId));
        if (!recall.getCompanyId().equals(companyId)) {
            throw new RecallNotFoundException("Retorno", recallId);
        }
        return recall;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizeLowercase(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase();
    }

    private static String normalizeLike(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : "%" + normalized.toLowerCase() + "%";
    }

    private static RecallResponse toResponse(Recall recall) {
        return new RecallResponse(recall.getId(), recall.getCompanyId(), recall.getContactId(),
                recall.getPhone(), recall.getHostId(), recall.getTag(), recall.getDueDate(),
                recall.getStatus(), recall.getAppointmentId(), recall.getCreatedBy(),
                recall.getCreatedAt(), recall.getUpdatedAt());
    }

    private static RecallSuggestionResponse toSuggestionResponse(RecallRepository.SuggestionCandidate candidate) {
        String fullName = Arrays.stream(new String[]{candidate.firstName(), candidate.lastName()})
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .reduce((a, b) -> a + " " + b)
                .orElse("Paciente");
        List<String> procedures = candidate.recentProcedures() == null
                ? List.of()
                : candidate.recentProcedures().stream().map(String::trim).filter(line -> !line.isEmpty()).toList();
        return new RecallSuggestionResponse(candidate.contactId(), fullName, candidate.mobile(),
                candidate.phone(), candidate.hostId(), candidate.lastAppointmentAt(), procedures);
    }
}
