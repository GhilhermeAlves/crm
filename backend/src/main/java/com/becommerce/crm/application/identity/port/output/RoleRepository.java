package com.becommerce.crm.application.identity.port.output;

import com.becommerce.crm.domain.identity.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository {
    Role save(Role role);
    Optional<Role> findById(UUID id);
    Optional<Role> findByNameAndCompanyId(String name, UUID companyId);
    List<Role> findAllByCompanyId(UUID companyId);
    boolean existsByNameAndCompanyId(String name, UUID companyId);
    void deleteById(UUID id);
}
