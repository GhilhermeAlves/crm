package com.becommerce.crm.application.identity.port.input;

import com.becommerce.crm.application.identity.dto.UserResponse;
import com.becommerce.crm.application.identity.dto.UpdateUserRequest;
import java.util.List;
import java.util.UUID;

public interface UserUseCase {
    UserResponse getUserById(UUID id);
    UserResponse getUserByEmail(String email);
    List<UserResponse> getUsersByCompanyId(UUID companyId);
    UserResponse updateUser(UUID id, UpdateUserRequest request);
    void deactivateUser(UUID id);
    void deleteUser(UUID id);
}
