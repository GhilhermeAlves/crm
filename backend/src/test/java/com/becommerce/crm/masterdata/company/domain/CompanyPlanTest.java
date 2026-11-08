package com.becommerce.crm.masterdata.company.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CompanyPlanTest {
    @Test
    void defaultPricesInCents() {
        assertEquals(4990, CompanyPlan.ESSENTIAL.defaultMonthlyPriceCents());
        assertEquals(19700, CompanyPlan.STARTER.defaultMonthlyPriceCents());
        assertEquals(39700, CompanyPlan.PROFESSIONAL.defaultMonthlyPriceCents());
        assertEquals(79700, CompanyPlan.BUSINESS.defaultMonthlyPriceCents());
        assertEquals(150000, CompanyPlan.ENTERPRISE.defaultMonthlyPriceCents());
    }
}
