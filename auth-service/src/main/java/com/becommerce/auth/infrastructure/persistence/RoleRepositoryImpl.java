package com.becommerce.auth.infrastructure.persistence;

import com.becommerce.auth.application.identity.port.output.RoleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class RoleRepositoryImpl implements RoleRepository {

    private final SpringDataUserRepository springData;

    public RoleRepositoryImpl(SpringDataUserRepository springData) {
        this.springData = springData;
    }

    @Override
    public List<String> findRoleNamesByUserIdAndCompanyId(UUID userId, UUID companyId) {
        return springData.findRoleNamesByUserIdAndCompanyId(userId, companyId);
    }
}
