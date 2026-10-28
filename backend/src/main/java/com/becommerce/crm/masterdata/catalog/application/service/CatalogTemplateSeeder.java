package com.becommerce.crm.masterdata.catalog.application.service;

import com.becommerce.crm.masterdata.catalog.application.port.output.CatalogItemRepository;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItem;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;
import com.becommerce.crm.masterdata.company.domain.CompanySegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

    private static final String DENTAL_RESOURCE = "/catalog/procedures-dental.txt";
    private static final List<String> DENTAL_PROCEDURES = loadDental();

    private static final Map<CompanySegment, String> CATEGORIES = Map.of(
            CompanySegment.DENTAL, "Odontologia",
            CompanySegment.TRICHOLOGY, "Tricologia",
            CompanySegment.OCCUPATIONAL_SAFETY, "Segurança do trabalho");

    private final CatalogItemRepository catalogItemRepository;

    public CatalogTemplateSeeder(CatalogItemRepository catalogItemRepository) {
        this.catalogItemRepository = catalogItemRepository;
    }

    /**
     * Cria os procedimentos do segmento que a empresa ainda não tem (comparando o
     * nome sem diferenciar maiúsculas). Roda na criação da empresa e quando o
     * segmento muda; segmentos sem lista não recebem nada.
     *
     * @return quantos itens foram criados
     */
    public int seedProcedures(UUID companyId, CompanySegment segment) {
        List<String> names = procedures(segment);
        if (names.isEmpty()) {
            return 0;
        }
        Set<String> existing = new HashSet<>(catalogItemRepository.findLowerCaseNames(companyId));
        int created = 0;
        for (String name : names) {
            if (!existing.add(name.toLowerCase(Locale.ROOT))) {
                continue;
            }
            catalogItemRepository.save(CatalogItem.create(companyId, CatalogItemType.SERVICE, name, null,
                    CATEGORIES.get(segment), BigDecimal.ZERO, "BRL", null, null, null));
            created++;
        }
        log.info("Catalog template seeded for company={} segment={}: {} new items", companyId, segment, created);
        return created;
    }

    /** True se a empresa já tem ao menos um procedimento da lista do segmento. */
    public boolean hasSegmentProcedures(UUID companyId, CompanySegment segment) {
        List<String> names = procedures(segment);
        if (names.isEmpty()) {
            return true;
        }
        Set<String> existing = catalogItemRepository.findLowerCaseNames(companyId);
        return names.stream().anyMatch(name -> existing.contains(name.toLowerCase(Locale.ROOT)));
    }

    static List<String> procedures(CompanySegment segment) {
        if (segment == CompanySegment.DENTAL) {
            return DENTAL_PROCEDURES;
        }
        return PROCEDURES.getOrDefault(segment, List.of());
    }

    /** Lista odontológica (341 procedimentos) em {@code catalog/procedures-dental.txt}. */
    private static List<String> loadDental() {
        try (InputStream in = CatalogTemplateSeeder.class.getResourceAsStream(DENTAL_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Recurso ausente: " + DENTAL_RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
