package com.becommerce.crm.masterdata.catalog.application.dto;

import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Criação/edição de item do catálogo (mesmo payload nos dois casos). */
public record CatalogItemRequest(
        @NotNull CatalogItemType type,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 4000) String description,
        @Size(max = 80) String category,
        @DecimalMin(value = "0.00") BigDecimal price,
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Moeda deve ter 3 letras (ex.: BRL)") String currency,
        @Size(max = 64) String sku
) {
}
