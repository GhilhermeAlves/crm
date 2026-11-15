package com.becommerce.crm.masterdata.company.domain;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyPlanModulesTest {

    @Test
    void essentialHasOnlyTheClinicBasics() {
        assertEquals(EnumSet.of(
                CompanyModule.PATIENTS, CompanyModule.AGENDA, CompanyModule.AGENDA_SETTINGS,
                CompanyModule.CATALOG, CompanyModule.DOCUMENTS,
                CompanyModule.TASKS, CompanyModule.STORAGE), CompanyPlan.ESSENTIAL.defaultModules());
    }

    @Test
    void eachPlanIncludesThePreviousOne() {
        CompanyPlan[] plans = CompanyPlan.values();
        for (int i = 1; i < plans.length; i++) {
            assertTrue(plans[i].defaultModules().containsAll(plans[i - 1].defaultModules()),
                    plans[i] + " deveria incluir os módulos de " + plans[i - 1]);
        }
    }

    @Test
    void featuresArriveAtTheProposedPlan() {
        assertTrue(CompanyPlan.STARTER.defaultModules().contains(CompanyModule.FINANCE));
        assertFalse(CompanyPlan.STARTER.defaultModules().contains(CompanyModule.INBOX));
        assertTrue(CompanyPlan.PROFESSIONAL.defaultModules().contains(CompanyModule.INBOX));
        assertFalse(CompanyPlan.PROFESSIONAL.defaultModules().contains(CompanyModule.WORKFLOWS));
        assertTrue(CompanyPlan.BUSINESS.defaultModules().contains(CompanyModule.AI_AGENT));
        assertEquals(EnumSet.allOf(CompanyModule.class), CompanyPlan.ENTERPRISE.defaultModules());
    }

    @Test
    void returnedSetIsACopy() {
        CompanyPlan.ESSENTIAL.defaultModules().add(CompanyModule.AUDIT);
        assertFalse(CompanyPlan.ESSENTIAL.defaultModules().contains(CompanyModule.AUDIT));
    }
}
