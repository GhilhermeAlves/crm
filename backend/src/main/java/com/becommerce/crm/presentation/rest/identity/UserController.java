package com.becommerce.crm.presentation.rest.identity;

import com.becommerce.crm.application.identity.dto.*;
import com.becommerce.crm.application.identity.port.input.UserUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    private final UserUseCase userUseCase;
    
    public UserController(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        UserResponse response = userUseCase.getUserById(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        UserResponse response = userUseCase.getUserByEmail(email);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<UserResponse>> getUsersByCompanyId(@PathVariable UUID companyId) {
        List<UserResponse> response = userUseCase.getUsersByCompanyId(companyId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID id,
                                                   @RequestBody UpdateUserRequest request) {
        UserResponse response = userUseCase.updateUser(id, request);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID id) {
        userUseCase.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userUseCase.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
