package com.becommerce.crm.masterdata.catalog.application.port.output;

import com.becommerce.crm.masterdata.catalog.domain.CatalogItem;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Porta de saída do catálogo ({@code catalog_items}, V078). RLS FORCE isola o tenant. */
public interface CatalogItemRepository {

    CatalogItem save(CatalogItem item);

    Optional<CatalogItem> findById(UUID id);

    /** Nomes já cadastrados na empresa, em minúsculas (evita duplicar a lista inicial do segmento). */
    Set<String> findLowerCaseNames(UUID companyId);

    boolean existsBySku(UUID companyId, String sku, UUID excludeId);

    /**
     * Busca paginada. {@code query} casa nome/categoria/descrição/SKU (case-insensitive);
     * {@code type} e {@code active} nulos não filtram.
     */
    PageResult search(UUID companyId, String query, CatalogItemType type, Boolean active,
                      int page, int pageSize);

    /** Produtos (itemType = PRODUCT) com estoque. {@code lowStock} filtra min_stock > 0 e quantity <= min_stock. */
    PageResult searchProducts(UUID companyId, String query, String category, Boolean lowStock,
                              Boolean active, int page, int pageSize);

    /** Lock pessimista de linha para registrar movimentação (PESSIMISTIC_WRITE). */
    Optional<CatalogItem> findByIdForUpdate(UUID id);

    record PageResult(List<CatalogItem> content, long totalElements) {}
}
