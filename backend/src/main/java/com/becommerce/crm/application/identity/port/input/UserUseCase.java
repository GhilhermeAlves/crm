package com.becommerce.crm.application.identity.port.input;

import com.becommerce.crm.application.identity.dto.*;

import java.util.UUID;

public interface UserUseCase {
    UserResponse getUserById(UUID id);
    UserResponse getUserByEmail(String email);
    PageResponse<UserResponse> listUsers(UUID companyId, String search, String status, int page, int pageSize, String sortBy, String sortDirection);
    UserResponse createUser(UUID companyId, CreateUserRequest request);
    UserResponse updateUser(UUID id, UpdateUserRequest request);
    void activateUser(UUID id);
    void deactivateUser(UUID id);
    void deleteUser(UUID id);
    UserResponse inviteUser(UUID companyId, UUID invitedBy, InviteUserRequest request);
    UserResponse acceptInvite(String token, String password);
    UserResponse getProfile(UUID userId);
    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);
}
