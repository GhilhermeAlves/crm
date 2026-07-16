package com.becommerce.crm.domain.audit;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class AuditLog {
    private UUID id;
    private UUID companyId;
    private UUID userId;
    private String userName;
    private String userEmail;
    private AuditAction action;
    private AuditModule module;
    private String entityName;
    private String entityId;
    private String description;
    private Map<String, Object> oldValues;
    private Map<String, Object> newValues;
    private String ipAddress;
    private String userAgent;
    private String requestMethod;
    private String requestUri;
    private AuditStatus status;
    private boolean success;
    private LocalDateTime createdAt;

    public AuditLog() {}

    public static AuditLog create(UUID companyId, AuditAction action, AuditModule module) {
        AuditLog log = new AuditLog();
        log.id = UUID.randomUUID();
        log.companyId = companyId;
        log.action = action;
        log.module = module;
        log.status = AuditStatus.SUCCESS;
        log.success = true;
        log.createdAt = LocalDateTime.now();
        return log;
    }

    public void markFailed(String errorDescription) {
        this.status = AuditStatus.FAILED;
        this.success = false;
        this.description = errorDescription;
    }

    public void markError() {
        this.status = AuditStatus.ERROR;
        this.success = false;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public AuditAction getAction() { return action; }
    public void setAction(AuditAction action) { this.action = action; }

    public AuditModule getModule() { return module; }
    public void setModule(AuditModule module) { this.module = module; }

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, Object> getOldValues() { return oldValues; }
    public void setOldValues(Map<String, Object> oldValues) { this.oldValues = oldValues; }

    public Map<String, Object> getNewValues() { return newValues; }
    public void setNewValues(Map<String, Object> newValues) { this.newValues = newValues; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getRequestMethod() { return requestMethod; }
    public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }

    public String getRequestUri() { return requestUri; }
    public void setRequestUri(String requestUri) { this.requestUri = requestUri; }

    public AuditStatus getStatus() { return status; }
    public void setStatus(AuditStatus status) { this.status = status; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
