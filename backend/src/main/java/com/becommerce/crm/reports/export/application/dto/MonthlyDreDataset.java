package com.becommerce.crm.reports.export.application.dto;

import java.util.List;

/**
 * DRE simplificada do mês (regime de caixa). {@code groups} vem na ordem de
 * apresentação; o primeiro grupo é o de receitas e os demais são subtraídos.
 */
public record MonthlyDreDataset(List<DreGroup> groups, long resultCents) {

    public record DreGroup(String label, boolean revenue, List<DreLine> lines) {
        public long totalCents() {
            return lines.stream().mapToLong(DreLine::totalCents).sum();
        }

        public long count() {
            return lines.stream().mapToLong(DreLine::count).sum();
        }
    }

    public record DreLine(String label, long count, long totalCents) {}
}
