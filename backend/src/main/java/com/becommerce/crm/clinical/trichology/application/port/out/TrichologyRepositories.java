package com.becommerce.crm.clinical.trichology.application.port.out;

import com.becommerce.crm.clinical.trichology.domain.AssessmentImage;
import com.becommerce.crm.clinical.trichology.domain.CaptureProtocol;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.FindingCatalogItem;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.Marker;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.Settings;
import com.becommerce.crm.clinical.trichology.domain.TrichologyAssessment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Portas de saída do módulo de tricologia. Todas as consultas exigem o TenantContext da empresa. */
public final class TrichologyRepositories {

    private TrichologyRepositories() {
    }

    public interface AssessmentRepository {
        TrichologyAssessment save(TrichologyAssessment assessment);

        Optional<TrichologyAssessment> find(UUID companyId, UUID contactId, UUID id);

        List<TrichologyAssessment> listByContact(UUID companyId, UUID contactId);

        /** Leitura pública pelo hash do token (policy quiz_token_select_policy). */
        Optional<TrichologyAssessment> findByQuizTokenHash(String tokenHash);

        /** Último exame publicado do paciente antes de {@code before} (comparação de evolução). */
        Optional<TrichologyAssessment> findPreviousPublished(UUID companyId, UUID contactId, Instant before,
                                                             UUID excludeId);

        boolean verificationCodeExists(String code);
    }

    public interface AssessmentImageRepository {
        AssessmentImage save(AssessmentImage image);

        Optional<AssessmentImage> findInAssessment(UUID companyId, UUID assessmentId, UUID id);

        Optional<AssessmentImage> findImage(UUID companyId, UUID id);

        List<AssessmentImage> listByAssessment(UUID companyId, UUID assessmentId);

        /** Imagem do tricoscópio ligada a algum exame (vira soft delete). */
        boolean isLinked(UUID companyId, UUID imageId);

        /** Imagem ligada a exame publicado (não pode nem ser ocultada). */
        boolean isLinkedToPublished(UUID companyId, UUID imageId);
    }

    public interface CatalogRepository {
        List<CaptureProtocol> protocols(UUID companyId);

        Optional<CaptureProtocol> protocol(UUID companyId, UUID id);

        CaptureProtocol saveProtocol(CaptureProtocol protocol);

        List<FindingCatalogItem> findings(UUID companyId);

        FindingCatalogItem saveFinding(FindingCatalogItem item);

        List<Marker> markers(UUID companyId);

        Marker saveMarker(Marker marker);

        Optional<Settings> settings(UUID companyId);

        Settings saveSettings(Settings settings);
    }

    /** Registro de chamadas de IA (sem imagem nem identificadores do paciente). */
    public record AiRun(UUID id, UUID companyId, UUID assessmentId, UUID assessmentImageId, String stage,
                        String model, String promptVersion, String imageSha256, Integer inputTokens,
                        Integer outputTokens, String status, String errorCode, Integer latencyMs, UUID createdBy,
                        Instant createdAt) {
    }

    public interface AiRunRepository {
        void save(AiRun run);

        List<AiRun> runsOf(UUID companyId, UUID assessmentId);
    }
}
