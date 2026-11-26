package com.becommerce.crm.clinical.trichology.application.service;

import com.becommerce.crm.clinical.trichology.application.port.out.TrichologyRepositories.CatalogRepository;
import com.becommerce.crm.clinical.trichology.domain.CaptureProtocol;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.FindingCatalogItem;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.Marker;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.Settings;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Configuração inicial por empresa, criada na primeira vez que o módulo é usado
 * (a presença de {@code trichology_settings} marca que já foi semeado). Roda
 * dentro da transação e do TenantContext de quem chamou.
 */
@Component
public class TrichologySeeder {

    private final CatalogRepository catalog;
    private final TrichologyAccess access;

    public TrichologySeeder(CatalogRepository catalog, TrichologyAccess access) {
        this.catalog = catalog;
        this.access = access;
    }

    public Settings ensureSeeded(UUID companyId) {
        return catalog.settings(companyId).orElseGet(() -> seed(companyId));
    }

    private Settings seed(UUID companyId) {
        if (catalog.protocols(companyId).isEmpty()) {
            catalog.saveProtocol(CaptureProtocol.standard(companyId));
        }
        if (catalog.findings(companyId).isEmpty()) {
            for (String[] f : CatalogItems.defaultFindings()) {
                catalog.saveFinding(new FindingCatalogItem(UUID.randomUUID(), companyId, f[0], f[1], null, true));
            }
        }
        if (catalog.markers(companyId).isEmpty()) {
            for (Object[] m : CatalogItems.defaultMarkers()) {
                catalog.saveMarker(new Marker(UUID.randomUUID(), companyId, (String) m[0], (String) m[1],
                        (String) m[2], (String) m[3], (String) m[4], new BigDecimal((String) m[5]),
                        new BigDecimal((String) m[6]), (Integer) m[7], true));
            }
        }
        return catalog.saveSettings(new Settings(companyId, access.defaultMinConfidence(), List.of(3, 6, 9, 12),
                CatalogItems.defaultExamRules()));
    }
}
