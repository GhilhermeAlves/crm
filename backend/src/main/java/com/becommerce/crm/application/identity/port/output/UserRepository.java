package com.becommerce.crm.application.identity.port.output;

import com.becommerce.crm.domain.identity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    List<User> findAllByCompanyId(UUID companyId);
    boolean existsByEmail(String email);
    void deleteById(UUID id);
}
