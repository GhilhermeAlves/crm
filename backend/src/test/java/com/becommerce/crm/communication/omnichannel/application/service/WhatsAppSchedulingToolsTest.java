package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.communication.notification.application.dto.CreateNotificationRequest;
import com.becommerce.crm.communication.notification.application.port.input.NotificationUseCase;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.sales.scheduling.application.dto.AppointmentResponse;
import com.becommerce.crm.sales.scheduling.application.dto.CreateAppointmentRequest;
import com.becommerce.crm.sales.scheduling.application.dto.SlotResponse;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentUseCase;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentTypeRepository;
import com.becommerce.crm.sales.scheduling.application.service.SlotService;
import com.becommerce.crm.sales.scheduling.domain.AppointmentSource;
import com.becommerce.crm.sales.scheduling.domain.AppointmentType;
import com.becommerce.crm.sales.scheduling.domain.AssignmentMode;
import com.becommerce.crm.sales.scheduling.domain.LocationKind;
import com.becommerce.crm.sales.scheduling.domain.exception.SlotUnavailableException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WhatsAppSchedulingToolsTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final AppointmentTypeRepository typeRepository = mock(AppointmentTypeRepository.class);
    private final SlotService slotService = mock(SlotService.class);
    private final AppointmentUseCase appointmentUseCase = mock(AppointmentUseCase.class);
    private final ContactRepository contactRepository = mock(ContactRepository.class);
    private final NotificationUseCase notificationUseCase = mock(NotificationUseCase.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final WhatsAppSchedulingTools tools = new WhatsAppSchedulingTools(typeRepository, slotService,
            appointmentUseCase, contactRepository, notificationUseCase, userRepository);

    private final UUID companyId = UUID.randomUUID();
    private final UUID hostA = UUID.randomUUID();
    private final UUID hostB = UUID.randomUUID();
    private final WhatsAppSchedulingTools.Context ctx =
            new WhatsAppSchedulingTools.Context(companyId, UUID.randomUUID(), "5534999998888", "Maria");
    private AppointmentType avaliacao;

    @BeforeEach
    void setUp() {
        avaliacao = AppointmentType.reconstitute(UUID.randomUUID(), companyId, "Avaliação", "avaliacao", null, 60,
                0, 0, 0, 60, 60, null, LocationKind.IN_PERSON, null, AssignmentMode.ROUND_ROBIN, false, true,
                List.of(hostA, hostB), Instant.now(), Instant.now());
        when(typeRepository.findByCompanyId(companyId)).thenReturn(List.of(avaliacao));
        when(contactRepository.findByCompanyIdAndPhone(any(), any())).thenReturn(Optional.empty());
        when(userRepository.findById(any())).thenReturn(Optional.empty());
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private static Instant at(LocalDate day, int hour, int minute) {
        return day.atTime(hour, minute).atZone(ZONE).toInstant();
    }

    private static AiProvider.ToolCall call(String name, Map<String, Object> args) {
        return new AiProvider.ToolCall("call-1", name, args);
    }

    @Test
    void guidanceEDefinicoes_listamTiposEFerramentas() {
        assertTrue(tools.guidance(tools.bookableTypes(companyId)).contains("Avaliação (1h)"));
        assertEquals(List.of(WhatsAppSchedulingTools.CHECK_SLOTS, WhatsAppSchedulingTools.BOOK),
                tools.definitions().stream().map(AiProvider.ToolDefinition::name).toList());
    }

    @Test
    void consultar_filtraPorPeriodoEAceitaNomeSemAcento() {
        LocalDate day = LocalDate.now(ZONE).plusDays(1);
        when(slotService.getAvailableSlots(eq(companyId), eq(avaliacao.getId()), any(), any())).thenReturn(List.of(
                new SlotResponse(at(day, 9, 0), at(day, 10, 0)),
                new SlotResponse(at(day, 15, 0), at(day, 16, 0))));

        String out = tools.execute(ctx, call(WhatsAppSchedulingTools.CHECK_SLOTS,
                Map.of("tipo_consulta", "avaliacao", "data_inicial", day.toString(), "periodo", "tarde")));

        assertTrue(out.contains("15:00"), out);
        assertFalse(out.contains("09:00"), out);
        assertTrue(out.contains(day + "T15:00"), out);
        assertEquals(companyId, TenantContext.getCompanyId());
    }

    @Test
    void consultar_semHorarios_sugereOutroPeriodo() {
        when(slotService.getAvailableSlots(any(), any(), any(), any())).thenReturn(List.of());

        String out = tools.execute(ctx, call(WhatsAppSchedulingTools.CHECK_SLOTS, Map.of("tipo_consulta", "Avaliação")));

        assertTrue(out.startsWith("Sem horários livres"), out);
    }

    @Test
    void agendar_criaComOrigemWhatsappEAvisaProfissional() {
        LocalDate day = LocalDate.now(ZONE).plusDays(2);
        Instant start = at(day, 14, 0);
        when(slotService.getAvailableSlots(any(), any(), eq(day), eq(day)))
                .thenReturn(List.of(new SlotResponse(start, start.plusSeconds(3600))));
        AppointmentResponse created = mock(AppointmentResponse.class);
        when(created.id()).thenReturn(UUID.randomUUID());
        when(appointmentUseCase.create(eq(companyId), any(), isNull(), eq(AppointmentSource.WHATSAPP)))
                .thenReturn(created);

        String out = tools.execute(ctx, call(WhatsAppSchedulingTools.BOOK, Map.of("tipo_consulta", "Avaliação",
                "inicio", LocalDateTime.ofInstant(start, ZONE).toString().substring(0, 16), "nome_paciente", "Maria Souza")));

        assertTrue(out.startsWith("Agendado com sucesso"), out);
        ArgumentCaptor<CreateAppointmentRequest> req = ArgumentCaptor.forClass(CreateAppointmentRequest.class);
        verify(appointmentUseCase).create(eq(companyId), req.capture(), isNull(), eq(AppointmentSource.WHATSAPP));
        assertEquals(hostA, req.getValue().hostId());
        assertEquals(start, req.getValue().startAt());
        assertEquals(start.plusSeconds(3600), req.getValue().endAt());
        assertTrue(req.getValue().title().contains("Maria Souza"));
        verify(notificationUseCase).create(eq(companyId), any(CreateNotificationRequest.class), any());
    }

    @Test
    void agendar_primeiroProfissionalOcupado_tentaOProximo() {
        LocalDate day = LocalDate.now(ZONE).plusDays(2);
        Instant start = at(day, 9, 0);
        when(slotService.getAvailableSlots(any(), any(), eq(day), eq(day)))
                .thenReturn(List.of(new SlotResponse(start, start.plusSeconds(3600))));
        AppointmentResponse created = mock(AppointmentResponse.class);
        when(created.id()).thenReturn(UUID.randomUUID());
        when(appointmentUseCase.create(eq(companyId), any(), isNull(), eq(AppointmentSource.WHATSAPP)))
                .thenThrow(new SlotUnavailableException())
                .thenReturn(created);

        String out = tools.execute(ctx, call(WhatsAppSchedulingTools.BOOK, Map.of("tipo_consulta", "Avaliação",
                "inicio", day + "T09:00", "nome_paciente", "Maria")));

        assertTrue(out.startsWith("Agendado com sucesso"), out);
        verify(appointmentUseCase, times(2)).create(eq(companyId), any(), isNull(), eq(AppointmentSource.WHATSAPP));
    }

    @Test
    void agendar_horarioQueNaoEstaLivre_naoCria() {
        LocalDate day = LocalDate.now(ZONE).plusDays(2);
        when(slotService.getAvailableSlots(any(), any(), eq(day), eq(day))).thenReturn(List.of());

        String out = tools.execute(ctx, call(WhatsAppSchedulingTools.BOOK, Map.of("tipo_consulta", "Avaliação",
                "inicio", day + "T09:00", "nome_paciente", "Maria")));

        assertTrue(out.contains("não está mais disponível"), out);
        verify(appointmentUseCase, never()).create(any(), any(), any(), any());
    }

    @Test
    void agendar_horarioMalFormatado_pedeFormatoCorreto() {
        String out = tools.execute(ctx, call(WhatsAppSchedulingTools.BOOK,
                Map.of("tipo_consulta", "Avaliação", "inicio", "amanhã às 9", "nome_paciente", "Maria")));

        assertTrue(out.startsWith("Horário inválido"), out);
    }

    @Test
    void formatDuration_e_periodo() {
        assertEquals("1h30", WhatsAppSchedulingTools.formatDuration(90));
        assertEquals("45 min", WhatsAppSchedulingTools.formatDuration(45));
        assertTrue(WhatsAppSchedulingTools.inPeriod(LocalDateTime.of(2026, 10, 9, 9, 0), "manhã"));
        assertFalse(WhatsAppSchedulingTools.inPeriod(LocalDateTime.of(2026, 10, 9, 9, 0), "tarde"));
    }
}
