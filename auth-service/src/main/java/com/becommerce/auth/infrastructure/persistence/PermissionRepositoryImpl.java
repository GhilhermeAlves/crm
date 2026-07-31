package com.becommerce.auth.infrastructure.persistence;

import com.becommerce.auth.application.identity.port.output.PermissionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class PermissionRepositoryImpl implements PermissionRepository {

    private final SpringDataUserRepository springData;

    public PermissionRepositoryImpl(SpringDataUserRepository springData) {
        this.springData = springData;
    }

    @Override
    public List<String> findPermissionNamesByUserIdAndCompanyId(UUID userId, UUID companyId) {
        return springData.findPermissionNamesByUserIdAndCompanyId(userId, companyId);
    }
}
