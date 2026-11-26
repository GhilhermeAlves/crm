package com.becommerce.crm.clinical.trichology.domain;

/**
 * Achado tricoscópico numa imagem. Vindo da IA é só proposta; entra no
 * relatório apenas depois de aceito pelo profissional ({@code accepted_findings}).
 *
 * @param code       código do catálogo da empresa (trichology_finding_catalog)
 * @param severity   MILD, MODERATE ou SEVERE
 * @param confidence 0..1 (nulo quando lançado pelo profissional)
 * @param source     AI ou PROFESSIONAL
 */
public record Finding(String code, String label, String severity, Double confidence, String evidence,
                      String source) {

    public static final String SOURCE_AI = "AI";
    public static final String SOURCE_PROFESSIONAL = "PROFESSIONAL";
}
