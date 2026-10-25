package com.becommerce.crm.masterdata.catalog.application.service;

import com.becommerce.crm.masterdata.catalog.application.port.output.CatalogItemRepository;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItem;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;
import com.becommerce.crm.masterdata.company.domain.CompanySegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lista inicial de procedimentos por segmento, criada no catálogo quando a
 * empresa é provisionada (como o {@code WorkflowTemplateSeeder}). Os itens são
 * serviços com preço zero: cada empresa ajusta preços e a lista depois.
 */
@Service
public class CatalogTemplateSeeder {

    private static final Logger log = LoggerFactory.getLogger(CatalogTemplateSeeder.class);

    static final Map<CompanySegment, List<String>> PROCEDURES = Map.of(
            CompanySegment.TRICHOLOGY, List.of(
                    "Consulta tricológica e anamnese",
                    "Tricoscopia digital e manual",
                    "Teste de tração (Pull test)",
                    "Biópsia de couro cabeludo",
                    "Tricograma",
                    "Microagulhamento capilar",
                    "Microinfusão de medicamentos (MMP)",
                    "Intradermoterapia capilar (Mesoterapia)",
                    "Terapia com LED / Laser de baixa intensidade",
                    "Peeling químico de couro cabeludo",
                    "Luz Pulsada Intensa (IPL)",
                    "Transplante capilar (FUE / FUT)",
                    "Terapia fotodinâmica (PDT)"),
            CompanySegment.OCCUPATIONAL_SAFETY, List.of(
                    "Elaboração e atualização do PGR",
                    "Gestão do PCMSO e ASO",
                    "Emissão e controle de CAT",
                    "LTCAT e laudos ambientais",
                    "Especificação de EPIs",
                    "Controle de estoque, entrega e CA de EPIs",
                    "Inspeção de EPCs",
                    "Treinamento admissional (integração)",
                    "Treinamentos periódicos de NRs",
                    "Organização da SIPAT",
                    "Processo eleitoral da CIPA",
                    "Capacitação da CIPA",
                    "Reuniões e atas da CIPA",
                    "Inspeções de rotina e checklists",
                    "Permissão de Trabalho (PT)",
                    "Análise Preliminar de Risco (APR)",
                    "Atendimento inicial a emergências",
                    "Investigação de acidentes",
                    "Plano de ação corretiva e preventiva"));

    private static final Map<CompanySegment, String> CATEGORIES = Map.of(
            CompanySegment.TRICHOLOGY, "Tricologia",
            CompanySegment.OCCUPATIONAL_SAFETY, "Segurança do trabalho");

    private final CatalogItemRepository catalogItemRepository;

    public CatalogTemplateSeeder(CatalogItemRepository catalogItemRepository) {
        this.catalogItemRepository = catalogItemRepository;
    }

    /** Cria os procedimentos do segmento; segmentos sem lista não recebem nada. */
    public void seedProcedures(UUID companyId, CompanySegment segment) {
        List<String> names = PROCEDURES.getOrDefault(segment, List.of());
        for (String name : names) {
            catalogItemRepository.save(CatalogItem.create(companyId, CatalogItemType.SERVICE, name, null,
                    CATEGORIES.get(segment), BigDecimal.ZERO, "BRL", null, null, null));
        }
        if (!names.isEmpty()) {
            log.info("Catalog template seeded for company={} segment={}: {} items", companyId, segment, names.size());
        }
    }
}
