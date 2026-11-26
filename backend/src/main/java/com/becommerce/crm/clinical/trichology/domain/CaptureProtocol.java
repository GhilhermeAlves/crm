package com.becommerce.crm.clinical.trichology.domain;

import com.becommerce.crm.clinical.trichology.domain.exception.TrichologyValidationException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** Protocolo de captura da clínica: regiões do couro cabeludo, magnificações e mínimo de fotos. */
public record CaptureProtocol(UUID id, UUID companyId, String name, List<Region> regions,
                              boolean isDefault, boolean active) {

    private static final Pattern CODE = Pattern.compile("^[A-Z0-9_]{2,40}$");
    private static final Pattern MAGNIFICATION = Pattern.compile("^[0-9]{1,4}x$");

    public record Region(String code, String label, List<String> magnifications, int minImages) {
    }

    public CaptureProtocol {
        if (name == null || name.isBlank() || name.length() > 120) {
            throw new TrichologyValidationException("Informe o nome do protocolo (até 120 caracteres).");
        }
        name = name.trim();
        if (regions == null || regions.isEmpty()) {
            throw new TrichologyValidationException("O protocolo precisa de pelo menos uma região.");
        }
        Set<String> codes = new HashSet<>();
        for (Region region : regions) {
            if (region.code() == null || !CODE.matcher(region.code()).matches()) {
                throw new TrichologyValidationException("Código de região inválido: " + region.code());
            }
            if (!codes.add(region.code())) {
                throw new TrichologyValidationException("Região repetida no protocolo: " + region.code());
            }
            if (region.label() == null || region.label().isBlank()) {
                throw new TrichologyValidationException("Informe o nome da região " + region.code() + ".");
            }
            if (region.minImages() < 0 || region.minImages() > 20) {
                throw new TrichologyValidationException("Mínimo de fotos por região deve ficar entre 0 e 20.");
            }
            for (String m : region.magnifications() == null ? List.<String>of() : region.magnifications()) {
                if (!MAGNIFICATION.matcher(m).matches()) {
                    throw new TrichologyValidationException("Magnificação inválida: " + m + " (use 20x, 50x...).");
                }
            }
        }
        regions = List.copyOf(regions);
    }

    public Optional<Region> region(String code) {
        return regions.stream().filter(r -> r.code().equals(code)).findFirst();
    }

    /** Protocolo padrão semeado para empresas TRICHOLOGY. */
    public static CaptureProtocol standard(UUID companyId) {
        List<String> mags = List.of("20x", "50x");
        return new CaptureProtocol(UUID.randomUUID(), companyId, "Protocolo padrão", List.of(
                new Region("FRONTAL", "Frontal", mags, 2),
                new Region("VERTEX", "Vértice (coroa)", mags, 2),
                new Region("OCCIPITAL", "Occipital (referência)", mags, 2),
                new Region("TEMPORAL_LEFT", "Temporal esquerda", mags, 1),
                new Region("TEMPORAL_RIGHT", "Temporal direita", mags, 1),
                new Region("HAIRLINE", "Linha de implantação", mags, 1),
                new Region("COMPLAINT_AREA", "Área da queixa", mags, 0)), true, true);
    }
}
