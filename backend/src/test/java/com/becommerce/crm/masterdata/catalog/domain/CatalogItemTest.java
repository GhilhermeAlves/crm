package com.becommerce.crm.masterdata.catalog.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogItemTest {

    private static final UUID COMPANY = UUID.randomUUID();

    @Test
    void createNormalizesFieldsAndStartsActive() {
        CatalogItem item = CatalogItem.create(COMPANY, CatalogItemType.PRODUCT, "  Plano Pro  ", " ",
                "  Assinaturas ", new BigDecimal("99.90"), "brl", "  ", null, null);

        assertEquals("Plano Pro", item.getName());
        assertNull(item.getDescription());
        assertEquals("Assinaturas", item.getCategory());
        assertEquals("BRL", item.getCurrency());
        assertNull(item.getSku());
        assertTrue(item.isActive());
    }

    @Test
    void currencyDefaultsToBrl() {
        CatalogItem item = CatalogItem.create(COMPANY, CatalogItemType.SERVICE, "Consultoria", null, null,
                null, null, null, null, null);
        assertEquals("BRL", item.getCurrency());
        assertNull(item.getPrice());
    }

    @Test
    void rejectsBlankNameAndNegativePrice() {
        assertThrows(IllegalArgumentException.class, () -> CatalogItem.create(COMPANY,
                CatalogItemType.PRODUCT, " ", null, null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> CatalogItem.create(COMPANY,
                CatalogItemType.PRODUCT, "X", null, null, new BigDecimal("-1"), null, null, null, null));
    }

    @Test
    void deactivateAndActivate() {
        CatalogItem item = CatalogItem.create(COMPANY, CatalogItemType.PRODUCT, "X", null, null, null, null,
                null, null, null);
        item.deactivate();
        assertFalse(item.isActive());
        item.activate();
        assertTrue(item.isActive());
    }

    @Test
    void createProductNormalizesStockFields() {
        CatalogItem item = CatalogItem.create(COMPANY, CatalogItemType.PRODUCT, "Luva", null,
                "EPI", new BigDecimal("10.00"), "BRL", null, "CAIXA", 5);
        assertEquals("CAIXA", item.getUnit());
        assertEquals(5, item.getMinStock());
        assertEquals(0, item.getQuantity());
    }

    @Test
    void updateQuantityRejectsNegative() {
        CatalogItem item = CatalogItem.create(COMPANY, CatalogItemType.PRODUCT, "A", null, null, null,
                null, null, null, 0);
        assertThrows(IllegalArgumentException.class, () -> item.updateQuantity(-1));
    }
}
