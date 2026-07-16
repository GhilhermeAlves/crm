package com.becommerce.crm.application.identity.service;

import com.becommerce.crm.application.identity.dto.*;
import com.becommerce.crm.application.identity.port.input.UserUseCase;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.exception.UserNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class UserService implements UserUseCase {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(UserNotFoundException::new);
        return mapToResponse(user);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(UserNotFoundException::new);
        return mapToResponse(user);
    }

    @Override
    public List<UserResponse> getUsersByCompanyId(UUID companyId) {
        return userRepository.findAllByCompanyId(companyId)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Override
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(UserNotFoundException::new);
        return mapToResponse(user);
    }

    @Override
    public void deactivateUser(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(UserNotFoundException::new);
        user.deactivate();
        userRepository.save(user);
    }

    @Override
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(UserNotFoundException::new);
        user.softDelete();
        userRepository.save(user);
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
            user.getId().toString(),
            user.getEmail().value(),
            user.getName(),
            user.getCompanyId().toString(),
            user.isActive(),
            user.getCreatedAt().toString(),
            user.getUpdatedAt().toString()
        );
    }
}
