package com.becommerce.crm.finance.report.application.service;

import com.becommerce.crm.finance.report.application.dto.CreateReportSuggestionRequest;
import com.becommerce.crm.finance.report.application.dto.ReportSuggestionResponse;
import com.becommerce.crm.finance.report.application.port.output.ReportSuggestionRepository;
import com.becommerce.crm.finance.report.domain.ReportSuggestionCategory;
import com.becommerce.crm.finance.report.domain.exception.ReportSuggestionValidationException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Sugestões de relatório (V094). Valida os temas escolhidos e a observação
 * opcional, grava sob o contexto de tenant e devolve o id criado.
 */
@Service
public class ReportSuggestionService {

    private static final int NOTES_MAX_LENGTH = 2000;

    private final ReportSuggestionRepository repository;

    public ReportSuggestionService(ReportSuggestionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ReportSuggestionResponse create(UUID companyId, CreateReportSuggestionRequest request,
                                           UUID actor) {
        List<ReportSuggestionCategory> categories = parseCategories(request.categories());
        String notes = normalizeNotes(request.notes());
        try {
            TenantContext.setCompanyId(companyId);
            UUID id = repository.create(companyId,
                    categories.stream().map(Enum::name).toList(), notes, actor);
            return new ReportSuggestionResponse(id);
        } finally {
            TenantContext.clear();
        }
    }

    private static List<ReportSuggestionCategory> parseCategories(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new ReportSuggestionValidationException(
                    "Selecione ao menos um relatório para sugerir.");
        }
        LinkedHashSet<ReportSuggestionCategory> categories = new LinkedHashSet<>();
        for (String value : raw) {
            if (value == null || value.isBlank()) {
                throw new ReportSuggestionValidationException(
                        "Tipo de relatório inválido: " + value + ".");
            }
            try {
                categories.add(ReportSuggestionCategory.from(value));
            } catch (IllegalArgumentException ex) {
                throw new ReportSuggestionValidationException(
                        "Tipo de relatório inválido: " + value + ".");
            }
        }
        return new ArrayList<>(categories);
    }

    private static String normalizeNotes(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > NOTES_MAX_LENGTH) {
            throw new ReportSuggestionValidationException(
                    "A observação deve ter no máximo " + NOTES_MAX_LENGTH + " caracteres.");
        }
        return trimmed;
    }
}
