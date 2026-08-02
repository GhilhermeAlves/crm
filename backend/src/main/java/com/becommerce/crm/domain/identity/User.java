package com.becommerce.crm.domain.identity;

import com.becommerce.crm.domain.identity.valueobject.Email;
import com.becommerce.crm.domain.identity.valueobject.Password;
import java.time.LocalDateTime;
import java.util.UUID;

public class User {
    private UUID id;
    private Email email;
    private Password password;
    private String firstName;
    private String lastName;
    private String name;
    private String phone;
    private String department;
    private String jobTitle;
    private String avatarUrl;
    private UUID companyId;
    private UserStatus status;
    private boolean isActive;
    private boolean crmEnabled;
    private String language;
    private String timezone;
    private String notes;
    private LocalDateTime lastLoginAt;
    private String inviteToken;
    private LocalDateTime invitedAt;
    private UUID invitedBy;
    private String keycloakSub;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public User() {
    }

    public static User create(Email email, Password password, String firstName, String lastName, UUID companyId) {
        User user = new User();
        user.id = UUID.randomUUID();
        user.email = email;
        user.password = password;
        user.firstName = firstName;
        user.lastName = lastName;
        user.name = firstName + " " + lastName;
        user.companyId = companyId;
        user.status = UserStatus.ACTIVE;
        user.isActive = true;
        user.crmEnabled = false;
        user.language = "pt-BR";
        user.timezone = "America/Sao_Paulo";
        user.createdAt = LocalDateTime.now();
        user.updatedAt = LocalDateTime.now();
        return user;
    }

    public static User createInvited(Email email, String firstName, String lastName, UUID companyId, UUID invitedBy) {
        User user = new User();
        user.id = UUID.randomUUID();
        user.email = email;
        user.firstName = firstName;
        user.lastName = lastName;
        user.name = firstName + " " + lastName;
        user.companyId = companyId;
        user.status = UserStatus.PENDING;
        user.isActive = false;
        user.crmEnabled = false;
        user.language = "pt-BR";
        user.timezone = "America/Sao_Paulo";
        user.inviteToken = UUID.randomUUID().toString();
        user.invitedAt = LocalDateTime.now();
        user.invitedBy = invitedBy;
        user.createdAt = LocalDateTime.now();
        user.updatedAt = LocalDateTime.now();
        return user;
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void grantCrmAccess() {
        this.crmEnabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void revokeCrmAccess() {
        this.crmEnabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void lock() {
        this.status = UserStatus.LOCKED;
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void activateFromInvite(String hashedPassword) {
        this.password = Password.fromHash(hashedPassword);
        this.status = UserStatus.ACTIVE;
        this.isActive = true;
        this.inviteToken = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProfile(String firstName, String lastName, String phone, String department,
                              String jobTitle, String language, String timezone, String notes) {
        if (firstName != null) this.firstName = firstName;
        if (lastName != null) this.lastName = lastName;
        if (firstName != null || lastName != null) {
            this.name = (this.firstName != null ? this.firstName : "") + " " + (this.lastName != null ? this.lastName : "");
        }
        if (phone != null) this.phone = phone;
        if (department != null) this.department = department;
        if (jobTitle != null) this.jobTitle = jobTitle;
        if (language != null) this.language = language;
        if (timezone != null) this.timezone = timezone;
        if (notes != null) this.notes = notes;
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePassword(Password password) {
        this.password = password;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Email getEmail() { return email; }
    public void setEmail(Email email) { this.email = email; }
    public Password getPassword() { return password; }
    public void setPassword(Password password) { this.password = password; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public boolean isCrmEnabled() { return crmEnabled; }
    public void setCrmEnabled(boolean crmEnabled) { this.crmEnabled = crmEnabled; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public String getInviteToken() { return inviteToken; }
    public void setInviteToken(String inviteToken) { this.inviteToken = inviteToken; }
    public LocalDateTime getInvitedAt() { return invitedAt; }
    public void setInvitedAt(LocalDateTime invitedAt) { this.invitedAt = invitedAt; }
    public UUID getInvitedBy() { return invitedBy; }
    public void setInvitedBy(UUID invitedBy) { this.invitedBy = invitedBy; }
    public String getKeycloakSub() { return keycloakSub; }
    public void setKeycloakSub(String keycloakSub) { this.keycloakSub = keycloakSub; }

    public void linkKeycloak(String keycloakSub) {
        this.keycloakSub = keycloakSub;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
