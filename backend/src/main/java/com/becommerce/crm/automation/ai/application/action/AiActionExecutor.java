package com.becommerce.crm.automation.ai.application.action;

import com.becommerce.crm.sales.activity.application.dto.CreateActivityRequest;
import com.becommerce.crm.sales.activity.application.port.in.ActivityUseCase;
import com.becommerce.crm.finance.transaction.application.dto.CreateFinancialTransactionRequest;
import com.becommerce.crm.finance.transaction.application.dto.PayTransactionRequest;
import com.becommerce.crm.finance.transaction.application.service.FinancialTransactionService;
import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.clinical.procedure.application.dto.PatientProcedureRequest;
import com.becommerce.crm.clinical.procedure.application.dto.ProcedureToothDto;
import com.becommerce.crm.clinical.procedure.application.port.in.PatientProcedureUseCase;
import com.becommerce.crm.clinical.procedure.domain.Dentition;
import com.becommerce.crm.clinical.procedure.domain.ProcedureState;
import com.becommerce.crm.masterdata.contact.application.dto.request.CreateContactRequest;
import com.becommerce.crm.masterdata.contact.application.port.in.ContactUseCase;
import com.becommerce.crm.masterdata.inventory.application.dto.RegisterMovementRequest;
import com.becommerce.crm.masterdata.inventory.application.service.InventoryService;
import com.becommerce.crm.masterdata.inventory.domain.StockMovementType;
import com.becommerce.crm.sales.pipeline.application.dto.UpdateOpportunityRequest;
import com.becommerce.crm.sales.pipeline.application.port.in.OpportunityUseCase;
import com.becommerce.crm.sales.task.application.dto.CreateTaskRequest;
import com.becommerce.crm.sales.task.application.port.in.TaskUseCase;
import com.becommerce.crm.sales.activity.domain.ActivityType;
import com.becommerce.crm.automation.ai.domain.AiAction;
import com.becommerce.crm.sales.task.domain.TaskPriority;
import com.becommerce.crm.sales.scheduling.application.dto.CreateAppointmentRequest;
import com.becommerce.crm.sales.scheduling.application.dto.RescheduleRequest;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Executor de acoes de escrita do assistente de IA (AI-05). Dispara a acao
 * confirmada para o service de dominio correspondente (Task/Activity/
 * Opportunity), reutilizando validacoes, tenant, auditoria e eventos ja
 * existentes. Os parametros tipados sao lidos da proposta PERSISTIDA (JSONB) -
 * nunca de entrada direta do LLM. Retorna o resultado estruturado do dominio.
 */
@Component
public class AiActionExecutor {

    private final TaskUseCase taskUseCase;
    private final ActivityUseCase activityUseCase;
    private final OpportunityUseCase opportunityUseCase;
    private final AppointmentUseCase appointmentUseCase;
    private final FinancialTransactionService financialTransactionService;
    private final InventoryService inventoryService;
    private final ContactUseCase contactUseCase;
    private final PatientProcedureUseCase procedureUseCase;

    /** Sem cadastro de paciente (mantido para os testes existentes). */
    public AiActionExecutor(TaskUseCase taskUseCase,
                            ActivityUseCase activityUseCase,
                            OpportunityUseCase opportunityUseCase,
                            AppointmentUseCase appointmentUseCase,
                            FinancialTransactionService financialTransactionService,
                            InventoryService inventoryService) {
        this(taskUseCase, activityUseCase, opportunityUseCase, appointmentUseCase,
                financialTransactionService, inventoryService, null);
    }

    /** Sem lancamento de procedimentos (mantido para os testes existentes). */
    public AiActionExecutor(TaskUseCase taskUseCase,
                            ActivityUseCase activityUseCase,
                            OpportunityUseCase opportunityUseCase,
                            AppointmentUseCase appointmentUseCase,
                            FinancialTransactionService financialTransactionService,
                            InventoryService inventoryService,
                            ContactUseCase contactUseCase) {
        this(taskUseCase, activityUseCase, opportunityUseCase, appointmentUseCase,
                financialTransactionService, inventoryService, contactUseCase, null);
    }

    @Autowired
    public AiActionExecutor(TaskUseCase taskUseCase,
                            ActivityUseCase activityUseCase,
                            OpportunityUseCase opportunityUseCase,
                            AppointmentUseCase appointmentUseCase,
                            FinancialTransactionService financialTransactionService,
                            InventoryService inventoryService,
                            ContactUseCase contactUseCase,
                            PatientProcedureUseCase procedureUseCase) {
        this.taskUseCase = taskUseCase;
        this.activityUseCase = activityUseCase;
        this.opportunityUseCase = opportunityUseCase;
        this.appointmentUseCase = appointmentUseCase;
        this.financialTransactionService = financialTransactionService;
        this.inventoryService = inventoryService;
        this.contactUseCase = contactUseCase;
        this.procedureUseCase = procedureUseCase;
    }

    /**
     * Transação própria: se o service de domínio falhar (conflito de horário,
     * registro inexistente), só esta escrita é desfeita e o {@code AiActionService}
     * consegue gravar a ação como FAILED em vez de estourar UnexpectedRollbackException.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Object execute(AiAction action) {
        Map<String, Object> p = action.getParameters();
        return switch (action.getTool()) {
            case "create_task" -> taskUseCase.create(
                    action.getCompanyId(),
                    new CreateTaskRequest(
                            uuid(p.get("contactId")),
                            uuid(p.get("opportunityId")),
                            str(p.get("title")),
                            str(p.get("description")),
                            uuid(p.get("assigneeId")),
                            localDateTime(p.get("dueAt")),
                            enumOf(p.get("priority"), TaskPriority.class)),
                    action.getUserId());
            case "create_activity" -> activityUseCase.create(
                    action.getCompanyId(),
                    new CreateActivityRequest(
                            uuid(p.get("contactId")),
                            uuid(p.get("opportunityId")),
                            enumOf(p.get("type"), ActivityType.class),
                            str(p.get("subject")),
                            str(p.get("description")),
                            localDateTime(p.get("activityAt"))),
                    action.getUserId());
            case "update_opportunity" -> opportunityUseCase.update(
                    action.getCompanyId(),
                    action.getEntityId(),
                    new UpdateOpportunityRequest(
                            str(p.get("title")),
                            decimal(p.get("value")),
                            uuid(p.get("assignedTo")),
                            localDateTime(p.get("expectedCloseDate")),
                            str(p.get("notes"))),
                    action.getUserId());
            case "cancel_appointment" -> appointmentUseCase.cancel(
                    action.getCompanyId(), uuid(p.get("appointmentId")), str(p.get("reason")));
            case "create_appointment" -> appointmentUseCase.create(
                    action.getCompanyId(),
                    new CreateAppointmentRequest(
                            uuid(p.get("appointmentTypeId")),
                            uuid(p.get("hostId")),
                            uuid(p.get("contactId")),
                            null,
                            str(p.get("title")),
                            Instant.parse(str(p.get("startAt"))),
                            Instant.parse(str(p.get("endAt"))),
                            null, null,
                            str(p.get("notes")),
                            false,
                            null, null, null, null, null),
                    action.getUserId());
            case "reschedule_appointment" -> appointmentUseCase.reschedule(
                    action.getCompanyId(), uuid(p.get("appointmentId")),
                    new RescheduleRequest(Instant.parse(str(p.get("startAt"))),
                            Instant.parse(str(p.get("endAt"))), false));
            case "create_transaction" -> financialTransactionService.create(
                    action.getCompanyId(),
                    new CreateFinancialTransactionRequest(
                            enumOf(p.get("type"), TransactionType.class),
                            str(p.get("description")),
                            enumOf(p.get("category"), TransactionCategory.class),
                            longValue(p.get("amountCents")),
                            null,
                            enumOf(p.get("paymentMethod"), PaymentMethod.class),
                            localDate(p.get("dueDate")),
                            uuid(p.get("contactId")),
                            null,
                            null,
                            null,
                            localDate(p.get("paidAt")),
                            intValue(p.get("installments")),
                            null),
                    action.getUserId());
            case "pay_transaction" -> financialTransactionService.pay(
                    action.getCompanyId(), uuid(p.get("transactionId")),
                    new PayTransactionRequest(localDate(p.get("paidAt"))), action.getUserId());
            case "register_stock_movement" -> inventoryService.registerMovement(
                    action.getCompanyId(), uuid(p.get("itemId")),
                    new RegisterMovementRequest(enumOf(p.get("type"), StockMovementType.class),
                            intValue(p.get("quantity")), str(p.get("note"))));
            case "create_patient" -> createPatient(action, p);
            case "create_procedure" -> createProcedures(action, p);
            default -> throw new IllegalArgumentException(
                    "Ferramenta de escrita nao suportada: " + action.getTool());
        };
    }

    /** Mesmo caminho da tela de pacientes (POST /patients): contato ja marcado como paciente. */
    private Object createPatient(AiAction action, Map<String, Object> p) {
        if (contactUseCase == null) {
            throw new IllegalStateException("Cadastro de paciente indisponivel.");
        }
        return contactUseCase.createPatient(
                action.getCompanyId(),
                new CreateContactRequest(
                        str(p.get("firstName")),
                        str(p.get("lastName")),
                        str(p.get("email")),
                        null,
                        str(p.get("mobile")),
                        str(p.get("notes")),
                        localDate(p.get("birthDate")),
                        str(p.get("cpf")),
                        null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null,
                        null, null, null),
                action.getUserId());
    }

    /**
     * Mesmo caminho do odontograma (POST /procedures), sem financeiro. O preco
     * persistido e por dente: um procedimento por dente; sem dentes (exame), um so.
     */
    private Object createProcedures(AiAction action, Map<String, Object> p) {
        if (procedureUseCase == null) {
            throw new IllegalStateException("Lancamento de procedimentos indisponivel.");
        }
        UUID contactId = uuid(p.get("contactId"));
        List<Integer> teeth = new ArrayList<>();
        if (p.get("teeth") instanceof List<?> raw) {
            raw.forEach(t -> teeth.add(intValue(t)));
        }
        List<List<ProcedureToothDto>> perProcedure = new ArrayList<>();
        if (teeth.isEmpty()) {
            perProcedure.add(List.of());
        } else {
            teeth.forEach(t -> perProcedure.add(List.of(new ProcedureToothDto(t, null))));
        }
        List<Object> created = new ArrayList<>();
        for (List<ProcedureToothDto> procedureTeeth : perProcedure) {
            created.add(procedureUseCase.create(action.getCompanyId(), contactId,
                    new PatientProcedureRequest(
                            uuid(p.get("catalogItemId")),
                            enumOf(p.get("state"), ProcedureState.class),
                            enumOf(p.get("dentition"), Dentition.class),
                            longValue(p.get("priceCents")),
                            null,
                            str(p.get("notes")),
                            localDate(p.get("performedAt")),
                            procedureTeeth,
                            false,
                            null),
                    action.getUserId(), false));
        }
        return created;
    }

    static UUID uuid(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof UUID u ? u : UUID.fromString(String.valueOf(value));
    }

    static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    static BigDecimal decimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(String.valueOf(value));
    }

    static LocalDateTime localDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        return LocalDateTime.parse(String.valueOf(value));
    }

    static LocalDate localDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate ld) {
            return ld;
        }
        return LocalDate.parse(String.valueOf(value));
    }

    static Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return new BigDecimal(String.valueOf(value)).longValueExact();
    }

    static Integer intValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return new BigDecimal(String.valueOf(value)).intValueExact();
    }

    static <E extends Enum<E>> E enumOf(Object value, Class<E> type) {
        if (value == null) {
            return null;
        }
        return Enum.valueOf(type, String.valueOf(value));
    }
}