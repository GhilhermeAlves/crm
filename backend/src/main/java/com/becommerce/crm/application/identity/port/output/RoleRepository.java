package com.becommerce.crm.application.identity.port.output;

import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.valueobject.RoleName;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository {
    Role save(Role role);
    Optional<Role> findById(UUID id);
    Optional<Role> findByNameAndCompanyId(RoleName name, UUID companyId);
}
