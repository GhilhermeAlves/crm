package com.becommerce.crm.presentation.rest.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.becommerce.crm.application.audit.service.AuditLogNotFoundException;
import com.becommerce.crm.domain.company.CompanyAlreadyExistsException;
import com.becommerce.crm.domain.company.CompanyNotFoundException;
import com.becommerce.crm.domain.company.CompanyDeletionForbiddenException;
import com.becommerce.crm.domain.identity.exception.DuplicateEmailException;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.domain.activity.exception.ActivityNotFoundException;
import com.becommerce.crm.domain.activity.exception.ActivityValidationException;
import com.becommerce.crm.domain.contact.exception.ContactNotFoundException;
import com.becommerce.crm.domain.identity.exception.DuplicateRoleException;
import com.becommerce.crm.domain.identity.exception.InvalidCredentialsException;
import com.becommerce.crm.domain.identity.exception.InvalidTokenException;
import com.becommerce.crm.domain.identity.exception.PermissionNotFoundException;
import com.becommerce.crm.domain.identity.exception.RoleNotFoundException;
import com.becommerce.crm.domain.identity.exception.UserNotFoundException;
import com.becommerce.crm.domain.identity.exception.UserProvisioningException;
import com.becommerce.crm.domain.invitation.exception.InvitationNotFoundException;
import com.becommerce.crm.domain.lead.exception.DuplicateLeadException;
import com.becommerce.crm.domain.lead.exception.LeadNotFoundException;
import com.becommerce.crm.domain.membership.exception.MembershipNotFoundException;
import com.becommerce.crm.domain.pipeline.exception.OpportunityNotFoundException;
import com.becommerce.crm.domain.pipeline.exception.PipelineNotFoundException;
import com.becommerce.crm.domain.pipeline.exception.PipelineValidationException;
import com.becommerce.crm.domain.pipeline.exception.StageNotFoundException;
import com.becommerce.crm.domain.quota.exception.QuotaExceededException;
import com.becommerce.crm.domain.task.exception.TaskNotFoundException;
import com.becommerce.crm.domain.task.exception.TaskValidationException;
import com.becommerce.crm.domain.workflow.WorkflowNotFoundException;
import com.becommerce.crm.domain.workflow.WorkflowValidationException;

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
                "message", ex.getMessage(),
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

    @ExceptionHandler(UserProvisioningException.class)
    public ResponseEntity<Map<String, Object>> handleUserProvisioningException(UserProvisioningException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of(
                "status", 401,
                "error", "Unauthorized",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
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
