package com.becommerce.crm.sales.scheduling.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.sales.scheduling.application.dto.*;
import com.becommerce.crm.sales.scheduling.application.port.out.ScheduleBlockRepository;
import com.becommerce.crm.sales.scheduling.domain.ScheduleBlock;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BlockServiceTest {

    private final ScheduleBlockRepository repository = mock(ScheduleBlockRepository.class);
    private final BlockService service = new BlockService(repository, mock(TenantAuditRecorder.class));
    private final Instant start = Instant.parse("2030-03-04T12:00:00Z");

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void singleBlockHasNoGroup() {
        service.create(UUID.randomUUID(), new CreateBlockRequest(UUID.randomUUID(), start,
                start.plus(1, ChronoUnit.HOURS), "Almoço", null), null);
        ArgumentCaptor<ScheduleBlock> captor = ArgumentCaptor.forClass(ScheduleBlock.class);
        verify(repository).save(captor.capture());
        assertNull(captor.getValue().getRecurrenceGroupId());
    }

    @Test
    void recurrenceCreatesAllWithSameGroup() {
        BlockResponse r = service.create(UUID.randomUUID(), new CreateBlockRequest(UUID.randomUUID(), start,
                start.plus(1, ChronoUnit.HOURS), "Almoço",
                new RecurrenceRequest(RecurrenceFrequency.DAILY, 1, RecurrenceEnd.AFTER_COUNT, null, 3)), null);
        ArgumentCaptor<ScheduleBlock> captor = ArgumentCaptor.forClass(ScheduleBlock.class);
        verify(repository, times(3)).save(captor.capture());
        List<ScheduleBlock> saved = captor.getAllValues();
        assertNotNull(saved.get(0).getRecurrenceGroupId());
        assertTrue(saved.stream().allMatch(b -> saved.get(0).getRecurrenceGroupId().equals(b.getRecurrenceGroupId())));
        assertEquals(start, r.startAt());
    }
}
