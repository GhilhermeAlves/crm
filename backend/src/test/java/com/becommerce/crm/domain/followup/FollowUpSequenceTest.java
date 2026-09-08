package com.becommerce.crm.domain.followup;

import com.becommerce.crm.domain.followup.exception.FollowUpSequenceValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FollowUpSequenceTest {

    private final UUID companyId = UUID.randomUUID();

    @Test
    void create_shouldCreateActiveSequence() {
        FollowUpSequence s = FollowUpSequence.create(companyId, "Carrinho abandonado", "Follow-up de recuperação");

        assertNotNull(s.getId());
        assertEquals(companyId, s.getCompanyId());
        assertEquals("Carrinho abandonado", s.getName());
        assertEquals(FollowUpSequenceStatus.ACTIVE, s.getStatus());
        assertTrue(s.isActive());
    }

    @Test
    void create_blankName_shouldThrow() {
        assertThrows(FollowUpSequenceValidationException.class,
                () -> FollowUpSequence.create(companyId, "  ", null));
        assertThrows(FollowUpSequenceValidationException.class,
                () -> FollowUpSequence.create(companyId, null, null));
    }

    @Test
    void create_nameTooLong_shouldThrow() {
        assertThrows(FollowUpSequenceValidationException.class,
                () -> FollowUpSequence.create(companyId, "x".repeat(121), null));
    }

    @Test
    void update_shouldChangeNameAndDescriptionAndTrim() {
        FollowUpSequence s = FollowUpSequence.create(companyId, "Seq", null);

        s.update("Nova seq", "   desc   ");

        assertEquals("Nova seq", s.getName());
        assertEquals("desc", s.getDescription());
        assertNull(FollowUpSequence.create(companyId, "Sem desc", "   ").getDescription());
    }

    @Test
    void update_blankName_shouldThrow() {
        FollowUpSequence s = FollowUpSequence.create(companyId, "Seq", null);
        assertThrows(FollowUpSequenceValidationException.class,
                () -> s.update(" ", null));
    }

    @Test
    void activateDeactivate_shouldChangeStatus() {
        FollowUpSequence s = FollowUpSequence.create(companyId, "Seq", null);
        assertEquals(FollowUpSequenceStatus.ACTIVE, s.getStatus());
        s.deactivate();
        assertEquals(FollowUpSequenceStatus.INACTIVE, s.getStatus());
        assertFalse(s.isActive());
        s.activate();
        assertEquals(FollowUpSequenceStatus.ACTIVE, s.getStatus());
        assertTrue(s.isActive());
    }

    @Test
    void reconstitute_shouldRestoreValues() {
        FollowUpSequence original = FollowUpSequence.create(companyId, "Seq", "desc");
        FollowUpSequence restored = FollowUpSequence.reconstitute(
                original.getId(), original.getCompanyId(), original.getName(), original.getDescription(),
                original.getStatus(), original.getCreatedAt(), original.getUpdatedAt());

        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getStatus(), restored.getStatus());
    }
}