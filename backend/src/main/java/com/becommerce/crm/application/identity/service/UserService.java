package com.becommerce.crm.application.identity.service;

import com.becommerce.crm.application.identity.dto.*;
import com.becommerce.crm.application.identity.port.input.UserUseCase;
import com.becommerce.crm.application.identity.port.output.EventPublisher;
import com.becommerce.crm.application.identity.port.output.PasswordEncoder;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.application.membership.port.output.MembershipRepository;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.UserStatus;
import com.becommerce.crm.domain.membership.Membership;
import com.becommerce.crm.domain.membership.MembershipStatus;
import com.becommerce.crm.domain.identity.event.UserCreatedEvent;
import com.becommerce.crm.domain.identity.exception.InvalidTokenException;
import com.becommerce.crm.domain.identity.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService implements UserUseCase {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;
    private final MembershipRepository membershipRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       EventPublisher eventPublisher, MembershipRepository membershipRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.membershipRepository = membershipRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com ID: " + id));
        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com email: " + email));
        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(UUID companyId, String search, String status,
                                                 int page, int pageSize, String sortBy, String sortDirection) {
        UserStatus userStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                userStatus = UserStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                userStatus = null;
            }
        }

        UserRepository.PageResult result = userRepository.findByCompanyIdWithFilters(
                companyId, search, userStatus, page, pageSize, sortBy, sortDirection);

        var users = result.content().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.of(users, page, pageSize, result.totalElements());
    }

    @Override
    @Transactional
    public UserResponse createUser(UUID companyId, CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new com.becommerce.crm.domain.identity.exception
                    .DuplicateEmailException("Já existe um usuário com este email: " + request.email());
        }

        String tempPassword = UUID.randomUUID().toString().substring(0, 12) + "A1!";
        var password = new com.becommerce.crm.domain.identity.valueobject.Password(tempPassword);

        User user = User.create(
                new com.becommerce.crm.domain.identity.valueobject.Email(request.email()),
                password,
                request.firstName(),
                request.lastName(),
                companyId
        );
        user.setPhone(request.phone());
        user.setDepartment(request.department());
        user.setJobTitle(request.jobTitle());
        user.setLanguage(request.language() != null ? request.language() : "pt-BR");
        user.setTimezone(request.timezone() != null ? request.timezone() : "America/Sao_Paulo");
        user.setNotes(request.notes());

        User saved = userRepository.save(user);
        eventPublisher.publish(UserCreatedEvent.create(saved.getId(), saved.getEmail().value(), saved.getCompanyId()));

        log.info("Usuário criado: {} para empresa {}", saved.getEmail().value(), companyId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com ID: " + id));

        if (request.email() != null && !request.email().equals(user.getEmail().value())) {
            if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
                throw new com.becommerce.crm.domain.identity.exception
                        .DuplicateEmailException("Já existe um usuário com este email: " + request.email());
            }
            user.setEmail(new com.becommerce.crm.domain.identity.valueobject.Email(request.email()));
        }

        user.updateProfile(
                request.firstName(), request.lastName(), request.phone(),
                request.department(), request.jobTitle(),
                request.language(), request.timezone(), request.notes()
        );
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }

        User saved = userRepository.save(user);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void activateUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com ID: " + id));
        user.activate();
        userRepository.save(user);
        log.info("Usuário ativado: {}", user.getEmail().value());
    }

    @Override
    @Transactional
    public void deactivateUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com ID: " + id));
        user.deactivate();
        userRepository.save(user);
        log.info("Usuário desativado: {}", user.getEmail().value());
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com ID: " + id));
        user.softDelete();
        userRepository.save(user);
        log.info("Usuário excluído (soft delete): {}", user.getEmail().value());
    }

    @Override
    @Transactional
    public UserResponse inviteUser(UUID companyId, UUID invitedBy, InviteUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new com.becommerce.crm.domain.identity.exception
                    .DuplicateEmailException("Já existe um usuário com este email: " + request.email());
        }

        User user = User.createInvited(
                new com.becommerce.crm.domain.identity.valueobject.Email(request.email()),
                request.firstName(),
                request.lastName(),
                companyId,
                invitedBy
        );
        user.setDepartment(request.department());
        user.setJobTitle(request.jobTitle());

        User saved = userRepository.save(user);

        membershipRepository.save(Membership.invite(
                saved.getId(), companyId, "AGENT", invitedBy));

        log.info("Convite enviado para: {} (token: {})", saved.getEmail().value(), saved.getInviteToken());
        // TODO: Send invitation email when EmailService is fully implemented
        // emailService.sendInviteEmail(saved.getEmail().value(), saved.getInviteToken(), saved.getFirstName());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse acceptInvite(String token, String password) {
        User user = userRepository.findByInviteToken(token)
                .orElseThrow(() -> new InvalidTokenException("Convite inválido ou expirado"));

        String hashedPassword = passwordEncoder.encode(password);
        user.activateFromInvite(hashedPassword);

        User saved = userRepository.save(user);

        membershipRepository.findByUserIdAndCompanyId(saved.getId(), saved.getCompanyId())
                .ifPresent(membership -> {
                    membership.setStatus(MembershipStatus.ACTIVE);
                    membership.setJoinedAt(java.time.LocalDateTime.now());
                    membershipRepository.save(membership);
                });

        log.info("Convite aceito por: {}", saved.getEmail().value());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

        user.updateProfile(
                request.firstName(), request.lastName(), request.phone(),
                request.department(), request.jobTitle(),
                request.language(), request.timezone(), request.notes()
        );
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }

        User saved = userRepository.save(user);
        return mapToResponse(saved);
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId().toString(),
                user.getEmail().value(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                user.getPhone(),
                user.getDepartment(),
                user.getJobTitle(),
                user.getAvatarUrl(),
                user.getCompanyId().toString(),
                user.getStatus().name(),
                user.isActive(),
                user.getLanguage(),
                user.getTimezone(),
                user.getNotes(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
