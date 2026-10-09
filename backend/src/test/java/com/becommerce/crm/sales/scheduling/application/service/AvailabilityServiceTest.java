package com.becommerce.crm.sales.scheduling.application.service;

import com.becommerce.crm.sales.scheduling.application.dto.AvailabilityResponse;
import com.becommerce.crm.sales.scheduling.application.dto.SetAvailabilityRequest;
import com.becommerce.crm.sales.scheduling.application.port.out.AvailabilityRepository;
import com.becommerce.crm.sales.scheduling.domain.AvailabilityWindow;
import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock AvailabilityRepository repository;

    @InjectMocks AvailabilityService service;

    @SuppressWarnings("unchecked")
    private List<AvailabilityWindow> savedWindows(String body) throws Exception {
        when(repository.findTimezone(userId)).thenReturn(Optional.empty());
        service.set(companyId, userId, json.readValue(body, SetAvailabilityRequest.class));
        ArgumentCaptor<List<AvailabilityWindow>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).replaceWindows(eq(companyId), eq(userId), captor.capture());
        return captor.getValue();
    }

    @Test
    void set_weekdayOneFromFrontend_isMonday_andSundayIsAccepted() throws Exception {
        List<AvailabilityWindow> windows = savedWindows("""
                {"timezone":"America/Sao_Paulo","rules":[
                  {"weekday":1,"startTime":"09:00","endTime":"12:00"},
                  {"weekday":7,"startTime":"09:00","endTime":"12:00"}]}""");
        assertEquals(List.of(DayOfWeek.MONDAY, DayOfWeek.SUNDAY),
                windows.stream().map(AvailabilityWindow::weekday).toList());
    }

    @Test
    void set_duplicatedRules_areSavedOnce() throws Exception {
        List<AvailabilityWindow> windows = savedWindows("""
                {"rules":[{"weekday":2,"startTime":"14:00","endTime":"19:00"},
                          {"weekday":2,"startTime":"14:00","endTime":"19:00"}]}""");
        assertEquals(1, windows.size());
    }

    @Test
    void set_invalidWeekday_isRejectedWithoutSaving() throws Exception {
        SetAvailabilityRequest request = json.readValue(
                "{\"rules\":[{\"weekday\":8,\"startTime\":\"09:00\",\"endTime\":\"12:00\"}]}",
                SetAvailabilityRequest.class);
        assertThrows(SchedulingValidationException.class, () -> service.set(companyId, userId, request));
        verify(repository, never()).replaceWindows(any(), any(), any());
    }

    @Test
    void get_returnsIsoWeekdayNumber() throws Exception {
        when(repository.findTimezone(userId)).thenReturn(Optional.of("America/Sao_Paulo"));
        when(repository.findWindowsByUserId(companyId, userId)).thenReturn(List.of(new AvailabilityWindow(
                UUID.randomUUID(), companyId, userId, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0))));
        when(repository.findOverridesByUserId(companyId, userId)).thenReturn(List.of());
        AvailabilityResponse response = service.get(companyId, userId);
        assertTrue(json.writeValueAsString(response).contains("\"weekday\":1"));
    }
}
