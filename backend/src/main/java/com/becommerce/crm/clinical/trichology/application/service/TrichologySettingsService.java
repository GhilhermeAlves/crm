package com.becommerce.crm.clinical.trichology.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.FindingRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.MarkerRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.ProtocolRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.SettingsUpdateRequest;
import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.SettingsView;
import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.CatalogRepository;
import com.becommerce.crm.clinical.trichology.domain.CaptureProtocol;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.ExamRule;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.FindingCatalogItem;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.Marker;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.Settings;
import com.becommerce.crm.clinical.trichology.domain.exception.TrichologyNotFoundException;
import com.becommerce.crm.clinical.trichology.domain.exception.TrichologyValidationException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/** Configurações da clínica: protocolos, catálogo de achados, marcadores e limites (doc v1.2, seção 9). */
@Service
public class TrichologySettingsService {

    private static final Pattern CODE = Pattern.compile("^[A-Z0-9_]{2,40}$");

    private final CatalogRepository catalog;
    private final TrichologyAccess access;
    private final TrichologySeeder seeder;
    private final TenantAuditRecorder auditor;

    public TrichologySettingsService(CatalogRepository catalog, TrichologyAccess access, TrichologySeeder seeder,
                                     TenantAuditRecorder auditor) {
        this.catalog = catalog;
        this.access = access;
        this.seeder = seeder;
        this.auditor = auditor;
    }

    @Transactional
    public SettingsView get(UUID companyId) {
        return inTenant(companyId, () -> view(companyId, seeder.ensureSeeded(companyId)));
    }

    @Transactional
    public SettingsView update(UUID companyId, SettingsUpdateRequest request, UUID userId) {
        return inTenant(companyId, () -> {
            Settings current = seeder.ensureSeeded(companyId);
            BigDecimal min = request.minConfidence() == null ? current.minConfidence() : request.minConfidence();
            if (min.compareTo(BigDecimal.ZERO) < 0 || min.compareTo(BigDecimal.ONE) > 0) {
                throw new TrichologyValidationException("Limite de confiança deve ficar entre 0 e 1.");
            }
            List<Integer> horizons = request.reportHorizonsMonths() == null ? current.reportHorizonsMonths()
                    : request.reportHorizonsMonths();
            if (horizons.stream().anyMatch(h -> h == null || h < 1 || h > 60)) {
                throw new TrichologyValidationException("Horizontes de retorno entre 1 e 60 meses.");
            }
            List<ExamRule> rules = request.examRules() == null ? current.examRules() : request.examRules();
            Settings saved = catalog.saveSettings(new Settings(companyId, min, horizons, rules));
            audit(companyId, "TRICHOLOGY_SETTINGS_UPDATED", userId);
            return view(companyId, saved);
        });
    }

    @Transactional
    public SettingsView saveProtocol(UUID companyId, UUID protocolId, ProtocolRequest request, UUID userId) {
        return inTenant(companyId, () -> {
            seeder.ensureSeeded(companyId);
            UUID id = protocolId == null ? UUID.randomUUID() : catalog.protocol(companyId, protocolId)
                    .orElseThrow(() -> new TrichologyNotFoundException("Protocolo não encontrado.")).id();
            CaptureProtocol protocol = new CaptureProtocol(id, companyId, request.name(), request.regions(),
                    request.isDefault() && request.active(), request.active());
            if (protocol.isDefault()) {
                catalog.protocols(companyId).stream().filter(p -> p.isDefault() && !p.id().equals(id))
                        .forEach(p -> catalog.saveProtocol(new CaptureProtocol(p.id(), companyId, p.name(),
                                p.regions(), false, p.active())));
            }
            catalog.saveProtocol(protocol);
            audit(companyId, "TRICHOLOGY_PROTOCOL_SAVED", userId);
            return view(companyId, seeder.ensureSeeded(companyId));
        });
    }

    @Transactional
    public SettingsView saveFinding(UUID companyId, UUID findingId, FindingRequest request, UUID userId) {
        return inTenant(companyId, () -> {
            seeder.ensureSeeded(companyId);
            String code = requireCode(request.code());
            if (request.label() == null || request.label().isBlank() || request.label().length() > 120) {
                throw new TrichologyValidationException("Informe o nome do achado (até 120 caracteres).");
            }
            List<FindingCatalogItem> all = catalog.findings(companyId);
            UUID id = findingId;
            if (id == null) {
                if (all.stream().anyMatch(f -> f.code().equals(code))) {
                    throw new TrichologyValidationException("Já existe um achado com o código " + code + ".");
                }
                id = UUID.randomUUID();
            } else {
                UUID target = id;
                FindingCatalogItem existing = all.stream().filter(f -> f.id().equals(target)).findFirst()
                        .orElseThrow(() -> new TrichologyNotFoundException("Achado não encontrado."));
                if (!existing.code().equals(code)) {
                    throw new TrichologyValidationException("O código de um achado não pode mudar; crie outro.");
                }
            }
            catalog.saveFinding(new FindingCatalogItem(id, companyId, code, request.label().trim(),
                    request.description(), request.active()));
            audit(companyId, "TRICHOLOGY_FINDING_SAVED", userId);
            return view(companyId, seeder.ensureSeeded(companyId));
        });
    }

    @Transactional
    public SettingsView saveMarker(UUID companyId, UUID markerId, MarkerRequest request, UUID userId) {
        return inTenant(companyId, () -> {
            seeder.ensureSeeded(companyId);
            String code = requireCode(request.code());
            if (request.label() == null || request.label().isBlank()) {
                throw new TrichologyValidationException("Informe o nome do marcador.");
            }
            if (request.system() == null || request.system().isBlank()) {
                throw new TrichologyValidationException("Informe o sistema do marcador (ex.: FERRO, TIREOIDE).");
            }
            if (request.sex() != null && !List.of("F", "M").contains(request.sex())) {
                throw new TrichologyValidationException("Sexo do marcador deve ser F, M ou vazio.");
            }
            if (request.referenceMin() != null && request.referenceMax() != null
                    && request.referenceMin().compareTo(request.referenceMax()) >= 0) {
                throw new TrichologyValidationException("A referência mínima deve ser menor que a máxima.");
            }
            if (request.targetQuartile() != null && (request.targetQuartile() < 1 || request.targetQuartile() > 4)) {
                throw new TrichologyValidationException("Quartil-alvo entre 1 e 4.");
            }
            List<Marker> all = catalog.markers(companyId);
            UUID id = markerId;
            if (id == null) {
                if (all.stream().anyMatch(m -> m.code().equals(code) && java.util.Objects.equals(m.sex(),
                        request.sex()))) {
                    throw new TrichologyValidationException("Já existe o marcador " + code + " para esse sexo.");
                }
                id = UUID.randomUUID();
            } else {
                UUID target = id;
                all.stream().filter(m -> m.id().equals(target)).findFirst()
                        .orElseThrow(() -> new TrichologyNotFoundException("Marcador não encontrado."));
            }
            catalog.saveMarker(new Marker(id, companyId, code, request.label().trim(), request.unit(),
                    request.system().trim().toUpperCase(), request.sex(), request.referenceMin(),
                    request.referenceMax(), request.targetQuartile(), request.active()));
            audit(companyId, "TRICHOLOGY_MARKER_SAVED", userId);
            return view(companyId, seeder.ensureSeeded(companyId));
        });
    }

    private SettingsView view(UUID companyId, Settings s) {
        return new SettingsView(s.minConfidence(), s.reportHorizonsMonths(), s.examRules(),
                catalog.protocols(companyId), catalog.findings(companyId), catalog.markers(companyId));
    }

    private static String requireCode(String code) {
        String c = code == null ? "" : code.trim().toUpperCase();
        if (!CODE.matcher(c).matches()) {
            throw new TrichologyValidationException("Código inválido: use letras maiúsculas, números e _.");
        }
        return c;
    }

    private void audit(UUID companyId, String description, UUID userId) {
        auditor.record(companyId, AuditAction.UPDATE, AuditModule.SETTINGS, "TrichologySettings",
                companyId.toString(), description, userId, Map.of());
    }

    private <T> T inTenant(UUID companyId, Supplier<T> work) {
        access.require(companyId);
        try {
            TenantContext.setCompanyId(companyId);
            return work.get();
        } finally {
            TenantContext.clear();
        }
    }
}
