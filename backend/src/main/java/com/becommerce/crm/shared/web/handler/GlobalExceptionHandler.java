package com.becommerce.crm.shared.web.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.becommerce.crm.analytics.audit.application.service.AuditLogNotFoundException;
import com.becommerce.crm.masterdata.company.domain.CompanyAlreadyExistsException;
import com.becommerce.crm.masterdata.company.domain.CompanyNotFoundException;
import com.becommerce.crm.masterdata.company.domain.InvalidCompanyModulesException;
import com.becommerce.crm.masterdata.company.domain.ModuleDisabledException;
import com.becommerce.crm.masterdata.company.domain.CompanyDeletionForbiddenException;
import com.becommerce.crm.identity.domain.exception.DuplicateEmailException;
import com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException;
import com.becommerce.crm.identity.domain.exception.IdentityServiceUnavailableException;
import com.becommerce.crm.sales.activity.domain.exception.ActivityNotFoundException;
import com.becommerce.crm.sales.activity.domain.exception.ActivityValidationException;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactNotFoundException;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactConflictException;
import com.becommerce.crm.masterdata.contact.note.domain.exception.ContactNoteNotFoundException;
import com.becommerce.crm.clinical.documenttemplate.domain.exception.DocumentTemplateNotFoundException;
import com.becommerce.crm.clinical.anamnesis.domain.exception.PatientAnamnesisNotFoundException;
import com.becommerce.crm.clinical.evolution.domain.exception.PatientEvolutionNotFoundException;
import com.becommerce.crm.clinical.document.domain.exception.PatientDocumentNotFoundException;
import com.becommerce.crm.identity.domain.exception.DuplicateRoleException;
import com.becommerce.crm.identity.domain.exception.InvalidCredentialsException;
import com.becommerce.crm.identity.domain.exception.InvalidTokenException;
import com.becommerce.crm.identity.domain.exception.PermissionNotFoundException;
import com.becommerce.crm.identity.domain.exception.RoleNotFoundException;
import com.becommerce.crm.identity.domain.exception.UserNotFoundException;
import com.becommerce.crm.identity.domain.exception.UserProvisioningException;
import com.becommerce.crm.identity.invitation.domain.exception.InvitationNotFoundException;
import com.becommerce.crm.identity.invitation.domain.exception.InvitationNoLongerValidException;
import com.becommerce.crm.sales.lead.domain.exception.DuplicateLeadException;
import com.becommerce.crm.sales.lead.domain.exception.LeadNotFoundException;
import com.becommerce.crm.automation.campaign.domain.exception.CampaignNotFoundException;
import com.becommerce.crm.automation.campaign.domain.exception.CampaignValidationException;
import com.becommerce.crm.communication.template.domain.exception.TemplateNotFoundException;
import com.becommerce.crm.masterdata.anamnesis.domain.exception.AnamnesisModelNotFoundException;
import com.becommerce.crm.masterdata.anamnesis.domain.exception.AnamnesisValidationException;
import com.becommerce.crm.masterdata.catalog.domain.exception.CatalogItemNotFoundException;
import com.becommerce.crm.masterdata.catalog.domain.exception.CatalogSkuConflictException;
import com.becommerce.crm.masterdata.catalog.domain.exception.InvalidCatalogItemException;
import com.becommerce.crm.masterdata.inventory.domain.exception.InsufficientStockException;
import com.becommerce.crm.masterdata.inventory.domain.exception.InventoryItemNotFoundException;
import com.becommerce.crm.finance.report.domain.exception.ReportSuggestionValidationException;
import com.becommerce.crm.finance.transaction.domain.exception.FinancialTransactionNotFoundException;
import com.becommerce.crm.finance.transaction.domain.exception.FinancialTransactionValidationException;
import com.becommerce.crm.identity.membership.domain.exception.MembershipNotFoundException;
import com.becommerce.crm.sales.pipeline.domain.exception.OpportunityNotFoundException;
import com.becommerce.crm.sales.pipeline.domain.exception.PipelineNotFoundException;
import com.becommerce.crm.sales.pipeline.domain.exception.PipelineValidationException;
import com.becommerce.crm.sales.pipeline.domain.exception.StageNotFoundException;
import com.becommerce.crm.masterdata.quota.domain.exception.QuotaExceededException;
import com.becommerce.crm.masterdata.storage.domain.exception.StorageObjectNotFoundException;
import com.becommerce.crm.reports.export.domain.exception.ReportExportNotFoundException;
import com.becommerce.crm.reports.export.domain.exception.ReportExportValidationException;
import com.becommerce.crm.sales.task.domain.exception.TaskNotFoundException;
import com.becommerce.crm.sales.task.domain.exception.TaskValidationException;
import com.becommerce.crm.communication.notification.domain.exception.NotificationNotFoundException;
import com.becommerce.crm.communication.notification.domain.exception.NotificationValidationException;
import com.becommerce.crm.communication.omnichannel.domain.OmnichannelChannelConflictException;
import com.becommerce.crm.communication.omnichannel.domain.OmnichannelNotFoundException;
import com.becommerce.crm.automation.ai.domain.AiProviderException;
import com.becommerce.crm.automation.ai.domain.AiConversationNotFoundException;
import com.becommerce.crm.automation.ai.domain.AiActionNotFoundException;
import com.becommerce.crm.automation.ai.domain.AiActionInvalidStateException;
import com.becommerce.crm.automation.workflow.domain.WorkflowNotFoundException;
import com.becommerce.crm.automation.workflow.domain.WorkflowValidationException;
import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingNotFoundException;
import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingValidationException;
import com.becommerce.crm.sales.scheduling.domain.exception.SlotUnavailableException;
import com.becommerce.crm.sales.followup.domain.exception.FollowUpNotFoundException;
import com.becommerce.crm.sales.followup.domain.exception.FollowUpSequenceNotFoundException;
import com.becommerce.crm.sales.followup.domain.exception.FollowUpSequenceValidationException;
import com.becommerce.crm.sales.followup.domain.exception.FollowUpValidationException;
import com.becommerce.crm.sales.chair.domain.exception.ChairNotFoundException;
import com.becommerce.crm.sales.chair.domain.exception.ChairValidationException;
import com.becommerce.crm.sales.tag.domain.exception.TagNotFoundException;
import com.becommerce.crm.sales.tag.domain.exception.TagValidationException;
import com.becommerce.crm.sales.holiday.domain.exception.HolidayNotFoundException;
import com.becommerce.crm.sales.holiday.domain.exception.HolidayValidationException;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of(
                "status", 401,
                "error", "Unauthorized",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of(
                "status", 403,
                "error", "Forbidden",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> Map.of("field", fe.getField(), "message", fe.getDefaultMessage()))
            .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Validation Error",
                "errors", errors,
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", "O arquivo excede o limite de 5 MB permitido.",
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFoundException(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", "The requested resource was not found",
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of(
                "status", 401,
                "error", "Unauthorized",
                "message", "Credenciais inválidas.",
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTokenException(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFoundException(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCompanyNotFoundException(CompanyNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(CompanyAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleCompanyAlreadyExistsException(CompanyAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of(
                "status", 409,
                "error", "Conflict",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(OmnichannelChannelConflictException.class)
    public ResponseEntity<Map<String, Object>> handleOmnichannelChannelConflict(OmnichannelChannelConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of(
                "status", 409,
                "error", "Conflict",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(CatalogSkuConflictException.class)
    public ResponseEntity<Map<String, Object>> handleCatalogSkuConflict(CatalogSkuConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of(
                "status", 409,
                "error", "Conflict",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientStock(InsufficientStockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of(
                "status", 409,
                "error", "Conflict",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(InvalidCatalogItemException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCatalogItem(InvalidCatalogItemException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(ModuleDisabledException.class)
    public ResponseEntity<Map<String, Object>> handleModuleDisabledException(ModuleDisabledException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of(
                "status", 403,
                "code", "MODULE_DISABLED",
                "module", ex.getModule().name(),
                "error", "Forbidden",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(InvalidCompanyModulesException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCompanyModulesException(
        InvalidCompanyModulesException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(CompanyDeletionForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleCompanyDeletionForbiddenException(
        CompanyDeletionForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of(
                "status", 409,
                "error", "Conflict",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(AuditLogNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAuditLogNotFoundException(AuditLogNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(MembershipNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMembershipNotFoundException(MembershipNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(InvitationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleInvitationNotFoundException(InvitationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(InvitationNoLongerValidException.class)
    public ResponseEntity<Map<String, Object>> handleInvitationNoLongerValidException(
            InvitationNoLongerValidException ex) {
        return ResponseEntity.status(HttpStatus.GONE)
            .body(Map.of(
                "status", 410,
                "error", "Gone",
                "message", ex.getMessage(),
                "invitationStatus", ex.getStatus().name(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmailException(DuplicateEmailException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of(
                "status", 409,
                "error", "Conflict",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRoleNotFoundException(RoleNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(PermissionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePermissionNotFoundException(PermissionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(DuplicateRoleException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateRoleException(DuplicateRoleException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of(
                "status", 409,
                "error", "Conflict",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(LeadNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleLeadNotFoundException(LeadNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(DuplicateLeadException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateLeadException(DuplicateLeadException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of(
                "status", 409,
                "error", "Conflict",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(OmnichannelNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOmnichannelNotFoundException(OmnichannelNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler({CampaignNotFoundException.class, TemplateNotFoundException.class,
        CatalogItemNotFoundException.class, AnamnesisModelNotFoundException.class,
        FinancialTransactionNotFoundException.class, InventoryItemNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleCampaignOrTemplateNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(CampaignValidationException.class)
    public ResponseEntity<Map<String, Object>> handleCampaignValidationException(CampaignValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(AnamnesisValidationException.class)
    public ResponseEntity<Map<String, Object>> handleAnamnesisValidationException(
            AnamnesisValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(FinancialTransactionValidationException.class)
    public ResponseEntity<Map<String, Object>> handleFinancialTransactionValidationException(
            FinancialTransactionValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(ReportSuggestionValidationException.class)
    public ResponseEntity<Map<String, Object>> handleReportSuggestionValidationException(
            ReportSuggestionValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(CrmAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleCrmAccessDenied(CrmAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of(
                "status", 403,
                "code", "CRM_ACCESS_DENIED",
                "error", "Forbidden",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(PipelineNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePipelineNotFoundException(PipelineNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(StageNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleStageNotFoundException(StageNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(OpportunityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOpportunityNotFoundException(OpportunityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(ContactNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleContactNotFoundException(ContactNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(ContactNoteNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleContactNoteNotFoundException(ContactNoteNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(DocumentTemplateNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentTemplateNotFoundException(
            DocumentTemplateNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(PatientAnamnesisNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePatientAnamnesisNotFoundException(
            PatientAnamnesisNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(PatientEvolutionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePatientEvolutionNotFoundException(
            PatientEvolutionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(PatientDocumentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePatientDocumentNotFoundException(
            PatientDocumentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(PipelineValidationException.class)
    public ResponseEntity<Map<String, Object>> handlePipelineValidationException(PipelineValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(ActivityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleActivityNotFoundException(ActivityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(ActivityValidationException.class)
    public ResponseEntity<Map<String, Object>> handleActivityValidationException(ActivityValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTaskNotFoundException(TaskNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(TaskValidationException.class)
    public ResponseEntity<Map<String, Object>> handleTaskValidationException(TaskValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotificationNotFoundException(NotificationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(NotificationValidationException.class)
    public ResponseEntity<Map<String, Object>> handleNotificationValidationException(NotificationValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<Map<String, Object>> handleAiProviderException(AiProviderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(Map.of(
                "status", 502,
                "error", "Bad Gateway",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(AiConversationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAiConversationNotFoundException(
        AiConversationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(AiActionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAiActionNotFoundException(
        AiActionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(AiActionInvalidStateException.class)
    public ResponseEntity<Map<String, Object>> handleAiActionInvalidStateException(
        AiActionInvalidStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of(
                "status", 409,
                "error", "Conflict",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(WorkflowNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleWorkflowNotFoundException(WorkflowNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(WorkflowValidationException.class)
    public ResponseEntity<Map<String, Object>> handleWorkflowValidationException(WorkflowValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(FollowUpNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleFollowUpNotFoundException(FollowUpNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(FollowUpSequenceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleFollowUpSequenceNotFoundException(
            FollowUpSequenceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(FollowUpValidationException.class)
    public ResponseEntity<Map<String, Object>> handleFollowUpValidationException(FollowUpValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(FollowUpSequenceValidationException.class)
    public ResponseEntity<Map<String, Object>> handleFollowUpSequenceValidationException(
            FollowUpSequenceValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQuotaExceededException(QuotaExceededException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(Map.of(
                "status", 422,
                "code", "QUOTA_EXCEEDED",
                "error", "Unprocessable Entity",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(StorageObjectNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleStorageObjectNotFoundException(StorageObjectNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(ReportExportNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleReportExportNotFoundException(ReportExportNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(ReportExportValidationException.class)
    public ResponseEntity<Map<String, Object>> handleReportExportValidationException(
            ReportExportValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(UserProvisioningException.class)
    public ResponseEntity<Map<String, Object>> handleUserProvisioningException(UserProvisioningException ex) {
        log.warn("UserProvisioning falhou: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of(
                "status", 401,
                "error", "Unauthorized",
                "message", "Não foi possível concluir o cadastro, tente novamente.",
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(IdentityServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleIdentityServiceUnavailableException(
            IdentityServiceUnavailableException ex) {
        log.error("IdentityService indisponível: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "status", 503,
                "error", "Service Unavailable",
                "message", "Serviço de identidade indisponível, tente novamente.",
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("IllegalState: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", "Operação não pode ser concluída.",
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(SchedulingNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSchedulingNotFoundException(SchedulingNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException.class)
    public ResponseEntity<Map<String, Object>> handleContactValidationException(
            com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(ContactConflictException.class)
    public ResponseEntity<Map<String, Object>> handleContactConflictException(ContactConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of(
                "status", 409,
                "error", "Conflict",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(SchedulingValidationException.class)
    public ResponseEntity<Map<String, Object>> handleSchedulingValidationException(SchedulingValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(SlotUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleSlotUnavailableException(SlotUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "status", 409,
                        "error", "Conflict",
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    @ExceptionHandler(ChairNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleChairNotFoundException(ChairNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "status", 404,
                        "error", "Not Found",
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    @ExceptionHandler(ChairValidationException.class)
    public ResponseEntity<Map<String, Object>> handleChairValidationException(ChairValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", 400,
                        "error", "Bad Request",
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    @ExceptionHandler(TagNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTagNotFoundException(TagNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "status", 404,
                        "error", "Not Found",
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    @ExceptionHandler(TagValidationException.class)
    public ResponseEntity<Map<String, Object>> handleTagValidationException(TagValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", 400,
                        "error", "Bad Request",
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    @ExceptionHandler(HolidayNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleHolidayNotFoundException(HolidayNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "status", 404,
                        "error", "Not Found",
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    @ExceptionHandler(HolidayValidationException.class)
    public ResponseEntity<Map<String, Object>> handleHolidayValidationException(HolidayValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", 400,
                        "error", "Bad Request",
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                "status", 500,
                "error", "Internal Server Error",
                "message", "An unexpected error occurred",
                "timestamp", LocalDateTime.now().toString()
            ));
    }
}
