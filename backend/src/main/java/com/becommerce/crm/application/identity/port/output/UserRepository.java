package com.becommerce.crm.application.identity.port.output;

import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.UserStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    Optional<User> findByInviteToken(String token);
    Optional<User> findByKeycloakSub(String keycloakSub);
    List<User> findAllByCompanyId(UUID companyId);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID id);
    void deleteById(UUID id);

    PageResult findByCompanyIdWithFilters(UUID companyId, String search, UserStatus status,
                                          int page, int pageSize, String sortBy, String sortDirection);

    record PageResult(List<User> content, long totalElements) {}
}
