package com.becommerce.crm.automation.campaign.infrastructure.audience;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudienceResolverImplTest {

    @Test
    void selectedContactIds_readsValidIdsAndSkipsInvalid() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        String json = "{\"contactIds\":[\"" + a + "\",\"nao-e-uuid\",\"" + b + "\"]}";

        assertEquals(List.of(a, b), AudienceResolverImpl.selectedContactIds(json));
    }

    @Test
    void selectedContactIds_emptyForMissingOrMalformedCriteria() {
        assertTrue(AudienceResolverImpl.selectedContactIds(null).isEmpty());
        assertTrue(AudienceResolverImpl.selectedContactIds("{}").isEmpty());
        assertTrue(AudienceResolverImpl.selectedContactIds("{quebrado").isEmpty());
    }
}
