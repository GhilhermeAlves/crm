package com.becommerce.crm.clinical.trichology.application.service;

import com.becommerce.crm.clinical.trichology.domain.exception.TrichologyNotFoundException;
import com.becommerce.crm.masterdata.company.application.service.CompanyModulesService;
import com.becommerce.crm.masterdata.company.domain.CompanyModule;
import com.becommerce.crm.masterdata.company.domain.CompanySegment;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Guarda e configuração do módulo (doc v1.2, seções 7.1 e 8).
 * <ul>
 *   <li>{@code app.trichology.enabled=false}: tudo responde 404;</li>
 *   <li>empresa precisa do segmento TRICHOLOGY e do módulo TRICHOLOGY ligado;</li>
 *   <li>CRM_ADMIN e o dono da plataforma passam pela guarda de segmento/módulo.</li>
 * </ul>
 */
@Component
public class TrichologyAccess {

    static final String CRM_ADMIN = "CRM_ADMIN";

    private final CompanyModulesService modules;
    private final boolean enabled;
    private final boolean aiEnabled;
    private final String visionModel;
    private final String textModel;
    private final BigDecimal defaultMinConfidence;
    private final int timeoutSeconds;
    private final int maxRetries;
    private final int quizLinkDays;

    public TrichologyAccess(CompanyModulesService modules,
                            @Value("${app.trichology.enabled:false}") boolean enabled,
                            @Value("${app.trichology.ai.enabled:true}") boolean aiEnabled,
                            @Value("${app.trichology.ai.vision-model:}") String visionModel,
                            @Value("${app.trichology.ai.text-model:}") String textModel,
                            @Value("${app.trichology.ai.min-confidence:0.6}") BigDecimal defaultMinConfidence,
                            @Value("${app.trichology.ai.timeout-seconds:30}") int timeoutSeconds,
                            @Value("${app.trichology.ai.max-retries:2}") int maxRetries,
                            @Value("${app.trichology.quiz-link-days:7}") int quizLinkDays) {
        this.modules = modules;
        this.enabled = enabled;
        this.aiEnabled = aiEnabled;
        this.visionModel = blankToNull(visionModel);
        this.textModel = blankToNull(textModel);
        this.defaultMinConfidence = defaultMinConfidence;
        this.timeoutSeconds = timeoutSeconds;
        this.maxRetries = maxRetries;
        this.quizLinkDays = quizLinkDays;
    }

    /** Recusa quando o módulo está desligado, a empresa não é de tricologia ou o módulo não está ativo. */
    public void require(UUID companyId) {
        requireEnabled();
        if (isPrivileged()) {
            return;
        }
        if (modules.segmentOf(companyId) != CompanySegment.TRICHOLOGY) {
            throw new AccessDeniedException("Recurso exclusivo de clínicas de tricologia/medicina capilar.");
        }
        if (!modules.isEnabled(companyId, CompanyModule.TRICHOLOGY)) {
            throw new AccessDeniedException("O módulo de Tricologia não está ativo para esta empresa.");
        }
    }

    /** Rotas públicas (quiz e verificação) só exigem o módulo ligado no servidor. */
    public void requireEnabled() {
        if (!enabled) {
            throw new TrichologyNotFoundException("Recurso não encontrado.");
        }
    }

    private static boolean isPrivileged() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof CurrentUser user
                && (user.isPlatformAdmin() || user.roles().contains(CRM_ADMIN));
    }

    public boolean aiEnabled() { return aiEnabled; }
    public String visionModel() { return visionModel; }
    public String textModel() { return textModel; }
    public BigDecimal defaultMinConfidence() { return defaultMinConfidence; }
    public int timeoutSeconds() { return timeoutSeconds; }
    public int maxRetries() { return maxRetries; }
    public int quizLinkDays() { return quizLinkDays; }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
